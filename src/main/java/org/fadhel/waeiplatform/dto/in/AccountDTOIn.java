package org.fadhel.waeiplatform.dto.in;

import lombok.*;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class AccountDTOIn {

    private String fullName;
    private String email;
    private String password;
    private String role;

}
