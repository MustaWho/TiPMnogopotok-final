package ru.study.reviews.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;
import ru.study.reviews.model.Review;

@Component
public class ReviewHtmlParser {
    private final ObjectMapper mapper;
    public ReviewHtmlParser(ObjectMapper mapper) { this.mapper = mapper; }

    public List<Review> parse(String url, String html) {
        List<Review> result = new ArrayList<>();
        for (var script : Jsoup.parse(html).select("script[type=application/ld+json]")) {
            try { walk(mapper.readTree(script.data()), url, result); }
            catch (com.fasterxml.jackson.core.JsonProcessingException ignored) {
                // One invalid JSON-LD block must not hide valid blocks on the same page.
            }
        }
        if (result.isEmpty()) throw new IllegalArgumentException(
                "На странице нет полных отзывов Schema.org Review (автор, текст, рейтинг 1–5, дата ISO).");
        return List.copyOf(result);
    }

    private void walk(JsonNode node, String url, List<Review> result) {
        if (node == null) return;
        if (node.isObject() && isReview(node.path("@type"))) {
            try {
                String author = node.path("author").isTextual() ? node.path("author").asText()
                        : node.path("author").path("name").asText();
                String text = node.path("reviewBody").asText();
                double rating = Double.parseDouble(node.path("reviewRating").path("ratingValue").asText());
                String rawDate = node.path("datePublished").asText();
                LocalDate date = LocalDate.parse(rawDate.length() > 10 ? rawDate.substring(0, 10) : rawDate);
                if (!author.isBlank() && author.length() <= 500 && !text.isBlank()
                        && Double.isFinite(rating) && rating >= 1 && rating <= 5) {
                    result.add(new Review(null, url, author, text, rating, date, OffsetDateTime.now()));
                }
            } catch (IllegalArgumentException | java.time.DateTimeException ignored) { }
        }
        if (node.isContainerNode()) node.forEach(child -> walk(child, url, result));
    }

    private boolean isReview(JsonNode type) {
        if (type.isTextual()) return type.asText().equals("Review")
                || type.asText().equals("https://schema.org/Review")
                || type.asText().equals("http://schema.org/Review");
        if (type.isArray()) for (JsonNode item : type) if (isReview(item)) return true;
        return false;
    }
}
