package org.watson.demos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync(proxyTargetClass = true)
@SpringBootApplication
public class Application {

    public static void main(final String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
