package tech.songjian.backend.http.server.controller;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.songjian.gateway.client.core.ApiInvoker;
import tech.songjian.gateway.client.core.ApiProperties;
import tech.songjian.gateway.client.core.ApiProtocol;
import tech.songjian.gateway.client.core.ApiService;

@Slf4j
@RestController
@ApiService(serviceId = "backend-http-server", protocol = ApiProtocol.HTTP, patternPath = "/http-server/**")
public class PingController {

    @Autowired
    private ApiProperties apiProperties;

    @ApiInvoker(path = "/http-server/ping")
    @GetMapping("/http-server/ping")
    public String ping() throws InterruptedException {
        log.info("{}", apiProperties);
        Thread.sleep(200000);
        return "pong";
    }

}
