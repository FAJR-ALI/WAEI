package org.fadhel.waeiplatform.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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
    private Long id;


    private String name;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 20)
    private OrganizationType type;

    private String sector;

    @Column(unique = true)
    private String email;

    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 20)
    private OrganizationVerificationStatus verificationStatus;

    @Column(length = 1000)
    private String rejectionReason;

    private LocalDateTime verifiedAt;

    private Long verifiedByAccountId;

    private LocalDateTime createdAt;

    private LocalDateTime submittedAt;

    private String documentReference;

    private String documentOriginalName;

    private String documentStoredName;

    private String documentContentType;

    private Long documentSize;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (submittedAt == null) {
            submittedAt = now;
        }
        if (verificationStatus == null) {
            verificationStatus = OrganizationVerificationStatus.PENDING;
        }
    }

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "organization")
    private Set<TrainingProgram> trainingPrograms;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "organization")
    private Set<TrainingRequest> trainingRequests;

    @OneToMany(mappedBy = "organization")
    private List<Account> account;
}
