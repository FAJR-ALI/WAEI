package org.fadhel.waeiplatform.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.fadhel.waeiplatform.api.ApiResponse;
import org.fadhel.waeiplatform.dto.in.PaymentRequest;
import org.fadhel.waeiplatform.dto.in.PaymentUpdateRequest;
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
        paymentService.addPayment(request);
        return ResponseEntity.status(200).body(new ApiResponse("Payment added successfully"));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<?> updatePayment(@PathVariable Long id, @Valid @RequestBody PaymentUpdateRequest request) {
        paymentService.updatePayment(id, request);
        return ResponseEntity.status(200).body(new ApiResponse("Payment updated successfully"));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deletePayment(@PathVariable Long id) {
        paymentService.deletePayment(id);
        return ResponseEntity.status(200).body(new ApiResponse("Payment deleted successfully"));
    }
}
