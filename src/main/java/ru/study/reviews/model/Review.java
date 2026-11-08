package ru.study.reviews.model;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record Review(Long id, String sourceUrl, String author, String text,
                     double rating, LocalDate date, OffsetDateTime collectedAt) { }
