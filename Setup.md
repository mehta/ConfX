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
    GRANT CREATE ON DATABASE confx_db TO confx_user;
    ```
    Flyway is configured to create the schema `confx_schema` if it doesn't exist. The `confx_user` being the owner of `confx_db` usually suffices.

### 1.3 Application Configuration

1.  **Clone the Repository (if not already done):**
    ```bash
    # git clone https://github.com/mehta/ConfX.git
    # cd ConfX
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

## What's Next?
### Ready to test cURL requests:
[CaseStudy1](https://github.com/mehta/ConfX/blob/master/Usage1.md)

[CaseStudy2](https://github.com/mehta/ConfX/blob/master/Usage2.md)

[CaseStudy3](https://github.com/mehta/ConfX/blob/master/Usage3.md)
