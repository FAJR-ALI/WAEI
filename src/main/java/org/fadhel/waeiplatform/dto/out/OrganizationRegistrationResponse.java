package org.fadhel.waeiplatform.dto.out;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.fadhel.waeiplatform.model.OrganizationVerificationStatus;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class OrganizationRegistrationResponse {

    private Long id;
    private String name;
    private OrganizationVerificationStatus verificationStatus;
    private String message;
}
