package org.fadhel.waeiplatform.dto.out;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.fadhel.waeiplatform.model.OrganizationType;
import org.fadhel.waeiplatform.model.OrganizationVerificationStatus;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class OrganizationDTOOut {

    private Long id;
    private String name;
    private OrganizationType type;
    private String sector;
    private String email;
    private String phoneNumber;
    private OrganizationVerificationStatus verificationStatus;
    private String rejectionReason;
    private LocalDateTime createdAt;
    private LocalDateTime submittedAt;
    private LocalDateTime verifiedAt;
    private String documentReference;
    private String documentOriginalName;
    private String documentContentType;
    private Long documentSize;
    private boolean documentAvailable;
    private String documentDownloadUrl;

}
