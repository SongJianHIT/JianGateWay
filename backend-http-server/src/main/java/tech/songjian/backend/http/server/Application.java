/**
 * @projectName JianGateWay
 * @package tech.songjian.backend.http.server
 * @className tech.songjian.backend.http.server.Application
 */
package tech.songjian.backend.http.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Application
 * @description
 * @author SongJian
 * @date 2023/6/9 10:13
 * @version
 */
@SpringBootApplication
@RestController
public class Application {

    @GetMapping("/http-demo/ping")
    public String ping() {
        return "pong";
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class);
    }
}

