package org.fadhel.waeiplatform.dto.in;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InternalAnnouncementRequest {

    @NotNull
    private Long trainingRequestId;

    @Size(max = 2, message = "Language is not supported")
    private String lang;

    @Pattern(regexp = "^(notice|email|chat)$", message = "Unsupported announcement channel")
    private String channel;
}