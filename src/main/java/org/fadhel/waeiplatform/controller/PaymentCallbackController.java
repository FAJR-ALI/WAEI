package org.fadhel.waeiplatform.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fadhel.waeiplatform.api.ApiException;
import org.fadhel.waeiplatform.dto.out.PaymentResponse;
import org.fadhel.waeiplatform.service.PaymentService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Controller
@RequiredArgsConstructor
public class PaymentCallbackController {

    private final PaymentService paymentService;

    @GetMapping("/payment/callback")
    public String callbackWithoutReference(Model model) {
        model.addAttribute("state", "empty");
        return "payment-callback";
    }

    @GetMapping("/payment/callback/{paymentId}")
    public String callback(@PathVariable Long paymentId, Model model) {
        try {
            PaymentResponse payment = paymentService.syncForCallback(paymentId);
            model.addAttribute("state", "ok");
            model.addAttribute("payment", payment);
        } catch (ApiException e) {
            model.addAttribute("state", "error");
            model.addAttribute("message", e.getMessage());
        } catch (ResponseStatusException e) {
            model.addAttribute("state", "error");
            model.addAttribute("message", e.getReason() != null ? e.getReason() : e.getMessage());
        } catch (RuntimeException e) {
            log.warn("Could not confirm payment {} on the return page", paymentId, e);
            model.addAttribute("state", "error");
            model.addAttribute("message", "We could not confirm the payment with Moyasar right now.");
        }

        return "payment-callback";
    }
}
