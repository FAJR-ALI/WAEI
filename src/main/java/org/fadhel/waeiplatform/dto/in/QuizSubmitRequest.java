package org.fadhel.waeiplatform.dto.in;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

// Example: { "answers": ["A", "C", "B"] }  (same order as the questions)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class QuizSubmitRequest {

    @NotEmpty(message = "Answers are required")
    @Size(min = 3, max = 3, message = "You must answer exactly 3 questions")
    private List<@Pattern(regexp = "A|B|C", message = "Each answer must be A, B or C") String> answers;
}
