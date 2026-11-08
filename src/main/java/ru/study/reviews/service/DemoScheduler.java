package ru.study.reviews.service;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DemoScheduler {
    private final ParseService service;
    private final boolean enabled;
    private volatile int port;
    public DemoScheduler(ParseService service, @Value("${parser.demo-enabled}") boolean enabled) {
        this.service = service; this.enabled = enabled;
    }
    @EventListener
    public void serverStarted(WebServerInitializedEvent event) { port = event.getWebServer().getPort(); }
    @EventListener(ApplicationReadyEvent.class)
    public void onReady() { collect(); }
    @Scheduled(fixedDelayString = "${parser.schedule-delay-ms}",
            initialDelayString = "${parser.schedule-initial-delay-ms}")
    public void collect() {
        if (enabled && port > 0 && !service.hasActiveJobs()) service.submit(List.of(
                "http://localhost:" + port + "/demo/product/1",
                "http://localhost:" + port + "/demo/product/2"));
    }
}
