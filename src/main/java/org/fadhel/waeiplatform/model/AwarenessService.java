package org.fadhel.waeiplatform.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AwarenessService {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotEmpty(message = "Title is required")
    @Size(min = 3, max = 100, message = "Title must be between 3 and 100 characters")
    @Column(columnDefinition = "varchar(100) not null")
    private String title;

    @NotEmpty(message = "Category is required")
    @Column(columnDefinition = "varchar(50) not null")
    private String category;

    @NotEmpty(message = "Description is required")
    @Column(columnDefinition = "text not null")
    private String description;

    // Link to an official video that shows the correct way (Red Crescent, Civil Defense...)
    @Column(columnDefinition = "varchar(255)")
    private String officialVideoUrl;

    @Column(columnDefinition = "boolean default true")
    private Boolean isActive = true;

    // 1 service -> 0..* sessions
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "awarenessService")
    @JsonIgnore
    private Set<AwarenessSession> sessions;

    // 0..1 service -> 0..* calendar events (promotes)
    @OneToMany(mappedBy = "awarenessService")
    @JsonIgnore
    private Set<AwarenessCalendarEvent> calendarEvents;
}
