package org.fadhel.waeiplatform.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

// Awareness story written by the AI for children. Nothing about the child is stored.
@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class KidsStory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotEmpty(message = "Title is required")
    @Size(max = 150, message = "Title must be at most 150 characters")
    @Column(columnDefinition = "varchar(150) not null")
    private String title;

    // Safety topic, example: "fire at home", "crossing the street"
    @NotEmpty(message = "Topic is required")
    @Column(columnDefinition = "varchar(100) not null")
    private String topic;

    @NotEmpty(message = "Age group is required")
    @Pattern(regexp = "3-5|6-8|9-12", message = "Age group must be 3-5, 6-8 or 9-12")
    @Column(columnDefinition = "varchar(10) not null")
    private String ageGroup;

    @NotEmpty(message = "Content is required")
    @Column(columnDefinition = "text not null")
    private String content;

    // The lesson the child should remember
    @Column(columnDefinition = "varchar(255)")
    private String lesson;

    // PENDING -> admin reviews -> APPROVED (published) or REJECTED
    @NotEmpty(message = "Status is required")
    @Pattern(regexp = "PENDING|APPROVED|REJECTED", message = "Status must be PENDING, APPROVED or REJECTED")
    @Column(columnDefinition = "varchar(20) not null")
    private String status;

    // Why the admin rejected it
    @Column(columnDefinition = "varchar(255)")
    private String adminNote;

    @Column(columnDefinition = "datetime")
    private LocalDateTime createdAt;

    @Column(columnDefinition = "datetime")
    private LocalDateTime publishedAt;

    @PrePersist
    public void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (status == null) status = "PENDING";
    }
}
