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

    @NotEmpty(message = "Organization name must be filled")
    private String name;

    @NotEmpty
    @Pattern(regexp = "BENEFICIARY|PROVIDER|BOTH", message = "Type must be BENEFICIARY, PROVIDER or BOTH")
    private String type;

    @NotEmpty
    private String sector;

    @NotEmpty
    @Email(message = "Invalid Email Format")
    @Column(unique = true)
    private String email;

    @NotEmpty(message = "you should enter a phone number ")
    @Pattern(regexp = "^05\\d{8}$", message = "Phone number must be a valid Saudi number")
    private String phoneNumber;

    @NotEmpty(message = "Verification status cannot be empty")
    @Pattern(regexp = "PENDING|VERIFIED|REJECTED", message = "Invalid verification status")
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
