package com.abhinavmehta.confx.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class SetRulesRequestDto {
    @NotNull
    @Size(max = 50, message = "Cannot have more than 50 rules per config version") // Arbitrary limit
    private List<@Valid RuleDto> rules;
} 