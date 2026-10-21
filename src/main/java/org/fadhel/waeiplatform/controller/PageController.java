package org.fadhel.waeiplatform.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

// Opens the HTML pages in src/main/resources/templates (Thymeleaf)
// The data on each page comes from the REST API (/api/v1/...) using JavaScript
@Controller
public class PageController {

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    // Visitor pages (no login needed)

    // Example: /train?id=1
    @GetMapping("/train")
    public String train() {
        return "visitor/train";
    }

    @GetMapping("/programs")
    public String programs() {
        return "visitor/programs";
    }

    @GetMapping("/pricing")
    public String pricing() {
        return "visitor/pricing";
    }

    @GetMapping("/kids")
    public String kids() {
        return "visitor/kids";
    }

    // Example: /story?id=1
    @GetMapping("/story")
    public String story() {
        return "visitor/story";
    }

    @GetMapping("/call")
    public String call() {
        return "visitor/call";
    }
}
