package ru.study.reviews.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.study.reviews.service.AnswerService;
import ru.study.reviews.service.ParseService;

@RestController
public class ReviewController {
    private final ParseService parser;
    private final AnswerService answers;
    public ReviewController(ParseService parser, AnswerService answers) { this.parser = parser; this.answers = answers; }

    public record ParseRequest(@NotEmpty @Size(max = 20) List<@NotBlank String> urls) { }

    @PostMapping("/parse")
    public ResponseEntity<ParseService.JobView> parse(@Valid @RequestBody ParseRequest request) {
        var job = parser.submit(request.urls());
        return ResponseEntity.accepted().location(URI.create("/jobs/" + job.id())).body(job);
    }

    @GetMapping("/jobs/{id}")
    public ParseService.JobView job(@PathVariable UUID id) { return parser.get(id); }

    @GetMapping("/answer")
    public AnswerService.Answer answer(@RequestParam(defaultValue = "date") String sort,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestParam(required = false) Double minRating, @RequestParam(required = false) String author,
            @RequestParam(required = false) String sourceUrl,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return answers.find(sort, direction, minRating, author, sourceUrl, page, size);
    }
}
