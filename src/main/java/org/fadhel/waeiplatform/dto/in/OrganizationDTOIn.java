package org.fadhel.waeiplatform.dto.in;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class OrganizationDTOIn {


    @NotEmpty(message = "Organization name must be filled")
    private String name;

    @NotEmpty(message = "Organization type is required")
    @Pattern(regexp = "BENEFICIARY|PROVIDER|BOTH", message = "Type must be BENEFICIARY, PROVIDER or BOTH")
    private String type;

    @NotEmpty(message = "Sector is required")
    private String sector;

    @NotEmpty(message = "Email is required")
    @Email(message = "Invalid Email Format")
    private String email;

    @NotEmpty(message = "Phone number is required")
    @Pattern(regexp = "^05\\d{8}$", message = "Phone number must be a valid Saudi number")
    private String phoneNumber;

    private String documentReference;
}
