package com.taskmanagement.secure;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableTransactionManagement
public class SecureTaskManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(SecureTaskManagementApplication.class, args);
    }
}
