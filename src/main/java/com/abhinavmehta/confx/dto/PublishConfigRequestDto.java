package com.abhinavmehta.confx.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class PublishConfigRequestDto {

    @NotBlank(message = "Value cannot be blank. For a boolean 'false', send 'false' as string.") // Value is TEXT, so even booleans are strings here.
    private String value;

    @Size(max = 1000, message = "Change description cannot exceed 1000 characters")
    private String changeDescription; // Optional "commit message"

    @NotNull
    @Size(max = 50, message = "Cannot have more than 50 rules per config version")
    private List<@Valid RuleDto> rules = new ArrayList<>(); // List of rules for this version
} 