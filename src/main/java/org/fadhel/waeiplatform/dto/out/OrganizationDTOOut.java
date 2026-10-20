package org.fadhel.waeiplatform.dto.out;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class OrganizationDTOOut {

    private Long id;
    private String name;
    private String type;
    private String sector;
    private String email;
    private String phoneNumber;
    private String verificationStatus;
    private LocalDateTime createdAt;
    private String documentReference;

}
