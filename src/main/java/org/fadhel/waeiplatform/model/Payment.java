package org.fadhel.waeiplatform.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Pattern;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency = "SAR";

    @Column(nullable = false, length = 20)
    @Pattern(regexp = "^(PENDING|PAID|FAILED|CANCELLED|REFUNDED)$", message = "Invalid status")
    private String status;

    @Column(unique = true)
    private String moyasarPaymentId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime paidAt;

    @ManyToOne
    @JoinColumn
    @JsonIgnore
    private TrainingRequest trainingRequest;

}
