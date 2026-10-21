package org.fadhel.waeiplatform.dto.in;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.fadhel.waeiplatform.model.OrganizationType;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class OrganizationResubmitRequest {

    @NotBlank(message = "Organization name is required")
    private String name;

    @NotNull(message = "Organization type is required")
    private OrganizationType type;

    @NotBlank(message = "Sector is required")
    private String sector;

    @NotBlank(message = "Organization email is required")
    @Email(message = "Invalid organization email format")
    private String email;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^05\\d{8}$", message = "Phone number must be a valid Saudi number")
    private String phoneNumber;

    private String documentReference;

    @NotBlank(message = "Organization administrator email is required")
    @Email(message = "Invalid organization administrator email format")
    private String adminEmail;

    @NotBlank(message = "Organization administrator password is required")
    private String password;

    private MultipartFile document;
}
