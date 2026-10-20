package org.fadhel.waeiplatform.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.IdGeneratorType;

import java.time.LocalDateTime;

@Entity
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer Id;

    @NotEmpty(message = "please enter your full Name ")
    private String fullName;

    @NotEmpty(message = "please enter your Email")
    @Email(message = "Input a valid email")
    @Column(unique = true)
    private String email;

    @NotEmpty(message = "password is required")
    private String password;

    @NotEmpty(message = "choose a role")
    @Pattern(regexp = "PLATFORM_ADMIN|ORG_ADMIN", message = "Role must be PLATFORM_ADMIN or ORG_ADMIN")
    private String role;

    @NotEmpty(message = "Status cannot be empty")
    @Pattern(regexp = "ACTIVE|SUSPENDED|PENDING", message = "Invalid account status")
    private String status;

    private LocalDateTime createdAt;

    @ManyToOne
    @JoinColumn(name = "organization_id")
    @JsonIgnore
    private Organization organization;

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
    }

}
