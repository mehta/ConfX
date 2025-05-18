package com.abhinavmehta.confx.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationContext {
    // Arbitrary attributes provided by the client SDK for rule evaluation.
    // Examples: userId, region, email, custom attributes.
    private Map<String, Object> attributes;
} 