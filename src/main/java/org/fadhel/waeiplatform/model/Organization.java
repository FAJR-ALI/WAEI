package org.fadhel.waeiplatform.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Entity
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor

public class Organization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;


    private String name;

    private String type;

    private String sector;

    @Column(unique = true)
    private String email;

    private String phoneNumber;

    private String verificationStatus;

    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
    }

    private String documentReference;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "organization")
    private Set<TrainingProgram> trainingPrograms;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "organization")
    private Set<TrainingRequest> trainingRequests;

    @OneToMany(mappedBy = "organization")
    private List<Account> account;
}
