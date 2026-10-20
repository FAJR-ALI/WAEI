package org.fadhel.waeiplatform.dto.in;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class NotificationDTOIn {


    @NotEmpty(message = "Channel cannot be empty")
    @Pattern(regexp = "EMAIL|WHATSAPP", message = "Channel must be EMAIL or WHATSAPP")
    private String channel;

    @NotEmpty(message = "Title cannot be empty")
    private String title;

    @NotEmpty(message = "Message cannot be empty")
    private String message;

    @NotEmpty(message = "Recipient cannot be empty")
    private String recipient;

    private Integer accountId;
}
