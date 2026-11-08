package ru.study.reviews.service;

import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import ru.study.reviews.model.Review;
import ru.study.reviews.repository.ReviewRepository;

@Service
public class AnswerService {
    private final ReviewRepository repository;
    public AnswerService(ReviewRepository repository) { this.repository = repository; }

    public Answer find(String sort, String direction, Double minRating, String author,
                       String sourceUrl, int page, int size) {
        if (page < 0 || size < 1 || size > 100) throw new IllegalArgumentException("page >= 0, size от 1 до 100");
        if (minRating != null && (!Double.isFinite(minRating) || minRating < 1 || minRating > 5))
            throw new IllegalArgumentException("minRating от 1 до 5");
        Comparator<Review> comparator = switch (sort) {
            case "rating" -> Comparator.comparingDouble(Review::rating);
            case "date" -> Comparator.comparing(Review::date);
            case "author" -> Comparator.comparing(Review::author, String.CASE_INSENSITIVE_ORDER);
            case "id" -> Comparator.comparing(Review::id);
            default -> throw new IllegalArgumentException("sort: id, rating, date, author");
        };
        if (direction.equalsIgnoreCase("desc")) comparator = comparator.reversed();
        else if (!direction.equalsIgnoreCase("asc")) throw new IllegalArgumentException("direction: asc или desc");
        // Only immutable DTOs leave JDBC; parallel workers never share a JDBC connection.
        List<Review> result = repository.findAll().parallelStream()
                .filter(r -> minRating == null || r.rating() >= minRating)
                .filter(r -> author == null || r.author().toLowerCase(java.util.Locale.ROOT)
                        .contains(author.toLowerCase(java.util.Locale.ROOT)))
                .filter(r -> sourceUrl == null || r.sourceUrl().equals(sourceUrl))
                .sorted(comparator.thenComparing(Review::id)).toList();
        List<Review> content = result.stream().skip((long) page * size).limit(size).toList();
        return new Answer(content, result.size(), page, size);
    }
    public record Answer(List<Review> content, long totalElements, int page, int size) { }
}
