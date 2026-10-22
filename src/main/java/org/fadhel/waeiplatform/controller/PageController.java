package org.fadhel.waeiplatform.controller;

import org.fadhel.waeiplatform.model.OrganizationType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
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

    @GetMapping("/register")
    public String register() {
        return "redirect:/login#join-waei";
    }

    @GetMapping("/register/beneficiary")
    public String registerBeneficiary(Model model) {
        return registrationPage(model, OrganizationType.BENEFICIARY);
    }

    @GetMapping("/register/provider")
    public String registerProvider(Model model) {
        return registrationPage(model, OrganizationType.PROVIDER);
    }

    private String registrationPage(Model model, OrganizationType organizationType) {
        model.addAttribute("organizationType", organizationType.name());
        model.addAttribute("isProvider", organizationType == OrganizationType.PROVIDER);
        return "register";
    }

    // Organization and platform dashboards (the JWT is validated by the REST API)
    @GetMapping("/dashboard/admin")
    public String adminDashboard() {
        return "dashboard/admin";
    }

    @GetMapping("/dashboard/beneficiary")
    public String beneficiaryDashboard() {
        return "dashboard/beneficiary";
    }

    @GetMapping("/dashboard/provider")
    public String providerDashboard() {
        return "dashboard/provider";
    }

    // Visitor pages (no login needed)

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

    // Emergency guide
    @GetMapping("/guide")
    public String guide() {
        return "visitor/guide";
    }

    @GetMapping("/guide/detail")
    public String guideDetail() {
        return "visitor/guide-detail";
    }

    // Full-year awareness calendar
    @GetMapping("/calendar")
    public String calendar() {
        return "visitor/calendar";
    }
}
