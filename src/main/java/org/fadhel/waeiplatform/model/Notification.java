package org.fadhel.waeiplatform.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotEmpty(message = "Channel cannot be empty")
    @Pattern(regexp = "EMAIL|WHATSAPP", message = "Channel must be EMAIL or WHATSAPP")
    private String channel;

    @NotEmpty(message = "Title cannot be empty")
    private String title;

    @NotEmpty(message = "Message cannot be empty")
    private String message;

    @NotEmpty(message = "Recipient cannot be empty")
    private String recipient;

    @NotEmpty(message = "Status cannot be empty")
    @Pattern(regexp = "PENDING|SENT|FAILED", message = "Invalid notification status")
    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime sentAt;

    @ManyToOne
    @JoinColumn(name = "account_id")
    @JsonIgnore
    private Account account;

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
    }

}
