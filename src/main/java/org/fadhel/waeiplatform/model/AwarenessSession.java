package org.fadhel.waeiplatform.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AwarenessSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(columnDefinition = "datetime")
    private LocalDateTime startedAt;

    @Column(columnDefinition = "datetime")
    private LocalDateTime completedAt;

    @DecimalMin(value = "0.0", message = "Score cannot be less than 0")
    @DecimalMax(value = "100.0", message = "Score cannot be more than 100")
    private Double score;

    @Column(columnDefinition = "text")
    private String resultSummary;

    @Column(columnDefinition = "text")
    private String recommendations;

    // Quiz result: how many of the 3 questions were answered correctly (null = not taken yet)
    private Integer quizScore;

    @NotNull(message = "Consent must be specified")
    @Column(columnDefinition = "boolean not null")
    private Boolean consentGiven;

    @ManyToOne
    @JoinColumn(name = "awareness_service_id", referencedColumnName = "id", nullable = false)
    @JsonIgnore
    private AwarenessService awarenessService;

    // 1 session -> 3 quiz questions about the trainee's own mistakes
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "awarenessSession")
    @JsonIgnore
    private Set<SessionQuizQuestion> quizQuestions;

    @PrePersist
    public void onCreate() {
        if (startedAt == null) startedAt = LocalDateTime.now();
    }
}
