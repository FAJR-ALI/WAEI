package org.fadhel.waeiplatform.model;


import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.Set;

@Entity
@Table(name = "training_programs")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TrainingProgram {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, length = 100)
    private String category;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(nullable = false, length = 20)
    private String deliveryMode;

    @Column(nullable = false)
    private Boolean isActive = true;

    @ManyToOne
    @JoinColumn
    @JsonIgnore
    private Organization organization;

    @OneToMany(mappedBy = "trainingProgram")
    private Set<TrainingRequest> requests;
}
