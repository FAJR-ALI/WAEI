package org.fadhel.waeiplatform.dto.out;

import lombok.*;

import java.time.LocalDateTime;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class AccountDTOOut {

    private Integer id;
    private String fullName;
    private String email;
    private String role;
    private String status;
    private LocalDateTime createdAt;

}
