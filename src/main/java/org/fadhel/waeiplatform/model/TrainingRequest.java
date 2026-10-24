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
    private Integer daysCount;

    @Column(precision = 12, scale = 2)
    private BigDecimal dailyPrice;

    @Column(precision = 4, scale = 1)
    private BigDecimal hoursPerDay;

    @Column
    private LocalDateTime scheduledStart;

    @Column
    private LocalDateTime scheduledEnd;

    // Each value is the scheduledStart for which that manager received a reminder.
    // Nullable fields also work with existing requests and allow a new reminder after rescheduling.
    private LocalDateTime providerReminderForStart;
    private LocalDateTime beneficiaryReminderForStart;
    private LocalDateTime acceptedAt;

    private LocalDateTime completedAt;

    private LocalDateTime cancelledAt;

    @Column(length = 500)
    private String cancellationReason;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 20)
    private OnlineMeetingStatus meetingStatus;

    @Column(length = 64)
    private String zoomMeetingId;

    @Column(length = 1000)
    private String zoomJoinUrl;

    @Column(length = 64)
    private String zoomPassword;

    private LocalDateTime zoomCreatedAt;

    private LocalDateTime zoomLastAttemptAt;

    private Integer zoomAttemptCount;

    @Column(length = 1000)
    private String zoomLastError;

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
