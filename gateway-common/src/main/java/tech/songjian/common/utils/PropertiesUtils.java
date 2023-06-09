package tech.songjian.common.utils;

import java.lang.reflect.Method;
import java.util.Properties;

public class PropertiesUtils {

    public static void properties2Object(final Properties p, final Object object, String prefix) {
        // 通过反射获取 object 的方法
        Method[] methods = object.getClass().getMethods();
        for (Method method : methods) {
            String mn = method.getName();
            if (mn.startsWith("set")) {
                try {
                	// set 后第二个字符开始
                    String tmp = mn.substring(4);
                    // 把 set 后第一个字符取出
                    String first = mn.substring(3, 4);
                    String key = prefix + first.toLowerCase() + tmp;
                    // 从 source 中找到对应的属性
                    String property = p.getProperty(key);
                    if (property != null) {
                        Class<?>[] pt = method.getParameterTypes();
                        if (pt != null && pt.length > 0) {
                            String cn = pt[0].getSimpleName();
                            Object arg = null;
                            // 根据参数类型进行类型转换
                            if (cn.equals("int") || cn.equals("Integer")) {
                                arg = Integer.parseInt(property);
                            } else if (cn.equals("long") || cn.equals("Long")) {
                                arg = Long.parseLong(property);
                            } else if (cn.equals("double") || cn.equals("Double")) {
                                arg = Double.parseDouble(property);
                            } else if (cn.equals("boolean") || cn.equals("Boolean")) {
                                arg = Boolean.parseBoolean(property);
                            } else if (cn.equals("float") || cn.equals("Float")) {
                                arg = Float.parseFloat(property);
                            } else if (cn.equals("String")) {
                                arg = property;
                            } else {
                                continue;
                            }
                            // 调用对应的 set 方法，把参数设置到对象中
                            method.invoke(object, arg);
                        }
                    }
                } catch (Throwable ignored) {
                }
            }
        }
    }

    public static void properties2Object(final Properties p, final Object object) {
        properties2Object(p, object, "");
    }
}
