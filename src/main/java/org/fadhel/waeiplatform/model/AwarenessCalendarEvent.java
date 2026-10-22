package org.fadhel.waeiplatform.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AwarenessCalendarEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotEmpty(message = "Title is required")
    @Size(min = 3, max = 100, message = "Title must be between 3 and 100 characters")
    @Column(columnDefinition = "varchar(100) not null")
    private String title;

    @Size(max = 100, message = "English title must be at most 100 characters")
    @Column(columnDefinition = "varchar(100)")
    private String titleEn;

    @NotEmpty(message = "Category is required")
    @Column(columnDefinition = "varchar(50) not null")
    private String category;

    @Column(columnDefinition = "text")
    private String description;

    @Column(columnDefinition = "text")
    private String descriptionEn;

    @NotNull(message = "Event date is required")
    @Column(columnDefinition = "date not null")
    private LocalDate eventDate;

    // FIXED:11-14, NTH_WEEKDAY:09:SATURDAY:2, LAST_WEEKDAY:01:SUNDAY
    @Size(max = 40, message = "Date rule must be at most 40 characters")
    @Column(columnDefinition = "varchar(40)")
    private String dateRule;

    // Stable key for the built-in calendar entries, so the startup seeder never duplicates them
    @Size(max = 120, message = "Slug must be at most 120 characters")
    @Column(columnDefinition = "varchar(120)", unique = true)
    private String slug;

    @NotEmpty(message = "Recurrence pattern is required")
    @Pattern(regexp = "ANNUAL|ONE_TIME", message = "Recurrence pattern must be ANNUAL or ONE_TIME")
    @Column(columnDefinition = "varchar(20) not null")
    private String recurrencePattern;

    @Column(columnDefinition = "varchar(255)")
    private String sourceUrl;

    @Column(columnDefinition = "boolean default false")
    private Boolean isPublished = false;

    // True for the built-in awareness days that ship with Waei
    @Column(columnDefinition = "boolean default false")
    private Boolean isSystem = false;

    @ManyToOne
    @JoinColumn(name = "awareness_service_id", referencedColumnName = "id")
    @JsonIgnore
    private AwarenessService awarenessService;
}
