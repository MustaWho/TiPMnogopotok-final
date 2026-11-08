package ru.study.reviews;

import org.junit.jupiter.api.Test;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.boot.web.server.WebServer;
import ru.study.reviews.service.DemoScheduler;
import ru.study.reviews.service.ParseService;
import static org.mockito.Mockito.*;

class DemoSchedulerTest {
    @Test void schedulesDemoOnlyWhenEnabledReadyAndIdle() {
        ParseService service = mock(ParseService.class);
        DemoScheduler scheduler = new DemoScheduler(service, true);
        scheduler.collect();
        verifyNoInteractions(service);
        WebServerInitializedEvent event = mock(WebServerInitializedEvent.class);
        WebServer server = mock(WebServer.class);
        when(event.getWebServer()).thenReturn(server);
        when(server.getPort()).thenReturn(8080);
        scheduler.serverStarted(event);
        when(service.hasActiveJobs()).thenReturn(true);
        scheduler.collect();
        verify(service, never()).submit(anyList());
        when(service.hasActiveJobs()).thenReturn(false);
        scheduler.collect();
        verify(service).submit(java.util.List.of("http://localhost:8080/demo/product/1",
                "http://localhost:8080/demo/product/2"));
        ParseService disabledService = mock(ParseService.class);
        DemoScheduler disabled = new DemoScheduler(disabledService, false);
        disabled.serverStarted(event);
        disabled.collect();
        verifyNoInteractions(disabledService);
    }
}
