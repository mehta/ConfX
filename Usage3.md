# ConfX Advanced Configuration Examples

This guide demonstrates complex configuration scenarios with dependencies across different environments.

## Setup Base Environment

First, let's set up our project and environments:

```bash
# Set the base URL
export BASE_URL="http://localhost:8080/api/v1"

# Create the TMS Project
PROJECT_RESPONSE=$(curl -X POST "${BASE_URL}/projects" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Global TMS Platform",
    "description": "Transport Management System with Advanced Configurations"
  }')
export PROJECT_ID=$(echo "$PROJECT_RESPONSE" | jq -r '.id')
echo "Created project with ID: $PROJECT_ID"

# Create Development Environment
DEV_ENV_RESPONSE=$(curl -X POST "${BASE_URL}/projects/${PROJECT_ID}/environments" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Development",
    "description": "Development Environment",
    "colorTag": "#3498DB"
  }')
export DEV_ENV_ID=$(echo "$DEV_ENV_RESPONSE" | jq -r '.id')
echo "Created Development environment with ID: $DEV_ENV_ID"

# Create Staging Environment
STAGING_ENV_RESPONSE=$(curl -X POST "${BASE_URL}/projects/${PROJECT_ID}/environments" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Staging",
    "description": "Staging Environment",
    "colorTag": "#F1C40F"
  }')
export STAGING_ENV_ID=$(echo "$STAGING_ENV_RESPONSE" | jq -r '.id')
echo "Created Staging environment with ID: $STAGING_ENV_ID"

# Create Production Environment
PROD_ENV_RESPONSE=$(curl -X POST "${BASE_URL}/projects/${PROJECT_ID}/environments" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Production",
    "description": "Production Environment",
    "colorTag": "#E74C3C"
  }')
export PROD_ENV_ID=$(echo "$PROD_ENV_RESPONSE" | jq -r '.id')
echo "Created Production environment with ID: $PROD_ENV_ID"
```

## Scenario: Multi-Level Feature Dependencies

We'll create a complex scenario with multiple dependent features:

1. Base Feature: User Authentication System
2. Dependent Feature 1: Advanced Security Features
3. Dependent Feature 2: SSO Integration
4. Dependent Feature 3: Multi-Factor Authentication (depends on both Advanced Security and SSO)

### 1. Create Config Items

```bash
# 1. User Authentication System
AUTH_CONFIG_RESPONSE=$(curl -X POST "${BASE_URL}/projects/${PROJECT_ID}/configs" \
  -H "Content-Type: application/json" \
  -d '{
    "configKey": "tms.auth.system.enabled",
    "dataType": "BOOLEAN",
    "description": "Base authentication system enablement",
    "notes": "Core authentication feature flag"
  }')
export AUTH_CONFIG_ID=$(echo "$AUTH_CONFIG_RESPONSE" | jq -r '.id')
echo "Created Auth Config with ID: $AUTH_CONFIG_ID"

# 2. Advanced Security Features
SECURITY_CONFIG_RESPONSE=$(curl -X POST "${BASE_URL}/projects/${PROJECT_ID}/configs" \
  -H "Content-Type: application/json" \
  -d '{
    "configKey": "tms.auth.security.advanced.enabled",
    "dataType": "BOOLEAN",
    "description": "Advanced security features",
    "notes": "Enhanced security capabilities"
  }')
export SECURITY_CONFIG_ID=$(echo "$SECURITY_CONFIG_RESPONSE" | jq -r '.id')
echo "Created Security Config with ID: $SECURITY_CONFIG_ID"

# 3. SSO Integration
SSO_CONFIG_RESPONSE=$(curl -X POST "${BASE_URL}/projects/${PROJECT_ID}/configs" \
  -H "Content-Type: application/json" \
  -d '{
    "configKey": "tms.auth.sso.enabled",
    "dataType": "BOOLEAN",
    "description": "SSO integration feature",
    "notes": "Single Sign-On capabilities"
  }')
export SSO_CONFIG_ID=$(echo "$SSO_CONFIG_RESPONSE" | jq -r '.id')
echo "Created SSO Config with ID: $SSO_CONFIG_ID"

# 4. Multi-Factor Authentication
MFA_CONFIG_RESPONSE=$(curl -X POST "${BASE_URL}/projects/${PROJECT_ID}/configs" \
  -H "Content-Type: application/json" \
  -d '{
    "configKey": "tms.auth.mfa.enabled",
    "dataType": "BOOLEAN",
    "description": "Multi-factor authentication feature",
    "notes": "MFA capabilities requiring both advanced security and SSO"
  }')
export MFA_CONFIG_ID=$(echo "$MFA_CONFIG_RESPONSE" | jq -r '.id')
echo "Created MFA Config with ID: $MFA_CONFIG_ID"

```

### 2. Set Up Dependencies

```bash
# Advanced Security depends on Auth System
curl -X POST "${BASE_URL}/projects/${PROJECT_ID}/dependencies/for/${SECURITY_CONFIG_ID}" \
  -H "Content-Type: application/json" \
  -d "{
    \"prerequisiteConfigItemId\": ${AUTH_CONFIG_ID},
    \"prerequisiteExpectedValue\": \"true\",
    \"description\": \"Advanced security requires auth system\"
}"
# SSO depends on Auth System
curl -X POST "${BASE_URL}/projects/${PROJECT_ID}/dependencies/for/${SSO_CONFIG_ID}" \
  -H "Content-Type: application/json" \
  -d "{
    \"prerequisiteConfigItemId\": ${AUTH_CONFIG_ID},
    \"prerequisiteExpectedValue\": \"true\",
    \"description\": \"SSO requires auth system\"
}"
# MFA depends on Advanced Security
curl -X POST "${BASE_URL}/projects/${PROJECT_ID}/dependencies/for/${MFA_CONFIG_ID}" \
  -H "Content-Type: application/json" \
  -d "{
    \"prerequisiteConfigItemId\": ${SECURITY_CONFIG_ID},
    \"prerequisiteExpectedValue\": \"true\",
    \"description\": \"MFA requires advanced security\"
}"

# MFA also depends on SSO
curl -X POST "${BASE_URL}/projects/${PROJECT_ID}/dependencies/for/${MFA_CONFIG_ID}" \
  -H "Content-Type: application/json" \
  -d "{
    \"prerequisiteConfigItemId\": ${SSO_CONFIG_ID},
    \"prerequisiteExpectedValue\": \"true\",
    \"description\": \"MFA requires SSO\"
}"
```

### 3. Create Environment-Specific Versions

```bash
# Development Environment - Most permissive
# Auth System Config - Dev
curl -X POST "${BASE_URL}/projects/${PROJECT_ID}/environments/${DEV_ENV_ID}/configs/${AUTH_CONFIG_ID}/versions" \
-H "Content-Type: application/json" \
-d '{
  "value": "true",
  "changeDescription": "Enable auth system for all in Dev",
  "rules": []
}'

# Advanced Security Config - Dev
curl -X POST "${BASE_URL}/projects/${PROJECT_ID}/environments/${DEV_ENV_ID}/configs/${SECURITY_CONFIG_ID}/versions" \
-H "Content-Type: application/json" \
-d '{
  "value": "true",
  "changeDescription": "Enable advanced security for all in Dev",
  "rules": []
}'

# SSO Config - Dev
curl -X POST "${BASE_URL}/projects/${PROJECT_ID}/environments/${DEV_ENV_ID}/configs/${SSO_CONFIG_ID}/versions" \
-H "Content-Type: application/json" \
-d '{
  "value": "true",
  "changeDescription": "Enable SSO for all in Dev",
  "rules": []
}'

# MFA Config - Dev
curl -X POST "${BASE_URL}/projects/${PROJECT_ID}/environments/${DEV_ENV_ID}/configs/${MFA_CONFIG_ID}/versions" \
-H "Content-Type: application/json" \
-d '{
  "value": "true",
  "changeDescription": "Enable MFA for all in Dev",
  "rules": []
}'

# Staging Environment - More restrictive
# Auth System Config - Staging
curl -X POST "${BASE_URL}/projects/${PROJECT_ID}/environments/${STAGING_ENV_ID}/configs/${AUTH_CONFIG_ID}/versions" \
-H "Content-Type: application/json" \
-d '{
  "value": "false",
  "changeDescription": "Enable auth system for specific business units in Staging",
  "rules": [
    {
      "priority": 1,
      "conditionExpression": "attributes[\"business_unit\"] in [\"BU_1\", \"BU_2\"]",
      "valueToServe": "true",
      "description": "Enable for BU_1 and BU_2"
    }
  ]
}'

# Advanced Security Config - Staging
curl -X POST "${BASE_URL}/projects/${PROJECT_ID}/environments/${STAGING_ENV_ID}/configs/${SECURITY_CONFIG_ID}/versions" \
-H "Content-Type: application/json" \
-d '{
  "value": "false",
  "changeDescription": "Enable advanced security for specific business units in Staging",
  "rules": [
    {
      "priority": 1,
      "conditionExpression": "attributes[\"business_unit\"] == \"BU_1\"",
      "valueToServe": "true",
      "description": "Enable only for BU_1"
    }
  ]
}'

# SSO Config - Staging
curl -X POST "${BASE_URL}/projects/${PROJECT_ID}/environments/${STAGING_ENV_ID}/configs/${SSO_CONFIG_ID}/versions" \
-H "Content-Type: application/json" \
-d '{
  "value": "false",
  "changeDescription": "Enable SSO for specific business units in Staging",
  "rules": [
    {
      "priority": 1,
      "conditionExpression": "attributes[\"business_unit\"] in [\"BU_1\", \"BU_2\"]",
      "valueToServe": "true",
      "description": "Enable for BU_1 and BU_2"
    }
  ]
}'

# MFA Config - Staging
curl -X POST "${BASE_URL}/projects/${PROJECT_ID}/environments/${STAGING_ENV_ID}/configs/${MFA_CONFIG_ID}/versions" \
-H "Content-Type: application/json" \
-d '{
  "value": "false",
  "changeDescription": "Enable MFA for specific business units in Staging",
  "rules": [
    {
      "priority": 1,
      "conditionExpression": "attributes[\"business_unit\"] == \"BU_1\"",
      "valueToServe": "true",
      "description": "Enable only for BU_1"
    }
  ]
}'

# Production Environment - Most restrictive
# Auth System Config - Prod
curl -X POST "${BASE_URL}/projects/${PROJECT_ID}/environments/${PROD_ENV_ID}/configs/${AUTH_CONFIG_ID}/versions" \
-H "Content-Type: application/json" \
-d '{
  "value": "false",
  "changeDescription": "Enable auth system with region restrictions in Prod",
  "rules": [
    {
      "priority": 1,
      "conditionExpression": "attributes[\"business_unit\"] in [\"BU_1\", \"BU_2\"] && attributes[\"region\"] == \"US\"",
      "valueToServe": "true",
      "description": "Enable for US region BU_1 and BU_2"
    }
  ]
}'

# Advanced Security Config - Prod
curl -X POST "${BASE_URL}/projects/${PROJECT_ID}/environments/${PROD_ENV_ID}/configs/${SECURITY_CONFIG_ID}/versions" \
-H "Content-Type: application/json" \
-d '{
  "value": "false",
  "changeDescription": "Enable advanced security with strict rules in Prod",
  "rules": [
    {
      "priority": 1,
      "conditionExpression": "attributes[\"business_unit\"] == \"BU_1\" && attributes[\"region\"] == \"US\" && attributes[\"compliance_level\"] == \"HIGH\"",
      "valueToServe": "true",
      "description": "Enable for US region BU_1 with high compliance"
    }
  ]
}'

# SSO Config - Prod
curl -X POST "${BASE_URL}/projects/${PROJECT_ID}/environments/${PROD_ENV_ID}/configs/${SSO_CONFIG_ID}/versions" \
-H "Content-Type: application/json" \
-d '{
  "value": "false",
  "changeDescription": "Enable SSO with region restrictions in Prod",
  "rules": [
    {
      "priority": 1,
      "conditionExpression": "attributes[\"business_unit\"] in [\"BU_1\", \"BU_2\"] && attributes[\"region\"] == \"US\"",
      "valueToServe": "true",
      "description": "Enable for US region BU_1 and BU_2"
    }
  ]
}'

# MFA Config - Prod
curl -X POST "${BASE_URL}/projects/${PROJECT_ID}/environments/${PROD_ENV_ID}/configs/${MFA_CONFIG_ID}/versions" \
-H "Content-Type: application/json" \
-d '{
  "value": "false",
  "changeDescription": "Enable MFA with strict rules in Prod",
  "rules": [
    {
      "priority": 1,
      "conditionExpression": "attributes[\"business_unit\"] == \"BU_1\" && attributes[\"region\"] == \"US\" && attributes[\"compliance_level\"] == \"HIGH\"",
      "valueToServe": "true",
      "description": "Enable for US region BU_1 with high compliance"
    }
  ]
}'
```

### 4. Test Different Scenarios

```bash
# Function to test configuration
test_config() {
    local env_id=$1
    local env_name=$2
    local scenario=$3
    local attributes=$4
    
    echo "Testing in ${env_name} - ${scenario}"
    curl -X POST "${BASE_URL}/evaluate/projects/${PROJECT_ID}/environments/${env_id}/configs/tms.auth.system.enabled" \
    -H "Content-Type: application/json" \
    -d "{
      \"attributes\": ${attributes}
    }" | jq
    
    curl -X POST "${BASE_URL}/evaluate/projects/${PROJECT_ID}/environments/${env_id}/configs/tms.auth.security.advanced.enabled" \
    -H "Content-Type: application/json" \
    -d "{
      \"attributes\": ${attributes}
    }" | jq
    
    curl -X POST "${BASE_URL}/evaluate/projects/${PROJECT_ID}/environments/${env_id}/configs/tms.auth.sso.enabled" \
    -H "Content-Type: application/json" \
    -d "{
      \"attributes\": ${attributes}
    }" | jq
    
    curl -X POST "${BASE_URL}/evaluate/projects/${PROJECT_ID}/environments/${env_id}/configs/tms.auth.mfa.enabled" \
    -H "Content-Type: application/json" \
    -d "{
      \"attributes\": ${attributes}
    }" | jq
    echo "----------------------------------------"
}


# Development Environment Tests
echo "=== Development Environment Tests ==="
test_config $DEV_ENV_ID "Development" "Any Business Unit" '{
  "business_unit": "BU_3",
  "region": "EU"
}'

# Staging Environment Tests
echo "=== Staging Environment Tests ==="
test_config $STAGING_ENV_ID "Staging" "BU_1 (should enable all)" '{
  "business_unit": "BU_1"
}'
test_config $STAGING_ENV_ID "Staging" "BU_2 (should enable auth and SSO only)" '{
  "business_unit": "BU_2"
}'
test_config $STAGING_ENV_ID "Staging" "BU_3 (should disable all)" '{
  "business_unit": "BU_3"
}'

# Production Environment Tests
echo "=== Production Environment Tests ==="
test_config $PROD_ENV_ID "Production" "BU_1 US HIGH (should enable all)" '{
  "business_unit": "BU_1",
  "region": "US",
  "compliance_level": "HIGH"
}'
test_config $PROD_ENV_ID "Production" "BU_1 US LOW (should enable auth and SSO only)" '{
  "business_unit": "BU_1",
  "region": "US",
  "compliance_level": "LOW"
}'
test_config $PROD_ENV_ID "Production" "BU_1 EU HIGH (should disable all)" '{
  "business_unit": "BU_1",
  "region": "EU",
  "compliance_level": "HIGH"
}'
test_config $PROD_ENV_ID "Production" "BU_2 US (should enable auth and SSO only)" '{
  "business_unit": "BU_2",
  "region": "US"
}'
```

### Expected Results

1. Development Environment:
   - All features enabled for any business unit/region

2. Staging Environment:
   - BU_1: All features enabled (auth, security, SSO, MFA)
   - BU_2: Only auth and SSO enabled (security and MFA disabled due to dependencies)
   - BU_3: All features disabled

3. Production Environment:
   - BU_1 + US + HIGH compliance: All features enabled
   - BU_1 + US + LOW compliance: Only auth and SSO enabled
   - BU_1 + EU + HIGH compliance: All features disabled
   - BU_2 + US: Only auth and SSO enabled
   - Any other combination: All features disabled

This example demonstrates:
- Multi-level dependencies (MFA depends on both Security and SSO)
- Environment-specific rules
- Complex condition expressions
- Dependency chain evaluation
- Different evaluation results based on environment and attributes 