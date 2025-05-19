# ConfX Configuration Examples

## Case 1: Invoicing Feature Flag

First, let's create a config item for invoicing feature flag:

```bash
# Create config item for invoicing feature
curl -X POST "http://localhost:8080/api/v1/projects/1/configs" \
-H "Content-Type: application/json" \
-d '{
  "configKey": "tms.billing.invoicing.enabled",
  "dataType": "BOOLEAN",
  "description": "Enable/disable invoicing feature for businesses",
  "notes": "Controls whether a business can use invoicing functionality"
}' | jq

# Create a version for Development environment
curl -X POST "http://localhost:8080/api/v1/projects/1/environments/1/configs/1/versions" \
-H "Content-Type: application/json" \
-d '{
  "value": "false",
  "changeDescription": "Initial setup for invoicing feature flag - Development",
  "rules": [
    {
      "priority": 1,
      "conditionExpression": "attributes[\"business_id\"] == \"1\"",
      "valueToServe": "true",
      "description": "Enable invoicing for business_id=1"
    },
    {
      "priority": 2,
      "conditionExpression": "attributes[\"business_id\"] == \"2\"",
      "valueToServe": "false",
      "description": "Disable invoicing for business_id=2"
    }
  ]
}' | jq

# Create a version for Production environment (more restrictive)
curl -X POST "http://localhost:8080/api/v1/projects/1/environments/2/configs/1/versions" \
-H "Content-Type: application/json" \
-d '{
  "value": "false",
  "changeDescription": "Initial setup for invoicing feature flag - Production",
  "rules": [
    {
      "priority": 1,
      "conditionExpression": "attributes[\"business_id\"] == \"1\" && attributes[\"region\"] == \"US\"",
      "valueToServe": "true",
      "description": "Enable invoicing for business_id=1 in US region only"
    }
  ]
}' | jq

## Case 2: Invoicing Cycle Configuration

Create a config item for invoicing cycle:

```bash
# Create config item for invoicing cycle
curl -X POST "http://localhost:8080/api/v1/projects/1/configs" \
-H "Content-Type: application/json" \
-d '{
  "configKey": "tms.billing.invoicing.cycle.enabled",
  "dataType": "BOOLEAN",
  "description": "Enable/disable invoicing cycle feature",
  "notes": "Controls whether a business can use invoicing cycle functionality"
}' | jq

# Create a version for Development environment
curl -X POST "http://localhost:8080/api/v1/projects/1/environments/1/configs/2/versions" \
-H "Content-Type: application/json" \
-d '{
  "value": "false",
  "changeDescription": "Initial setup for invoicing cycle feature - Development",
  "rules": [
    {
      "priority": 1,
      "conditionExpression": "attributes[\"business_id\"] == \"1\" || attributes[\"business_id\"] == \"2\"",
      "valueToServe": "true",
      "description": "Enable invoicing cycle for both business_id=1 and business_id=2"
    }
  ]
}' | jq

# Create a version for Production environment
curl -X POST "http://localhost:8080/api/v1/projects/1/environments/2/configs/2/versions" \
-H "Content-Type: application/json" \
-d '{
  "value": "false",
  "changeDescription": "Initial setup for invoicing cycle feature - Production",
  "rules": [
    {
      "priority": 1,
      "conditionExpression": "attributes[\"business_id\"] == \"1\" && attributes[\"region\"] == \"US\"",
      "valueToServe": "true",
      "description": "Enable invoicing cycle for business_id=1 in US region"
    }
  ]
}' | jq

## Case 3: Setting up Config Dependency

Create a dependency between invoicing cycle and invoicing feature:

```bash
# Create config dependency (applies across all environments)
curl -X POST "http://localhost:8080/api/v1/projects/1/configs/2/dependencies" \
-H "Content-Type: application/json" \
-d '{
  "prerequisiteConfigItemId": 1,
  "prerequisiteExpectedValue": "true",
  "description": "Invoicing must be enabled for invoicing cycle to work"
}' | jq

# Test the configurations in Development environment:

# 1. Test for business_id=1 in Development
curl -X POST "http://localhost:8080/api/v1/projects/1/environments/1/evaluate" \
-H "Content-Type: application/json" \
-d '{
  "configKeys": ["tms.billing.invoicing.enabled", "tms.billing.invoicing.cycle.enabled"],
  "attributes": {
    "business_id": "1"
  }
}' | jq

# 2. Test for business_id=2 in Development
curl -X POST "http://localhost:8080/api/v1/projects/1/environments/1/evaluate" \
-H "Content-Type: application/json" \
-d '{
  "configKeys": ["tms.billing.invoicing.enabled", "tms.billing.invoicing.cycle.enabled"],
  "attributes": {
    "business_id": "2"
  }
}' | jq

# Test the configurations in Production environment:

# 3. Test for business_id=1 in Production (US region)
curl -X POST "http://localhost:8080/api/v1/projects/1/environments/2/evaluate" \
-H "Content-Type: application/json" \
-d '{
  "configKeys": ["tms.billing.invoicing.enabled", "tms.billing.invoicing.cycle.enabled"],
  "attributes": {
    "business_id": "1",
    "region": "US"
  }
}' | jq

# 4. Test for business_id=1 in Production (EU region)
curl -X POST "http://localhost:8080/api/v1/projects/1/environments/2/evaluate" \
-H "Content-Type: application/json" \
-d '{
  "configKeys": ["tms.billing.invoicing.enabled", "tms.billing.invoicing.cycle.enabled"],
  "attributes": {
    "business_id": "1",
    "region": "EU"
  }
}' | jq
```

### Expected Results:

1. Development Environment - business_id=1:
   - `tms.billing.invoicing.enabled` = true
   - `tms.billing.invoicing.cycle.enabled` = true

2. Development Environment - business_id=2:
   - `tms.billing.invoicing.enabled` = false
   - `tms.billing.invoicing.cycle.enabled` = false (due to dependency)

3. Production Environment - business_id=1, US region:
   - `tms.billing.invoicing.enabled` = true
   - `tms.billing.invoicing.cycle.enabled` = true

4. Production Environment - business_id=1, EU region:
   - `tms.billing.invoicing.enabled` = false
   - `tms.billing.invoicing.cycle.enabled` = false (due to dependency)

Note: Dependencies are defined at the config level but are evaluated in the context of each environment. This means:
- The dependency relationship (invoicing cycle depends on invoicing) is the same across all environments
- The actual values are evaluated based on the rules defined for each config in that specific environment
- When evaluating the dependent config (invoicing cycle), the system first checks if the prerequisite config (invoicing) is true in the SAME environment 