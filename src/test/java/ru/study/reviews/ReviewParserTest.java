package ru.study.reviews;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import ru.study.reviews.service.ReviewHtmlParser;
import static org.assertj.core.api.Assertions.*;

class ReviewParserTest {
    private final ReviewHtmlParser parser = new ReviewHtmlParser(new ObjectMapper());
    @Test void ignoresMalformedBlocksAndIncompleteReviews() {
        var result = parser.parse("https://example.org/product", """
          <script type="application/ld+json">invalid</script>
          <script type="application/ld+json">{"@graph":[
           {"@type":["Review"],"author":"Автор","reviewBody":"Текст",
            "reviewRating":{"ratingValue":"4"},"datePublished":"2026-01-02T10:00:00Z"},
           {"@type":"Review","author":"Без рейтинга"},
           {"@type":"Review","author":"Автор","reviewBody":"Текст",
            "reviewRating":{"ratingValue":6},"datePublished":"2026-01-02"}]}</script>
          """);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).rating()).isEqualTo(4);
        assertThat(result.get(0).date()).isEqualTo("2026-01-02");
    }
    @Test void reportsUnsupportedPage() {
        assertThatThrownBy(() -> parser.parse("https://example.org", "<h1>Captcha</h1>"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("нет полных отзывов");
    }
}
