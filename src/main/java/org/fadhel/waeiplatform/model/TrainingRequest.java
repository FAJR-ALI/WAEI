package org.fadhel.waeiplatform.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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

    @NotNull
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 30)
    private TrainingRequestStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime requestedAt;

    @NotNull
    @Positive
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal programPrice;

    @Column
    private LocalDateTime scheduledStart;

    @Column
    private LocalDateTime scheduledEnd;
    private LocalDateTime acceptedAt;

    private LocalDateTime completedAt;

    private LocalDateTime cancelledAt;

    @Column(length = 500)
    private String cancellationReason;

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
