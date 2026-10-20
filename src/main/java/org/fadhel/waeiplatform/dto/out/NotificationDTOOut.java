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
public class NotificationDTOOut {

    private Long id;

    private String channel;

    private String title;

    private String message;

    private String recipient;

    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime sentAt;

    private Long accountId;

}
