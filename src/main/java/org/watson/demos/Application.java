package org.watson.demos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.time.ZoneOffset;
import java.util.TimeZone;

@SpringBootApplication
public class Application {

    public static void main(final String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneOffset.UTC));
        SpringApplication.run(Application.class, args);
    }
}
