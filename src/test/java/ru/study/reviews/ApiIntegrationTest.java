package ru.study.reviews;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "parser.demo-enabled=false"})
class ApiIntegrationTest {
    @Autowired TestRestTemplate http;
    @LocalServerPort int port;

    @Test void concurrentCollectionDeduplicatesAndSupportsSortedResultsAndErrors() {
        var request = Map.of("urls", List.of(url("/demo/product/1"), url("/demo/product/2")));
        var first = CompletableFuture.supplyAsync(() -> http.postForEntity("/parse", request, JsonNode.class));
        var second = CompletableFuture.supplyAsync(() -> http.postForEntity("/parse", request, JsonNode.class));
        var a = first.join(); var b = second.join();
        assertThat(a.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(b.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        for (var response : List.of(a, b)) {
            String id = response.getBody().path("id").asText();
            await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                    assertThat(http.getForObject("/jobs/" + id, JsonNode.class).path("status").asText())
                            .isEqualTo("COMPLETED"));
        }
        JsonNode answer = http.getForObject("/answer?sort=rating&direction=desc&minRating=4&size=2", JsonNode.class);
        assertThat(answer.path("totalElements").asInt()).isEqualTo(4);
        assertThat(answer.path("content").size()).isEqualTo(2);
        assertThat(answer.path("content").get(0).path("rating").asDouble()).isEqualTo(5);
        assertThat(http.getForObject("/answer", JsonNode.class).path("totalElements").asInt()).isEqualTo(5);

        var failed = http.postForEntity("/parse", Map.of("urls", List.of(url("/demo/product/99"))), JsonNode.class);
        String id = failed.getBody().path("id").asText();
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                assertThat(http.getForObject("/jobs/" + id, JsonNode.class).path("status").asText()).isEqualTo("FAILED"));
        var partial = http.postForEntity("/parse", Map.of("urls", List.of(
                url("/demo/product/1"), url("/demo/product/99"))), JsonNode.class);
        String partialId = partial.getBody().path("id").asText();
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            JsonNode job = http.getForObject("/jobs/" + partialId, JsonNode.class);
            assertThat(job.path("status").asText()).isEqualTo("PARTIAL");
            assertThat(job.path("foundReviews").asInt()).isEqualTo(3);
            assertThat(job.path("savedReviews").asInt()).isZero();
            assertThat(job.path("errors").size()).isEqualTo(1);
        });
        assertThat(http.postForEntity("/parse", Map.of("urls", List.of("file:///etc/passwd")), String.class)
                .getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(http.postForEntity("/parse", Map.of("urls", List.of()), String.class)
                .getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(http.getForEntity("/answer?sort=wrong", String.class).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(http.getForEntity("/answer?size=0", String.class).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
    private String url(String path) { return "http://localhost:" + port + path; }
}
