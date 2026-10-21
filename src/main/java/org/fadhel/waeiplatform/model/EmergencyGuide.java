package org.fadhel.waeiplatform.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;


@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class EmergencyGuide {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotEmpty(message = "Title is required")
    @Size(max = 100, message = "Title must be at most 100 characters")
    @Column(columnDefinition = "varchar(100) not null unique")
    private String title;

    @NotEmpty(message = "Category is required")
    @Pattern(regexp = "FIRST_AID|KIDS|FIRE|CHRONIC|WEATHER", message = "Category must be FIRST_AID, KIDS, FIRE, CHRONIC or WEATHER")
    @Column(columnDefinition = "varchar(20) not null")
    private String category;

    @NotEmpty(message = "Summary is required")
    @Column(columnDefinition = "varchar(255) not null")
    private String summary;

    @NotEmpty(message = "Signs are required")
    @Column(columnDefinition = "text not null")
    private String signs;

    @NotEmpty(message = "Steps are required")
    @Column(columnDefinition = "text not null")
    private String steps;

    @Column(columnDefinition = "text")
    private String donts;

    @Column(columnDefinition = "text")
    private String whenToCall;

    @Column(columnDefinition = "boolean default false")
    private Boolean isUrgent = false;

    // Which icon the page shows
    @Pattern(regexp = "HEART|THROAT|BRAIN|FLAME|PULSE|SUN|DROP|BABY", message = "Invalid icon")
    @Column(columnDefinition = "varchar(20)")
    private String icon;

    @Column(columnDefinition = "varchar(150)")
    private String sourceName;

    @Column(columnDefinition = "varchar(255)")
    private String sourceUrl;

    // Only published cases are shown to visitors
    @Column(columnDefinition = "boolean default false")
    private Boolean isPublished = false;

    @Column(columnDefinition = "datetime")
    private LocalDateTime updatedAt;

    // Optional: the video training for this case ("تدرّب عليها" button)
    @ManyToOne
    @JoinColumn(name = "awareness_service_id", referencedColumnName = "id")
    @JsonIgnore
    private AwarenessService awarenessService;

    @PrePersist
    @PreUpdate
    public void onSave() {
        updatedAt = LocalDateTime.now();
    }
}
