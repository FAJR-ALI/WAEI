package org.fadhel.waeiplatform.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "training_requests")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TrainingRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer participantCount;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(nullable = false, length = 30)
    @Pattern(regexp = "^(PENDING|ACCEPTED|REJECTED|PAYMENT_PENDING|PAID|IN_PROGRESS|COMPLETED|CANCELLED)$", message = "Invalid status")
    private String status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime requestedAt;

    @NotNull
    @Positive
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal programPrice;

    @ManyToOne
    @JoinColumn
    @JsonIgnore
    private TrainingProgram trainingProgram;

    @ManyToOne(optional = false)
    @JoinColumn
    @JsonIgnore
    private Account account;

    @ManyToOne
    @JoinColumn
    @JsonIgnore
    private Organization organization;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "trainingRequest")
    private Set<Payment> payments;

}
