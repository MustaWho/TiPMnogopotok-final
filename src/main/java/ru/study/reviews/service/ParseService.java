package ru.study.reviews.service;

import java.net.URI;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.study.reviews.repository.ReviewRepository;

@Service
public class ParseService {
    private static final Logger log = LoggerFactory.getLogger(ParseService.class);
    private final ExecutorService executor;
    private final RestTemplate http;
    private final ReviewHtmlParser parser;
    private final ReviewRepository repository;
    private final ConcurrentMap<UUID, Job> jobs = new ConcurrentHashMap<>();

    public ParseService(ExecutorService executor, RestTemplate http,
                        ReviewHtmlParser parser, ReviewRepository repository) {
        this.executor = executor; this.http = http; this.parser = parser; this.repository = repository;
    }

    public synchronized JobView submit(List<String> urls) {
        List<String> sources = urls.stream().map(String::trim).distinct().toList();
        sources.forEach(this::validateUrl);
        // Limit retained history without evicting jobs that are still running.
        if (jobs.size() >= 200) jobs.values().stream().filter(Job::finished)
                .min(Comparator.comparing(j -> j.createdAt)).ifPresent(j -> jobs.remove(j.id));
        if (jobs.size() >= 200) throw new RejectedExecutionException("Слишком много активных заданий");
        Job job = new Job(sources.size());
        jobs.put(job.id, job);
        for (String url : sources) {
            try { executor.execute(() -> process(job, url)); }
            catch (RejectedExecutionException e) {
                job.errors.put(url, "Очередь заполнена; повторите запрос позже");
                job.completed.incrementAndGet();
            }
        }
        return job.view();
    }

    private void process(Job job, String url) {
        try {
            log.info("Сбор {} в потоке {}", url, Thread.currentThread().getName());
            String html = http.getForObject(URI.create(url), String.class);
            if (html == null) throw new IllegalArgumentException("Пустой HTTP-ответ");
            var reviews = parser.parse(url, html);
            job.found.addAndGet(reviews.size());
            job.saved.addAndGet(repository.saveAll(reviews));
        } catch (Exception e) {
            String message = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            job.errors.put(url, message.length() > 500 ? message.substring(0, 500) : message);
            log.warn("Ошибка сбора {}: {}", url, message);
        } finally { job.completed.incrementAndGet(); }
    }

    private void validateUrl(String url) {
        try {
            URI uri = URI.create(url);
            if (url.length() > 2048 || uri.getHost() == null || uri.getUserInfo() != null
                    || !("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme())))
                throw new IllegalArgumentException();
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Нужен полный HTTP(S) URL без логина и пароля: " + url);
        }
    }

    public JobView get(UUID id) {
        Job job = jobs.get(id);
        if (job == null) throw new NoSuchElementException("Задание не найдено");
        return job.view();
    }

    public boolean hasActiveJobs() { return jobs.values().stream().anyMatch(j -> !j.finished()); }

    public record JobView(UUID id, String status, int totalPages, int completedPages,
                          int foundReviews, int savedReviews, Map<String, String> errors, Instant createdAt) { }

    private static final class Job {
        final UUID id = UUID.randomUUID();
        final Instant createdAt = Instant.now();
        final int total;
        final AtomicInteger completed = new AtomicInteger();
        final AtomicInteger found = new AtomicInteger();
        final AtomicInteger saved = new AtomicInteger();
        final ConcurrentMap<String, String> errors = new ConcurrentHashMap<>();
        Job(int total) { this.total = total; }
        boolean finished() { return completed.get() == total; }
        JobView view() {
            int done = completed.get();
            String status = done < total ? "RUNNING" : errors.isEmpty() ? "COMPLETED"
                    : errors.size() == total ? "FAILED" : "PARTIAL";
            return new JobView(id, status, total, done, found.get(), saved.get(), Map.copyOf(errors), createdAt);
        }
    }
}
