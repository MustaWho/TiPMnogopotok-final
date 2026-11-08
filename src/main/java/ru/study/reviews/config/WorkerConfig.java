package ru.study.reviews.config;

import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class WorkerConfig {
    @Bean(destroyMethod = "shutdown")
    public ExecutorService parserExecutor(@Value("${parser.workers}") int workers,
                                          @Value("${parser.queue-capacity}") int capacity) {
        AtomicInteger number = new AtomicInteger();
        return new ThreadPoolExecutor(workers, workers, 0, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(capacity), task ->
                new Thread(task, "review-worker-" + number.incrementAndGet()),
                new ThreadPoolExecutor.AbortPolicy());
    }

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.connectTimeout(Duration.ofSeconds(5))
                .readTimeout(Duration.ofSeconds(10))
                .defaultHeader("User-Agent", "StudyReviewParser/1.0")
                .build();
    }
}
