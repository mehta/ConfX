package com.abhinavmehta.confx.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuleDto {
    private Long id; // Only in response

    @NotNull(message = "Priority cannot be null")
    @Min(value = 1, message = "Priority must be a positive integer")
    private Integer priority;

    @NotBlank(message = "Condition expression cannot be blank")
    @Size(max = 4000, message = "Condition expression cannot exceed 4000 characters")
    private String conditionExpression;

    @NotBlank(message = "Value to serve cannot be blank")
    @Size(max = 4000, message = "Value to serve cannot exceed 4000 characters") // TEXT type, but good to have a limit
    private String valueToServe;

    @Size(max = 1000, message = "Rule description cannot exceed 1000 characters")
    private String description;

    private Long createdAt; // Only in response
    private Long updatedAt; // Only in response
} 