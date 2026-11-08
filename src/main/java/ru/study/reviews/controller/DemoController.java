package ru.study.reviews.controller;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@RestController
public class DemoController {
    @GetMapping(value = "/demo/product/{id}", produces = "text/html;charset=UTF-8")
    public String product(@PathVariable int id) throws IOException {
        if (id < 1 || id > 2) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return new ClassPathResource("demo/product-" + id + ".html").getContentAsString(StandardCharsets.UTF_8);
    }
}
