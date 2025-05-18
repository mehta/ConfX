package com.abhinavmehta.confx.service;

import com.abhinavmehta.confx.dto.EvaluationContext;
import com.abhinavmehta.confx.entity.Rule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class RuleEvaluationService {

    private final ExpressionParser expressionParser = new SpelExpressionParser();

    /**
     * Evaluates rules against the given context and returns the value from the first matching rule.
     * Rules are assumed to be sorted by priority.
     * @param rules The list of rules to evaluate (sorted by priority).
     * @param evalContext The evaluation context containing attributes.
     * @return The value from the first matching rule, or null if no rules match.
     */
    public String evaluateRules(List<Rule> rules, EvaluationContext evalContext) {
        if (rules == null || rules.isEmpty()) {
            return null;
        }

        // SpEL context will have the attributes map as its root object.
        // Expressions can then be like: "attributes['region'] == 'EU'" 
        // or "attributes['userRoles'].contains('admin')"
        StandardEvaluationContext spelContext = new StandardEvaluationContext(evalContext.getAttributes());

        for (Rule rule : rules) {
            try {
                // Prepending '#' to treat the condition as a SpEL template expression is not needed 
                // if the expression directly uses the root object (the attributes map).
                // Example: attributes['country'] == 'US'
                // Example: attributes['itemCount'] > 10 && attributes['itemCount'] < 20
                // Example: attributes['features'].contains('newUI')
                Boolean matches = expressionParser.parseExpression(rule.getConditionExpression()).getValue(spelContext, Boolean.class);
                if (Boolean.TRUE.equals(matches)) {
                    log.debug("Rule matched (ID {}): '{}'. Serving value: '{}'", rule.getId(), rule.getConditionExpression(), rule.getValueToServe());
                    return rule.getValueToServe();
                }
            } catch (Exception e) {
                // Log the error and continue to the next rule. A malformed expression should not break evaluation of other rules.
                log.error("Error evaluating rule (ID {}): '{}'. Condition: '{}'. Error: {}", 
                          rule.getId(), rule.getDescription(), rule.getConditionExpression(), e.getMessage());
            }
        }
        return null; // No rule matched
    }
    
    /**
     * Evaluates a single condition expression against the given context map.
     * @param conditionExpression The SpEL expression string.
     * @param contextAttributes The map of attributes for evaluation.
     * @return True if the condition evaluates to true, false otherwise or if an error occurs.
     */
    public boolean evaluateSingleCondition(String conditionExpression, Map<String, Object> contextAttributes) {
        if (conditionExpression == null || conditionExpression.isBlank()) {
            return false; // Or throw an error, depending on desired behavior for blank expressions
        }
        StandardEvaluationContext spelContext = new StandardEvaluationContext(contextAttributes);
        try {
            Boolean result = expressionParser.parseExpression(conditionExpression).getValue(spelContext, Boolean.class);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("Error evaluating single condition: '{}'. Error: {}", conditionExpression, e.getMessage());
            return false; // Treat evaluation errors as non-match
        }
    }
} 