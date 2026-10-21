package org.fadhel.waeiplatform.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.fadhel.waeiplatform.api.ApiResponse;
import org.fadhel.waeiplatform.dto.in.PaymentRequest;
import org.fadhel.waeiplatform.integration.MoyasarClient;
import org.fadhel.waeiplatform.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping("/get")
    public ResponseEntity<?> getAllPayments() {
        return ResponseEntity.status(200).body(paymentService.getAllPayments());
    }

    @GetMapping("/get/{id}")
    public ResponseEntity<?> getPaymentById(@PathVariable Long id) {
        return ResponseEntity.status(200).body(paymentService.getPaymentById(id));
    }

    @GetMapping("/get-by-request/{trainingRequestId}")
    public ResponseEntity<?> getPaymentsByTrainingRequest(@PathVariable Long trainingRequestId) {
        return ResponseEntity.status(200).body(paymentService.getPaymentsByTrainingRequest(trainingRequestId));
    }

    @GetMapping("/get-by-status/{status}")
    public ResponseEntity<?> getPaymentsByStatus(@PathVariable String status) {
        return ResponseEntity.status(200).body(paymentService.getPaymentsByStatus(status));
    }

    @GetMapping("/get-by-moyasar/{moyasarPaymentId}")
    public ResponseEntity<?> getPaymentByMoyasarId(@PathVariable String moyasarPaymentId) {
        return ResponseEntity.status(200).body(paymentService.getPaymentByMoyasarId(moyasarPaymentId));
    }

    @PostMapping("/add")
    public ResponseEntity<?> addPayment(@Valid @RequestBody PaymentRequest request) {
        return ResponseEntity.status(200).body(paymentService.addPayment(request));
    }

    @PostMapping("/{id}/refund")
    public ResponseEntity<?> refundPayment(@PathVariable Long id) {
        return ResponseEntity.status(200).body(paymentService.refundPayment(id));
    }

    @PostMapping("/{id}/sync")
    public ResponseEntity<?> syncPayment(@PathVariable Long id) {
        return ResponseEntity.status(200).body(paymentService.syncPayment(id));
    }

    @PostMapping("/sync-by-invoice/{invoiceId}")
    public ResponseEntity<?> syncByInvoice(@PathVariable String invoiceId) {
        return ResponseEntity.status(200).body(paymentService.syncByInvoice(invoiceId));
    }

    @PostMapping("/webhook/moyasar")
    public ResponseEntity<?> moyasarWebhook(@RequestBody MoyasarClient.MoyasarWebhook payload) {
        paymentService.handleWebhook(payload);
        return ResponseEntity.status(200).body(new ApiResponse("Webhook processed"));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deletePayment(@PathVariable Long id) {
        paymentService.deletePayment(id);
        return ResponseEntity.status(200).body(new ApiResponse("Payment deleted successfully"));
    }
}
