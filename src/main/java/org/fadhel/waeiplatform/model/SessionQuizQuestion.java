package org.fadhel.waeiplatform.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// The AI writes 3 questions about the trainee's mistakes after each video evaluation
@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SessionQuizQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotEmpty(message = "Question is required")
    @Column(columnDefinition = "text not null")
    private String question;

    @NotEmpty(message = "Option A is required")
    @Column(columnDefinition = "varchar(255) not null")
    private String optionA;

    @NotEmpty(message = "Option B is required")
    @Column(columnDefinition = "varchar(255) not null")
    private String optionB;

    @NotEmpty(message = "Option C is required")
    @Column(columnDefinition = "varchar(255) not null")
    private String optionC;

    @NotEmpty(message = "Correct answer is required")
    @Pattern(regexp = "A|B|C", message = "Correct answer must be A, B or C")
    @Column(columnDefinition = "varchar(1) not null")
    private String correctAnswer;

    @Column(columnDefinition = "text")
    private String explanation;

    // What the trainee chose (null = not answered yet)
    @Column(columnDefinition = "varchar(1)")
    private String userAnswer;

    @ManyToOne
    @JoinColumn(name = "awareness_session_id", referencedColumnName = "id", nullable = false)
    @JsonIgnore
    private AwarenessSession awarenessSession;
}