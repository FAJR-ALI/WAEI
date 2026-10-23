package org.fadhel.waeiplatform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class WaeiPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(WaeiPlatformApplication.class, args);
    }

}
