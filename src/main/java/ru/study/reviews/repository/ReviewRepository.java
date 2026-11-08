package ru.study.reviews.repository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.study.reviews.model.Review;

@Repository
public class ReviewRepository {
    private final JdbcTemplate jdbc;
    public ReviewRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    // The monitor makes check + insert atomic for concurrent workers in this JVM.
    // The database UNIQUE constraint additionally protects the stored fingerprint.
    public synchronized int saveAll(List<Review> reviews) {
        int added = 0;
        for (Review r : reviews) {
            String key = fingerprint(r);
            added += jdbc.update("""
                INSERT INTO reviews (fingerprint, source_url, author, review_text, rating, review_date, collected_at)
                SELECT ?, ?, ?, ?, ?, ?, ? WHERE NOT EXISTS
                (SELECT 1 FROM reviews WHERE fingerprint = ?)
                """, key, r.sourceUrl(), r.author(), r.text(), r.rating(), r.date(), r.collectedAt(), key);
        }
        return added;
    }

    public List<Review> findAll() {
        return jdbc.query("SELECT * FROM reviews", (rs, n) -> new Review(
                rs.getLong("id"), rs.getString("source_url"), rs.getString("author"),
                rs.getString("review_text"), rs.getDouble("rating"),
                rs.getDate("review_date").toLocalDate(),
                rs.getObject("collected_at", OffsetDateTime.class)));
    }

    private String fingerprint(Review r) {
        try {
            // Length-delimited fields avoid ambiguous concatenations.
            StringBuilder value = new StringBuilder();
            for (String field : List.of(r.sourceUrl(), r.author(), r.text(),
                    Double.toString(r.rating()), r.date().toString())) {
                value.append(field.length()).append(':').append(field);
            }
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}
