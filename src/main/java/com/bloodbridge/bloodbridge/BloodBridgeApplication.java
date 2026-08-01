package com.bloodbridge.bloodbridge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableAsync
@EnableScheduling
@EnableCaching
@EnableTransactionManagement
@EntityScan(basePackages = {"com.bloodbridge.bloodbridge.entity", "com.bloodbridge.bloodbridge.shared"})
@EnableJpaRepositories(basePackages = {"com.bloodbridge.bloodbridge.repository", "com.bloodbridge.bloodbridge.shared"})
public class BloodBridgeApplication {

    public static void main(String[] args) {
        SpringApplication.run(BloodBridgeApplication.class, args);
    }
}
