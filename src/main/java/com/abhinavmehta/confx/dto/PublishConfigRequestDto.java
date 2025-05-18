package com.abhinavmehta.confx.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PublishConfigRequestDto {

    @NotBlank(message = "Value cannot be blank. For a boolean 'false', send 'false' as string.") // Value is TEXT, so even booleans are strings here.
    private String value;

    @Size(max = 1000, message = "Change description cannot exceed 1000 characters")
    private String changeDescription; // Optional "commit message"
} 