package ru.study.reviews;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ReviewParserApplication {
    public static void main(String[] args) {
        SpringApplication.run(ReviewParserApplication.class, args);
    }
}
