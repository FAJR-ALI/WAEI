package org.fadhel.waeiplatform.dto.out;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AuthResponse {

    private String token;
    private String tokenType;
    private Long accountId;
    private String fullName;
    private String email;
    private String role;
    private String status;
    private Long organizationId;
    private String organizationName;
    private String organizationType;
}
