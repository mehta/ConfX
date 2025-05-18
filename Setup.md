# ConfX Setup Guide

This guide provides comprehensive instructions for setting up the ConfX backend server and an overview of how to integrate and use the ConfX Java SDK.

## Part 1: Backend Server Setup

These steps will guide you through setting up the ConfX Spring Boot server.

### 1.1 Prerequisites

*   **Operating System:** macOS (instructions tailored for Homebrew, adaptable for Linux/Windows).
*   **Java Development Kit (JDK):** Version 18 or higher.
    *   Install via Homebrew (if not already installed or to manage versions, e.g., with `jenv`):
        ```bash
        brew install openjdk@18 # Or your preferred version like openjdk@21
        # Follow brew instructions to symlink or add to PATH
        # Example: sudo ln -sfn /usr/local/opt/openjdk@18/libexec/openjdk.jdk /Library/Java/JavaVirtualMachines/openjdk-18.jdk
        # java -version
        ```
*   **Gradle:** Version 7.x or higher. The project includes a Gradle wrapper (`./gradlew`), so a separate Gradle installation is not strictly necessary but can be useful.
    *   Install via Homebrew (optional):
        ```bash
        brew install gradle
        # gradle -v
        ```
*   **PostgreSQL Database Server:**
    *   Install via Homebrew:
        ```bash
        brew install postgresql
        ```
    *   Start PostgreSQL service (if not already running):
        ```bash
        brew services start postgresql
        ```
    *   Ensure you can connect to PostgreSQL, typically using `psql`:
        ```bash
        psql postgres # Connect to the default postgres maintenance database
        ```

### 1.2 Database Configuration

1.  **Create a Database User (Role) for ConfX:**
    Connect to PostgreSQL (e.g., `psql postgres`) and run:
    ```sql
    CREATE USER confx_user WITH PASSWORD 'confx_password'; 
    -- Choose a strong password for production.
    ```
2.  **Create the Database for ConfX:**
    ```sql
    CREATE DATABASE confx_db OWNER confx_user;
    ```
3.  **Grant Privileges (Optional, if Flyway will create schema):**
    If the `confx_user` should be able to create the schema (`confx_schema`) via Flyway (as per current setup):
    ```sql
    -- Connect to the confx_db if not already: \c confx_db
    -- The user needs CREATE ON DATABASE implicitly or explicitly to create schemas if it owns the DB
    -- If not owning the database, grant schema creation privilege directly:
    -- GRANT CREATE ON DATABASE confx_db TO confx_user;
    ```
    Flyway is configured to create the schema `confx_schema` if it doesn't exist. The `confx_user` being the owner of `confx_db` usually suffices.

### 1.3 Application Configuration

1.  **Clone the Repository (if not already done):**
    ```bash
    # git clone <repository_url>
    # cd <repository_directory>
    ```
2.  **Configure Database Connection:**
    Open the main application configuration file: `src/main/resources/application.properties`.
    Modify the following properties to match your PostgreSQL setup (if different from defaults):
    ```properties
    spring.datasource.url=jdbc:postgresql://localhost:5432/confx_db
    spring.datasource.username=confx_user
    spring.datasource.password=confx_password
    
    # Ensure schema names are consistent for Flyway and Hibernate
    spring.flyway.schemas=confx_schema
    spring.jpa.properties.hibernate.default_schema=confx_schema
    ```
    The Flyway scripts are located in `src/main/resources/db/migration` and will create the `confx_schema` if it doesn't exist.

### 1.4 Build and Run the Server

1.  **Build the Project:**
    Navigate to the root directory of the ConfX project (where `build.gradle` for the server is located) and run:
    ```bash
    ./gradlew clean build
    ```
2.  **Run the Application:**
    You can run the server using Gradle:
    ```bash
    ./gradlew bootRun
    ```
    Alternatively, run the generated JAR file from the `build/libs` directory:
    ```bash
    java -jar build/libs/confx-0.0.1-SNAPSHOT.jar 
    ```
    *(The exact JAR filename might vary slightly based on version.)*

    Upon successful startup, you should see logs indicating that Flyway has applied the database migrations and the Spring Boot application has started. The server typically runs on `http://localhost:8080`.

### 1.5 Verify Server Operation

*   **Health Check:** Access `http://localhost:8080/actuator/health` in your browser or via cURL. You should see a status of `UP`.
*   **API Endpoints:** The server exposes RESTful APIs under `/api/v1/...`. You can try listing projects (which will be empty initially): `curl http://localhost:8080/api/v1/projects`

The server is now set up. The next sections will detail setting up the SDK and then populating sample data with cURL commands.

## Part 2: Java SDK Setup (Overview)

The ConfX project includes a Java SDK located in the `ConfXSDK` directory. This SDK allows Java applications (including other Spring Boot services) to integrate with the ConfX server to fetch configurations, evaluate them client-side, and receive real-time updates via Server-Sent Events (SSE).

### 2.1 Building the SDK

1.  Navigate to the SDK directory:
    ```bash
    cd ConfXSDK
    ```
2.  Build the SDK using Gradle:
    ```bash
    ./gradlew clean build
    ```
    This will compile the SDK and run its tests, producing a JAR file (e.g., `ConfXSDK/build/libs/confx-sdk-0.0.1-SNAPSHOT.jar`).

### 2.2 Using the SDK in a Client Spring Boot Application

1.  **Add as a Dependency:**
    In your client Spring Boot project, add the SDK JAR as a dependency. For local development, after building the SDK, you can install it to your local Maven repository by running `./gradlew publishToMavenLocal` in the `ConfXSDK` directory. Then, reference it in your client project's `build.gradle`:

    ```gradle
    dependencies {
        implementation 'com.abhinavmehta.confx:confx-sdk:0.0.1-SNAPSHOT'
        // ... other dependencies
    }
    ```
    If publishing to a shared repository (like Artifactory or Maven Central), use the appropriate coordinates.

2.  **Configure and Initialize `ConfXClient` as a Spring Bean:**
    Create a Spring `@Configuration` class in your client application to define the `ConfXClient` bean. This ensures the SDK is initialized when your application starts and its lifecycle is managed by Spring.

    ```java
    package com.example.myapp.config; // Your client application's package

    import com.abhinavmehta.confx.sdk.ConfXClient;
    import com.abhinavmehta.confx.sdk.ConfXSDKConfig;
    import org.springframework.beans.factory.annotation.Value;
    import org.springframework.context.annotation.Bean;
    import org.springframework.context.annotation.Configuration;
    import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

    import java.util.concurrent.ScheduledExecutorService;

    @Configuration
    public class ConfXClientConfiguration {

        @Value("${confx.server.url:http://localhost:8080}")
        private String confxServerUrl;

        @Value("${confx.project.id}") // Ensure these are in your application.properties or env
        private Long confxProjectId;

        @Value("${confx.environment.id}")
        private Long confxEnvironmentId;

        // Optional: Define a shared ScheduledExecutorService if you want more control
        // Otherwise, the SDK creates its own internal one.
        @Bean(name = "confXSdkExecutor")
        public ScheduledExecutorService confXSdkExecutor() {
            ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
            scheduler.setPoolSize(2); // Configure as needed
            scheduler.setThreadNamePrefix("confx-sdk-tasks-");
            scheduler.setDaemon(true);
            scheduler.initialize();
            return scheduler.getScheduledExecutor();
        }

        @Bean(destroyMethod = "close") // Spring will call client.close() on shutdown
        public ConfXClient confXClient(/* Optional: @Qualifier("confXSdkExecutor") ScheduledExecutorService executor */) {
            ConfXSDKConfig sdkConfig = ConfXSDKConfig.builder()
                    .serverUrl(confxServerUrl)
                    .projectId(confxProjectId)
                    .environmentId(confxEnvironmentId)
                    // .executorService(executor) // Optionally pass the shared executor
                    // .sseReconnectTimeMs(5000) 
                    // .maxRetries(5)
                    .build();
            
            // The ConfXClient constructor initiates asynchronous loading and SSE connection.
            return new ConfXClient(sdkConfig); 
        }
    }
    ```
    Ensure you have the necessary properties (e.g., `confx.project.id`, `confx.environment.id`, and optionally `confx.server.url`) in your client application's `application.properties` or environment variables.

3.  **Inject and Use `ConfXClient` in Your Services/Components:**
    Now you can inject the `ConfXClient` bean into any of your Spring components (services, controllers, repositories, etc.).

    **Example: `MyFeatureService.java`**
    ```java
    package com.example.myapp.service;

    import com.abhinavmehta.confx.sdk.ConfXClient;
    import com.abhinavmehta.confx.sdk.dto.EvaluationContext;
    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.stereotype.Service;
    import java.util.Map;

    @Service
    public class MyFeatureService {

        private final ConfXClient confXClient;

        @Autowired
        public MyFeatureService(ConfXClient confXClient) {
            this.confXClient = confXClient;
        }

        public boolean isSuperCheckoutEnabled(String userId) {
            if (confXClient.isInitialized()) {
                EvaluationContext context = EvaluationContext.builder()
                    .attributes(Map.of("userId", userId, "userTier", "gold"))
                    .build();
                return confXClient.getBooleanValue("tms.checkout.superCheckout.enabled", context, false);
            } else {
                // SDK not ready, return a safe default
                // Log a warning or error here if this state is unexpected in production
                return false; 
            }
        }
    }
    ```

    **Example: `AnotherBusinessLogic.java`**
    ```java
    package com.example.myapp.logic;

    import com.abhinavmehta.confx.sdk.ConfXClient;
    import com.abhinavmehta.confx.sdk.dto.EvaluationContext;
    import org.springframework.stereotype.Component;

    @Component
    public class AnotherBusinessLogic {

        private final ConfXClient confXClient;

        public AnotherBusinessLogic(ConfXClient confXClient) {
            this.confXClient = confXClient;
        }

        public String getShipmentNotificationMessage(String customerType) {
            if (!confXClient.isInitialized()) {
                return "Your shipment is on its way!"; // Safe default
            }
            EvaluationContext context = EvaluationContext.builder()
                .attributes(Map.of("customerType", customerType))
                .build();
            return confXClient.getStringValue("tms.notification.shipment.template", context, "Default shipment update.");
        }
    }
    ```

Detailed SDK usage, including evaluation context and specific value getters, is provided within the SDK's own documentation or examples (if available). The SDK handles in-memory caching, SSE updates, and client-side rule/dependency evaluation automatically once initialized.

## Part 3: Sample Data Setup & API Testing

This section provides steps to populate the ConfX server with sample data. It includes configurations for different business units, carriers, and feature flags. Corresponding cURL commands are provided to test all major API functionalities.

**Assumptions:**
*   The ConfX server is running on `http://localhost:8080`.
*   `jq` (command-line JSON processor) is installed for easier parsing of responses (optional, but helpful: `brew install jq`).

### 3.1 Base URL Variable

For convenience in the cURL commands:
```bash
BASE_URL="http://localhost:8080/api/v1"
```

### 3.2 Project Setup

Let's create a project for our TMS.

```bash
# Create TMS Project
curl -X POST "${BASE_URL}/projects" \
-H "Content-Type: application/json" \
-d '{
  "name": "Global TMS Platform",
  "description": "Centralized Transport Management System Configurations"
}' | jq

# Assume Project ID 1 is returned. Export for later use:
PROJECT_ID=1
```

### 3.3 Environment Setup

Create Development and Production environments for the TMS project.

```bash
# Create Development Environment
curl -X POST "${BASE_URL}/projects/${PROJECT_ID}/environments" \
-H "Content-Type: application/json" \
-d '{
  "name": "Development",
  "description": "TMS Development Environment",
  "colorTag": "#3498DB"
}' | jq
# Assume DEV_ENV_ID = 1
DEV_ENV_ID=1

# Create Production Environment
curl -X POST "${BASE_URL}/projects/${PROJECT_ID}/environments" \
-H "Content-Type: application/json" \
-d '{
  "name": "Production",
  "description": "TMS Production Environment",
  "colorTag": "#E74C3C"
}' | jq
# Assume PROD_ENV_ID = 2
PROD_ENV_ID=2
```

### 3.4 Config Item Definitions

Define various config items for different aspects of the TMS.

```bash
# Config Item 1: Enable New Routing Algorithm (Feature Flag)
curl -X POST "${BASE_URL}/projects/${PROJECT_ID}/configs" \
-H "Content-Type: application/json" \
-d '{
  "configKey": "tms.routing.newAlgorithm.enabled",
  "dataType": "BOOLEAN",
  "description": "Enable the new experimental routing algorithm.",
  "notes": "Requires extensive testing before production."
}' | jq
# Assume ROUTING_ALGO_CONFIG_ID = 1
ROUTING_ALGO_CONFIG_ID=1

# Config Item 2: Max Shipment Weight (Business Rule - Per Carrier Type)
curl -X POST "${BASE_URL}/projects/${PROJECT_ID}/configs" \
-H "Content-Type: application/json" \
-d '{
  "configKey": "tms.shipment.maxWeightKg",
  "dataType": "INTEGER",
  "description": "Maximum allowed shipment weight in kilograms.",
  "notes": "Default value if no specific carrier rule applies."
}' | jq
# Assume MAX_WEIGHT_CONFIG_ID = 2
MAX_WEIGHT_CONFIG_ID=2

# Config Item 3: Preferred Carrier List (Business Logic - Region Specific)
curl -X POST "${BASE_URL}/projects/${PROJECT_ID}/configs" \
-H "Content-Type: application/json" \
-d '{
  "configKey": "tms.carrier.preferredList",
  "dataType": "JSON",
  "description": "Ordered list of preferred carrier codes for a region.",
  "notes": "Payload: [\"CARRIER_A\", \"CARRIER_B\"]"
}' | jq
# Assume PREF_CARRIER_CONFIG_ID = 3
PREF_CARRIER_CONFIG_ID=3

# Config Item 4: Real-time Tracking Feature (Feature Flag)
curl -X POST "${BASE_URL}/projects/${PROJECT_ID}/configs" \
-H "Content-Type: application/json" \
-d '{
  "configKey": "tms.tracking.realtime.enabled",
  "dataType": "BOOLEAN",
  "description": "Enable real-time GPS tracking for shipments."
}' | jq
# Assume TRACKING_CONFIG_ID = 4
TRACKING_CONFIG_ID=4

# Config Item 5: API Endpoint for External Rate Shopper
curl -X POST "${BASE_URL}/projects/${PROJECT_ID}/configs" \
-H "Content-Type: application/json" \
-d '{
  "configKey": "tms.integration.rateShopper.endpointUrl",
  "dataType": "STRING",
  "description": "URL for the external rate shopping service."
}' | jq
# Assume RATE_SHOPPER_URL_CONFIG_ID = 5
RATE_SHOPPER_URL_CONFIG_ID=5

# Config Item 6: Fuel Surcharge Percentage (Business Config)
curl -X POST "${BASE_URL}/projects/${PROJECT_ID}/configs" \
-H "Content-Type: application/json" \
-d '{
  "configKey": "tms.billing.fuelSurchargePercent",
  "dataType": "DOUBLE",
  "description": "Current fuel surcharge percentage (e.g., 0.15 for 15%)."
}' | jq
# Assume FUEL_SURCHARGE_CONFIG_ID = 6
FUEL_SURCHARGE_CONFIG_ID=6

# Config Item 7: Business Unit A - Specific Discount (Targeted Config)
curl -X POST "${BASE_URL}/projects/${PROJECT_ID}/configs" \
-H "Content-Type: application/json" \
-d '{
  "configKey": "tms.billing.bu_a.discount",
  "dataType": "DOUBLE",
  "description": "Special discount for Business Unit A customers."
}' | jq
# Assume BU_A_DISCOUNT_CONFIG_ID = 7
BU_A_DISCOUNT_CONFIG_ID=7

# Config Item 8: Enable ETA Prediction Service (Feature Flag)
curl -X POST "${BASE_URL}/projects/${PROJECT_ID}/configs" \
-H "Content-Type: application/json" \
-d '{
  "configKey": "tms.etaPrediction.enabled",
  "dataType": "BOOLEAN",
  "description": "Enable AI-based ETA prediction service."
}' | jq
# Assume ETA_PREDICT_CONFIG_ID = 8
ETA_PREDICT_CONFIG_ID=8

# Config Item 9: International Shipping Mode (Depends on ETA Prediction)
curl -X POST "${BASE_URL}/projects/${PROJECT_ID}/configs" \
-H "Content-Type: application/json" \
-d '{
  "configKey": "tms.shipping.international.mode",
  "dataType": "STRING",
  "description": "Mode for international shipping (e.g., AIR_EXPRESS, OCEAN_STD). Dependent on ETA service.",
  "notes": "If ETA service is off, might default to a standard mode."
}' | jq
# Assume INTL_SHIP_MODE_CONFIG_ID = 9
INTL_SHIP_MODE_CONFIG_ID=9

# Config Item 10: Customer Portal V2 Rollout (Feature Flag)
curl -X POST "${BASE_URL}/projects/${PROJECT_ID}/configs" \
-H "Content-Type: application/json" \
-d '{
  "configKey": "tms.customerPortal.v2.enabled",
  "dataType": "BOOLEAN",
  "description": "Enable the new V2 customer portal design."
}' | jq
# Assume PORTAL_V2_CONFIG_ID = 10
PORTAL_V2_CONFIG_ID=10
```

### 3.5 Publish Config Versions (with Rules)

#### 3.5.1 `tms.routing.newAlgorithm.enabled` (ID: `${ROUTING_ALGO_CONFIG_ID}`)

*   **Development (ID: `${DEV_ENV_ID}`):** Enable for internal users and a specific customer segment.
    ```bash
    curl -X POST "${BASE_URL}/projects/${PROJECT_ID}/environments/${DEV_ENV_ID}/configs/${ROUTING_ALGO_CONFIG_ID}/versions" \
    -H "Content-Type: application/json" \
    -d '{
      "value": "false", 
      "changeDescription": "Initial setup for new routing algorithm - Dev",
      "rules": [
        {
          "priority": 1,
          "conditionExpression": "attributes[\'userGroup\'] == \'internal_testers\'",
          "valueToServe": "true",
          "description": "Enable for internal testing team."
        },
        {
          "priority": 2,
          "conditionExpression": "attributes[\'customerSegment\'] == \'early_adopter_program\'",
          "valueToServe": "true",
          "description": "Enable for early adopter customers."
        }
      ]
    }' | jq
    ```
*   **Production (ID: `${PROD_ENV_ID}`):** Initially disabled.
    ```bash
    curl -X POST "${BASE_URL}/projects/${PROJECT_ID}/environments/${PROD_ENV_ID}/configs/${ROUTING_ALGO_CONFIG_ID}/versions" \
    -H "Content-Type: application/json" \
    -d '{
      "value": "false", 
      "changeDescription": "New routing algorithm - Prod (default off)",
      "rules": []
    }' | jq
    ```

#### 3.5.2 `tms.shipment.maxWeightKg` (ID: `${MAX_WEIGHT_CONFIG_ID}`)

*   **Development (ID: `${DEV_ENV_ID}`):** Higher default, specific rule for 'AIR' freight.
    ```bash
    curl -X POST "${BASE_URL}/projects/${PROJECT_ID}/environments/${DEV_ENV_ID}/configs/${MAX_WEIGHT_CONFIG_ID}/versions" \
    -H "Content-Type: application/json" \
    -d '{
      "value": "5000", 
      "changeDescription": "Max weight - Dev",
      "rules": [
        {
          "priority": 1,
          "conditionExpression": "attributes[\'freightType\'] == \'AIR\'",
          "valueToServe": "1500",
          "description": "Air freight max weight 1500kg."
        }
      ]
    }' | jq
    ```
*   **Production (ID: `${PROD_ENV_ID}`):** Stricter default.
    ```bash
    curl -X POST "${BASE_URL}/projects/${PROJECT_ID}/environments/${PROD_ENV_ID}/configs/${MAX_WEIGHT_CONFIG_ID}/versions" \
    -H "Content-Type: application/json" \
    -d '{
      "value": "2000", 
      "changeDescription": "Max weight - Prod",
      "rules": [
        {
          "priority": 1,
          "conditionExpression": "attributes[\'freightType\'] == \'AIR\'",
          "valueToServe": "1000",
          "description": "Air freight max weight 1000kg in Prod."
        }
      ]
    }' | jq
    ```

#### 3.5.3 `tms.carrier.preferredList` (ID: `${PREF_CARRIER_CONFIG_ID}`)

*   **Production (ID: `${PROD_ENV_ID}`):** Different lists for NA and EU regions.
    ```bash
    curl -X POST "${BASE_URL}/projects/${PROJECT_ID}/environments/${PROD_ENV_ID}/configs/${PREF_CARRIER_CONFIG_ID}/versions" \
    -H "Content-Type: application/json" \
    -d '{
      "value": "[\"GENERIC_CARRIER\"]", 
      "changeDescription": "Preferred carriers - Prod",
      "rules": [
        {
          "priority": 1,
          "conditionExpression": "attributes[\'region\'] == \'NA\'",
          "valueToServe": "[\"FEDEX_NA\", \"UPS_NA\"]",
          "description": "North America preferred carriers."
        },
        {
          "priority": 2,
          "conditionExpression": "attributes[\'region\'] == \'EU\'",
          "valueToServe": "[\"DHL_EU\", \"DPD_EU\"]",
          "description": "Europe preferred carriers."
        }
      ]
    }' | jq
    ```

#### 3.5.4 `tms.tracking.realtime.enabled` (ID: `${TRACKING_CONFIG_ID}`)

*   **Production (ID: `${PROD_ENV_ID}`):** Enabled for premium customers.
    ```bash
    curl -X POST "${BASE_URL}/projects/${PROJECT_ID}/environments/${PROD_ENV_ID}/configs/${TRACKING_CONFIG_ID}/versions" \
    -H "Content-Type: application/json" \
    -d '{
      "value": "false", 
      "changeDescription": "Real-time tracking - Prod",
      "rules": [
        {
          "priority": 1,
          "conditionExpression": "attributes[\'customerTier\'] == \'PREMIUM\'",
          "valueToServe": "true",
          "description": "Enable for premium tier customers."
        }
      ]
    }' | jq
    ```

#### 3.5.5 `tms.etaPrediction.enabled` (ID: `${ETA_PREDICT_CONFIG_ID}`)

*   **Production (ID: `${PROD_ENV_ID}`):** Default ON.
    ```bash
    curl -X POST "${BASE_URL}/projects/${PROJECT_ID}/environments/${PROD_ENV_ID}/configs/${ETA_PREDICT_CONFIG_ID}/versions" \
    -H "Content-Type: application/json" \
    -d '{
      "value": "true", 
      "changeDescription": "ETA Prediction - Prod (default ON)",
      "rules": []
    }' | jq
    ```

### 3.6 Config Dependencies Setup

`INTL_SHIP_MODE_CONFIG_ID` (ID: 9) depends on `ETA_PREDICT_CONFIG_ID` (ID: 8) being `true`.

```bash
curl -X POST "${BASE_URL}/projects/${PROJECT_ID}/dependencies/for/${INTL_SHIP_MODE_CONFIG_ID}" \
-H "Content-Type: application/json" \
-d '{
  "prerequisiteConfigItemId": '"${ETA_PREDICT_CONFIG_ID}"',
  "prerequisiteExpectedValue": "true",
  "description": "International shipping mode selection relies on ETA prediction service being active."
}' | jq
# Assume DEP_ID_INTL_ETA = 1 (for this dependency instance)
DEP_ID_INTL_ETA=1
```

Now, let's publish a version for `INTL_SHIP_MODE_CONFIG_ID` in Production.
```bash
curl -X POST "${BASE_URL}/projects/${PROJECT_ID}/environments/${PROD_ENV_ID}/configs/${INTL_SHIP_MODE_CONFIG_ID}/versions" \
-H "Content-Type: application/json" \
-d '{
  "value": "STANDARD_GROUND", 
  "changeDescription": "International Shipping Mode - Prod",
  "rules": [
    {
      "priority": 1,
      "conditionExpression": "attributes[\'destinationCountryGroup\'] == \'EU_FAST_LANE\' && attributes[\'parcelValueUSD\'] > 1000",
      "valueToServe": "AIR_EXPRESS_PREMIUM",
      "description": "High value EU fast lane shipments get premium air express."
    }
  ]
}' | jq
```

### 3.7 API Testing with cURL - Incremental Examples

#### 3.7.1 List Projects
```bash
curl "${BASE_URL}/projects" | jq
```

#### 3.7.2 Get Specific Project
```bash
curl "${BASE_URL}/projects/${PROJECT_ID}" | jq
```

#### 3.7.3 List Environments for TMS Project
```bash
curl "${BASE_URL}/projects/${PROJECT_ID}/environments" | jq
```

#### 3.7.4 Get Specific Environment (Production)
```bash
curl "${BASE_URL}/projects/${PROJECT_ID}/environments/${PROD_ENV_ID}" | jq
```

#### 3.7.5 List Config Items for TMS Project
```bash
curl "${BASE_URL}/projects/${PROJECT_ID}/configs" | jq
```

#### 3.7.6 Get Specific Config Item (Routing Algorithm)
```bash
curl "${BASE_URL}/projects/${PROJECT_ID}/configs/${ROUTING_ALGO_CONFIG_ID}" | jq
# Or by key
curl "${BASE_URL}/projects/${PROJECT_ID}/configs/key/tms.routing.newAlgorithm.enabled" | jq
```

#### 3.7.7 Get Active Version of a Config in an Environment
```bash
# Routing algorithm in Production
curl "${BASE_URL}/projects/${PROJECT_ID}/environments/${PROD_ENV_ID}/configs/${ROUTING_ALGO_CONFIG_ID}/versions/active" | jq
```

#### 3.7.8 Get Version History
```bash
# Routing algorithm in Development (should have 1 version initially)
curl "${BASE_URL}/projects/${PROJECT_ID}/environments/${DEV_ENV_ID}/configs/${ROUTING_ALGO_CONFIG_ID}/versions" | jq
```

#### 3.7.9 Evaluate Configs (using `/api/v1/evaluate/...`)

*   **Evaluate `tms.routing.newAlgorithm.enabled` in Dev for an internal tester:**
    ```bash
    curl -X POST "${BASE_URL}/evaluate/projects/${PROJECT_ID}/environments/${DEV_ENV_ID}/configs/tms.routing.newAlgorithm.enabled" \
    -H "Content-Type: application/json" \
    -d '{
      "attributes": {
        "userGroup": "internal_testers"
      }
    }' | jq
    # Expected: value: true, evaluationSource: "RULE_MATCH"
    ```

*   **Evaluate `tms.routing.newAlgorithm.enabled` in Dev for a regular user:**
    ```bash
    curl -X POST "${BASE_URL}/evaluate/projects/${PROJECT_ID}/environments/${DEV_ENV_ID}/configs/tms.routing.newAlgorithm.enabled" \
    -H "Content-Type: application/json" \
    -d '{
      "attributes": {
        "userGroup": "customer"
      }
    }' | jq
    # Expected: value: false, evaluationSource: "DEFAULT_VALUE"
    ```

*   **Evaluate `tms.shipment.maxWeightKg` in Prod for AIR freight:**
    ```bash
    curl -X POST "${BASE_URL}/evaluate/projects/${PROJECT_ID}/environments/${PROD_ENV_ID}/configs/tms.shipment.maxWeightKg" \
    -H "Content-Type: application/json" \
    -d '{
      "attributes": {
        "freightType": "AIR"
      }
    }' | jq
    # Expected: value: 1000, evaluationSource: "RULE_MATCH"
    ```

*   **Evaluate `tms.carrier.preferredList` in Prod for EU region:**
    ```bash
    curl -X POST "${BASE_URL}/evaluate/projects/${PROJECT_ID}/environments/${PROD_ENV_ID}/configs/tms.carrier.preferredList" \
    -H "Content-Type: application/json" \
    -d '{
      "attributes": {
        "region": "EU"
      }
    }' | jq
    # Expected: value: ["DHL_EU", "DPD_EU"], evaluationSource: "RULE_MATCH"
    ```

*   **Evaluate `tms.shipping.international.mode` in Prod (ETA service is ON):**
    ```bash
    # (Assuming ETA_PREDICT_CONFIG_ID (8) is true by default in Prod)
    curl -X POST "${BASE_URL}/evaluate/projects/${PROJECT_ID}/environments/${PROD_ENV_ID}/configs/tms.shipping.international.mode" \
    -H "Content-Type: application/json" \
    -d '{
      "attributes": { "destinationCountryGroup": "OTHER" } 
    }' | jq
    # Expected: value: "STANDARD_GROUND", evaluationSource: "DEFAULT_VALUE" (as prerequisite is met)
    ```

*   **Temporarily turn OFF ETA Prediction (`ETA_PREDICT_CONFIG_ID`) in Prod:**
    ```bash
    curl -X POST "${BASE_URL}/projects/${PROJECT_ID}/environments/${PROD_ENV_ID}/configs/${ETA_PREDICT_CONFIG_ID}/versions" \
    -H "Content-Type: application/json" \
    -d '{
      "value": "false", 
      "changeDescription": "Temporarily disable ETA for testing dependency",
      "rules": []
    }' | jq
    ```

*   **Re-evaluate `tms.shipping.international.mode` in Prod (ETA service is now OFF):**
    ```bash
    curl -X POST "${BASE_URL}/evaluate/projects/${PROJECT_ID}/environments/${PROD_ENV_ID}/configs/tms.shipping.international.mode" \
    -H "Content-Type: application/json" \
    -d '{
      "attributes": { "destinationCountryGroup": "OTHER" } 
    }' | jq
    # Expected: value: null (or appropriate "off" state for STRING), evaluationSource: "PREREQUISITE_NOT_MET"
    ```

#### 3.7.10 List Dependencies
```bash
# List prerequisites for INTL_SHIP_MODE_CONFIG_ID
curl "${BASE_URL}/projects/${PROJECT_ID}/dependencies/for/${INTL_SHIP_MODE_CONFIG_ID}" | jq

# List all dependencies for the project
curl "${BASE_URL}/projects/${PROJECT_ID}/dependencies/all" | jq
```

#### 3.7.11 SSE Connection Test (Conceptual)

1.  **Open an SSE connection (e.g., in a new terminal window or using an SSE client tool):**
    ```bash
    curl -N -H "Accept:text/event-stream" "${BASE_URL}/stream/projects/${PROJECT_ID}/environments/${PROD_ENV_ID}"
    ```
    You should see a `connection_established` event and periodic `: ping` heartbeats.

2.  **In another terminal, update a config in Production (e.g., re-enable ETA Prediction):**
    ```bash
    curl -X POST "${BASE_URL}/projects/${PROJECT_ID}/environments/${PROD_ENV_ID}/configs/${ETA_PREDICT_CONFIG_ID}/versions" \
    -H "Content-Type: application/json" \
    -d '{
      "value": "true", 
      "changeDescription": "Re-enable ETA for testing SSE",
      "rules": []
    }' | jq
    ```

3.  **Observe the SSE client terminal:** You should receive an SSE event of type `CONFIG_VERSION_UPDATED` with the payload containing the details of the updated `tms.etaPrediction.enabled` config.

    Example (structure of what you might see in the SSE stream):
    ```
    event: CONFIG_VERSION_UPDATED
    id: <timestamp>
    data: {"type":"CONFIG_VERSION_UPDATED","payload":{"id":...,"configItemId":8,"configItemKey":"tms.etaPrediction.enabled",..."value":"true",...}}
    
    ```

#### 3.7.12 Rollback a Config Version

*   First, get version history to find an old version ID. E.g., for `ETA_PREDICT_CONFIG_ID` in Prod:
    ```bash
    curl "${BASE_URL}/projects/${PROJECT_ID}/environments/${PROD_ENV_ID}/configs/${ETA_PREDICT_CONFIG_ID}/versions" | jq
    # Let's say an earlier version (e.g., value "false") has ID `XYZ`
    # VERSION_ID_TO_ROLLBACK_TO=XYZ (replace XYZ with actual ID from output)
    ```
*   Perform rollback:
    ```bash
    # curl -X POST "${BASE_URL}/projects/${PROJECT_ID}/environments/${PROD_ENV_ID}/configs/${ETA_PREDICT_CONFIG_ID}/versions/${VERSION_ID_TO_ROLLBACK_TO}/rollback" | jq
    ```
    This will create a *new* active version with the content of the old one.

### 3.8 Cleanup (Optional)

```bash
# Delete a specific dependency
# curl -X DELETE "${BASE_URL}/projects/${PROJECT_ID}/dependencies/${DEP_ID_INTL_ETA}"

# Delete a Config Item (will also delete its versions and rules due to cascade)
# curl -X DELETE "${BASE_URL}/projects/${PROJECT_ID}/configs/${PORTAL_V2_CONFIG_ID}"

# Delete an Environment (will also delete its config versions)
# curl -X DELETE "${BASE_URL}/projects/${PROJECT_ID}/environments/${DEV_ENV_ID}"

# Delete the Project (will delete everything under it)
# curl -X DELETE "${BASE_URL}/projects/${PROJECT_ID}"
```

This detailed setup and testing guide should help users get started with ConfX and verify its core functionalities using a practical TMS domain example. 
