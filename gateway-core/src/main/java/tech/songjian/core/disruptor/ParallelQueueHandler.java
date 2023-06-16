package tech.songjian.core.disruptor;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.lmax.disruptor.*;
import com.lmax.disruptor.dsl.ProducerType;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by IntelliJ IDEA.
 * tech.songjian.core.disruptor
 *
 * @Author: SongJian
 * @Create: 2023/6/13 20:40
 * @Version:
 * @Describe: 基于 disruptor 实现的多生产者多消费者无锁队列处理类
 */
public class ParallelQueueHandler<E> implements ParallelQueue<E> {

    private RingBuffer<Holder> ringBuffer;

    private EventListener<E> eventListener;

    private WorkerPool<Holder> workerPool;

    private ExecutorService executorService;

    private EventTranslatorOneArg<Holder, E> eventTranslator;

    public ParallelQueueHandler(Builder<E> builder) {
        this.executorService = Executors.newFixedThreadPool(builder.threads,
                new ThreadFactoryBuilder()
                        .setNameFormat("ParallelQueueHandler" + builder.namePrefix + "-pool-%d").build());
        this.eventListener = builder.listener;
        this.eventTranslator = new HolderEventTranslator();

        // 创建 RingBuffer
        this.ringBuffer = RingBuffer.create(builder.producerType,
                new HolderEventFactory(), builder.bufferSize, builder.waitStrategy);

        // 通过 RingBuffer 创建屏障
        SequenceBarrier sequenceBarrier = ringBuffer.newBarrier();

        // 创建多个消费者组
        WorkHandler<Holder>[] workHandlers = new WorkHandler[builder.threads];
        for (int i = 0; i < workHandlers.length; ++i) {
            workHandlers[i] = new HolderWorkHandler();
        }

        // 构建多个消费者线程池
        WorkerPool<Holder> workerPool = new WorkerPool<Holder>(ringBuffer,
                sequenceBarrier,
                new HolderExceptionHandler(),
                workHandlers);

        //设置多消费者的Sequence序号，主要用于统计消费进度，
        ringBuffer.addGatingSequences(workerPool.getWorkerSequences());
        this.workerPool = workerPool;
    }

    @Override
    public void add(E event) {
        final RingBuffer<Holder> holderRingBuffer = ringBuffer;
        if (holderRingBuffer == null) {
            process(this.eventListener, new IllegalStateException("ParallelQueueHandler 已经关闭！"), event);
        }
        try {
            ringBuffer.publishEvent(this.eventTranslator, event);
        } catch (NullPointerException e) {
            process(this.eventListener, new IllegalStateException("ParallelQueueHandler 已经关闭！"), event);
        }
    }

    @Override
    public void add(E... events) {
        final RingBuffer<Holder> holderRingBuffer = ringBuffer;
        if (holderRingBuffer == null) {
            process(this.eventListener, new IllegalStateException("ParallelQueueHandler 已经关闭！"), events);
        }
        try {
            ringBuffer.publishEvents(this.eventTranslator, events);
        } catch (NullPointerException e) {
            process(this.eventListener, new IllegalStateException("ParallelQueueHandler 已经关闭！"), events);
        }
    }

    @Override
    public boolean tryAdd(E event) {
        final RingBuffer<Holder> holderRingBuffer = ringBuffer;
        if (holderRingBuffer == null) {
            return false;
        }
        try {
            return ringBuffer.tryPublishEvent(this.eventTranslator, event);
        } catch (NullPointerException e) {
            return false;
        }
    }

    @Override
    public boolean tryAdd(E... events) {
        final RingBuffer<Holder> holderRingBuffer = ringBuffer;
        if (holderRingBuffer == null) {
            return false;
        }
        try {
            return ringBuffer.tryPublishEvents(this.eventTranslator, events);
        } catch (NullPointerException e) {
            return false;
        }
    }

    @Override
    public void start() {
        this.ringBuffer = workerPool.start(executorService);
    }

    @Override
    public void shutDown() {
        RingBuffer<Holder> holderRingBuffer = ringBuffer;
        ringBuffer = null;
        if (holderRingBuffer == null) {
            return;
        }
        if (workerPool != null) {
            workerPool.drainAndHalt();
        }
        if (executorService != null) {
            executorService.shutdown();
        }
    }

    @Override
    public boolean isShutDown() {
        return ringBuffer == null;
    }


    private static <E> void process (EventListener<E> listener, Throwable e, E event) {
        listener.onException(e, -1, event);
    }

    private static <E> void process (EventListener<E> listener, Throwable e, E... events) {
        for (E event : events) {
            process(listener, e, event);
        }
    }

    /**
     * 建造者模式构建
     * 通过 setter 返回 builder
     */
    public static class Builder<E> {
        // 多生产者
        private ProducerType producerType = ProducerType.MULTI;
        // buffer大小
        private int bufferSize = 1024 * 16;
        // 线程数
        private int threads = 1;
        // 前缀
        private String namePrefix = "";
        // 等待策略
        private WaitStrategy waitStrategy = new BlockingWaitStrategy();
        private EventListener<E> listener;

        public Builder<E> setProducerType(ProducerType producerType) {
            Preconditions.checkNotNull(producerType);
            this.producerType = producerType;
            return this;
        }

        public Builder<E> setBufferSize(int bufferSize) {
            Preconditions.checkArgument(Integer.bitCount(bufferSize) == 1);
            this.bufferSize = bufferSize;
            return this;
        }

        public Builder<E> setThreads(int threads) {
            Preconditions.checkArgument(Integer.bitCount(threads) >= 1);
            this.threads = threads;
            return this;
        }

        public Builder<E> setNamePrefix(String namePrefix) {
            Preconditions.checkNotNull(namePrefix);
            this.namePrefix = namePrefix;
            return this;
        }

        public Builder<E> setWaitStrategy(WaitStrategy waitStrategy) {
            Preconditions.checkNotNull(waitStrategy);
            this.waitStrategy = waitStrategy;
            return this;
        }

        public Builder<E> setListener(EventListener<E> listener) {
            Preconditions.checkNotNull(listener);
            this.listener = listener;
            return this;
        }

        public ParallelQueueHandler<E> build () {
            return new ParallelQueueHandler<>(this);
        }
    }

    /**
     * 事件
     */
    public class Holder {
        private E event;

        public void setValue(E event) {
            this.event = event;
        }

        @Override
        public String toString() {
            return "Holder{" +
                    "event=" + event +
                    '}';
        }
    }

    private class HolderEventTranslator implements EventTranslatorOneArg<Holder, E> {

        @Override
        public void translateTo(Holder holder, long l, E e) {
            holder.setValue(e);
        }
    }

    public class HolderEventFactory implements EventFactory<Holder> {

        @Override
        public Holder newInstance() {
            return new Holder();
        }
    }

    public class HolderWorkHandler implements WorkHandler<Holder> {

        @Override
        public void onEvent(Holder holder) throws Exception {
            eventListener.onEvent(holder.event);
            holder.setValue(null);
        }
    }

    public class HolderExceptionHandler implements ExceptionHandler<Holder> {

        @Override
        public void handleEventException(Throwable throwable, long l, Holder event) {
            Holder holder = (Holder) event;
            try {
                eventListener.onException(throwable, l, holder.event);
            } catch (Exception e) {

            } finally {
                holder.setValue(null);
            }
        }

        @Override
        public void handleOnStartException(Throwable throwable) {
            throw new UnsupportedOperationException(throwable);
        }

        @Override
        public void handleOnShutdownException(Throwable throwable) {
            throw new UnsupportedOperationException(throwable);
        }
    }
}
