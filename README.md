# ConfX - Headless Feature/Config Management Service

ConfX is a comprehensive feature flag and configuration management service designed to provide real-time updates, multi-environment support, version control, and a powerful rule engine for targeted configuration delivery.

## 1. Server Setup Instructions

To set up and run the ConfX server, follow these steps:

### Prerequisites

*   **Java Development Kit (JDK):** Version 18 or higher.
*   **Gradle:** Version 7.x or higher (the project includes a Gradle wrapper `./gradlew`).
*   **PostgreSQL:** A running instance of PostgreSQL.

### Database Setup

1.  **Create a PostgreSQL database:**
    *   Name: `confx` (or your preferred name, update properties accordingly)
2.  **Create a PostgreSQL user (role):**
    *   Username: `user` (or your preferred name)
    *   Password: `password` (or your preferred strong password)
    *   Grant this user necessary permissions on the `confx` database (e.g., `CREATE`, `CONNECT`, `TEMPORARY`, DML, DDL if Flyway is to create the schema).

    Example SQL (run as a PostgreSQL superuser):
    ```sql
    CREATE USER myuser WITH PASSWORD 'mypassword';
    CREATE DATABASE confx OWNER myuser;
    -- If your DB user for the application won't own the schema but will operate within it:
    -- CREATE SCHEMA confx_schema AUTHORIZATION myuser; 
    -- GRANT ALL PRIVILEGES ON SCHEMA confx_schema TO myuser;
    ```

### Application Configuration

1.  Navigate to `src/main/resources/application.properties`.
2.  Update the following datasource properties if your setup differs from the defaults:
    ```properties
    spring.datasource.url=jdbc:postgresql://localhost:5432/confx
    spring.datasource.username=user
    spring.datasource.password=password
    ```
3.  Flyway properties are also present and will use the datasource properties by default. Ensure the `spring.flyway.schemas=confx_schema` and `spring.jpa.properties.hibernate.default_schema=confx_schema` match your desired schema name. The Flyway scripts will create this schema if it doesn't exist (`CREATE SCHEMA IF NOT EXISTS confx_schema;` in `V1__...sql`).

### Build and Run

1.  **Build the project:**
    Open a terminal in the project's root directory and run:
    ```bash
    ./gradlew build
    ```
2.  **Run the application:**
    After a successful build, run the application using:
    ```bash
    ./gradlew bootRun
    ```
    Alternatively, you can run the generated JAR file:
    ```bash
    java -jar build/libs/confx-0.0.1-SNAPSHOT.jar
    ```

Upon successful startup, Flyway will automatically apply database migrations, creating the necessary tables (`projects`, `environments`, `config_items`, `config_versions`, `rules`, `config_dependencies`) within the `confx_schema`.

The server will typically start on `http://localhost:8080`.

### Verify Server

You can check the server status by accessing actuator endpoints (if enabled and exposed, default is `management.endpoints.web.exposure.include=*`):
*   Health: `http://localhost:8080/actuator/health`

The server provides API endpoints under `/api/v1/...` for managing projects, environments, configurations, and more. It also supports client SDK connections for real-time updates (details of which are beyond this server setup guide).

## 2. Project Overview and Feature Support

ConfX is a powerful, self-hosted solution for managing dynamic configurations and feature flags across multiple environments for various applications. It allows developers and product managers to control feature rollouts, A/B testing, and application settings without requiring new code deployments.

### Core Concepts Explained Incrementally

#### 2.1 Projects

*   **Concept:** A `Project` is the highest-level organizational unit in ConfX. It acts as a container for all related configurations and environments for a specific application, microservice, or a distinct part of a larger system.
*   **Example:** You might have a project named "WebApp-Frontend", another "OrderService-Backend", etc.
*   **Management:** Projects can be created, updated, listed, and deleted via the REST API (`/api/v1/projects`). Each project has a unique name and an optional description.

#### 2.2 Environments

*   **Concept:** Within each `Project`, you can define multiple `Environments`. An environment represents a specific deployment stage or context, such as `development`, `staging`, `qa`, or `production`.
*   **Independence:** Crucially, each environment holds its *own independent set of configuration values and rules*. This means a feature flag can be enabled in `staging` but disabled in `production`, or an API endpoint URL can differ between `development` and `production`.
*   **Management:** Environments are managed under a specific project (`/api/v1/projects/{projectId}/environments`). They have a name (unique within the project), an optional description, and a color tag for UI distinction.

#### 2.3 Configuration Items (`ConfigItem`)

*   **Concept:** A `ConfigItem` is the blueprint or definition of a single piece of configuration or a feature flag. It defines:
    *   `configKey`: A unique string identifier for the config within its project (e.g., `enableNewDashboard`, `paymentGatewayTimeoutMs`).
    *   `dataType`: The type of value this config holds (e.g., `BOOLEAN`, `STRING`, `INTEGER`, `DOUBLE`, `JSON`).
    *   A description and optional notes.
*   **Purpose:** It does *not* store the actual value for different environments; it only defines what the configuration *is*.
*   **Management:** ConfigItems are defined per project (`/api/v1/projects/{projectId}/configs`).

#### 2.4 Configuration Versions (`ConfigVersion`)

*   **Concept:** This is where the actual configuration values for each `ConfigItem` within a specific `Environment` are stored and versioned. Every time you change a configuration's value (or its targeting rules) for a specific environment, a new `ConfigVersion` record is created.
*   **Key Attributes of a `ConfigVersion`:**
    *   Link to `ConfigItem` and `Environment`.
    *   `value`: The default value of the config for this version in this environment (e.g., "true", "1000", "{\"theme\": \"dark\"}").
    *   `versionNumber`: A sequential integer, incremented for each new version of this config in this environment.
    *   `isActive`: A boolean flag. Only *one* version of a config item in a given environment can be `isActive=true` at any time. This active version is what clients will evaluate against by default.
    *   `changeDescription`: A "commit message" explaining why this version was created.
    *   `rules`: A list of targeting rules associated with this specific version (see next section).
*   **Audit Log & Rollback:** The collection of `ConfigVersion` records for a config item in an environment forms its complete history (audit log). The system supports rolling back to any previous version by creating a new active version that copies the settings of the chosen older version.
*   **Management:** Config versions are managed via endpoints like `/api/v1/projects/{projectId}/environments/{environmentId}/configs/{configItemId}/versions`.

#### 2.5 Rules and Rule Engine

*   **Concept:** ConfX features a powerful rule engine that allows for conditional targeting of configuration values. Rules are associated with a specific `ConfigVersion`.
*   **Structure of a Rule:**
    *   `priority`: An integer determining the order of evaluation (lower numbers evaluated first).
    *   `conditionExpression`: A string expression (using Spring Expression Language - SpEL) that evaluates to `true` or `false` based on an `EvaluationContext` provided by the client SDK at evaluation time.
    *   `valueToServe`: The specific value (as a string) to be returned if the `conditionExpression` is true.
*   **EvaluationContext:** This is a set of key-value attributes provided by the client SDK when requesting a config value (e.g., `userId`, `region`, `email`, custom application attributes). Expressions like `attributes['region'] == 'US'` or `attributes['userAge'] > 21` are evaluated against this context.
*   **Evaluation Flow:** When a config is evaluated for a given context:
    1.  The active `ConfigVersion` for the item in the environment is retrieved.
    2.  Its associated rules are evaluated in order of `priority`.
    3.  If a rule's condition matches, its `valueToServe` is returned.
    4.  If no rules match, the default `value` from the `ConfigVersion` is returned.
*   **Supported Operators (via SpEL):** `==`, `!=`, `>`, `<`, `>=`, `<=`, logical `AND` (`&&`), `OR` (`||`), `NOT` (`!`), and list operators like `contains()` (e.g., `attributes['segments'].contains('beta')`).

#### 2.6 Configuration Dependencies

*   **Concept:** ConfX allows defining dependencies between `ConfigItem`s within the same project. For example, `ConfigB` (dependent) might only be truly active or take effect if `ConfigA` (prerequisite) evaluates to a specific value.
*   **Definition:** A dependency specifies:
    *   The dependent `ConfigItem`.
    *   The prerequisite `ConfigItem`.
    *   The `prerequisiteExpectedValue`: The value the prerequisite config must evaluate to for the dependency to be met.
*   **Evaluation Impact:** When evaluating a dependent config:
    1.  All its prerequisites are evaluated first (recursively, using the same evaluation context).
    2.  If any prerequisite does *not* evaluate to its `prerequisiteExpectedValue`, the dependent config is considered "off" (e.g., evaluates to `false` if boolean, or `null` for other types), irrespective of its own rules or default value.
    3.  If all prerequisites are met, the dependent config proceeds with its normal rule and default value evaluation.
*   **Cyclic Dependency Handling:**
    *   **Prevention:** When adding a new dependency, the system performs a check (using DFS) to prevent the creation of circular dependencies (e.g., A depends on B, and B depends on A).
    *   **Evaluation Time Safety:** During evaluation, a separate cycle detection mechanism (using an evaluation stack) is in place as a safeguard. If a cycle is detected during a recursive evaluation call, the config causing the cycle will evaluate to its "off" state to prevent infinite loops.
*   **Management:** Dependencies are defined at the `ConfigItem` level, typically via endpoints like `/api/v1/projects/{projectId}/dependencies/for/{configItemId}`.

#### 2.7 Real-time Updates (Server-Sent Events - SSE)

*   **Concept:** To ensure clients receive configuration changes almost instantaneously without constant polling, ConfX uses Server-Sent Events (SSE).
*   **Connection:** Client SDKs establish an SSE connection to a specific stream endpoint on the server, typically scoped to a project and environment (e.g., `/api/v1/stream/projects/{projectId}/environments/{environmentId}`).
*   **Event Pushing:** When a configuration is updated (i.e., a new `ConfigVersion` is published and becomes active), the server publishes an event.
    *   The `ConfigUpdateEventListener` listens for these application events.
    *   It then uses the `SseService` to push a message to all connected SSE clients subscribed to the relevant project and environment.
*   **Payload:** The SSE message typically includes:
    *   `type`: e.g., `CONFIG_VERSION_UPDATED`, `CONFIG_ITEM_DELETED`, etc.
    *   `payload`: For `CONFIG_VERSION_UPDATED`, this is the complete `ConfigVersionResponseDto` of the new active version (including its key, value, data type, and rules). For deletions, it contains identifiers of the deleted entity.
*   **Client Action:** The client SDK receives this event, parses the payload, and updates its local in-memory cache with the new configuration data. This ensures that subsequent calls to evaluate that config key use the latest information.

#### 2.8 Alive Connections & Heartbeats

*   **Purpose:** To maintain the SSE connection integrity, especially through intermediaries like proxies or load balancers that might terminate idle connections, and to allow the server to detect disconnected clients.
*   **Mechanism:** The `SseService` periodically sends SSE comments (e.g., `: ping`) as heartbeats to all active client connections. This generates network traffic without being interpreted as an actual data event by the client.
*   **Client Handling:** Standard `EventSource` implementations in browsers or custom SSE clients typically ignore comments. The SDK uses these heartbeats (or lack thereof on error) to manage connection state and attempt reconnections.
*   **Server Cleanup:** The `SseService` also uses timeouts and error handlers on `SseEmitter` objects to clean up and remove emitters for clients that have disconnected or errored out.

#### 2.9 Initial Configuration Load (Bulk Fetch)

*   **Concept:** When an SDK instance initializes, it needs to fetch the current state of all active configurations for its designated project and environment.
*   **Endpoint:** The server provides an endpoint (e.g., `/api/v1/projects/{projectId}/environments/{environmentId}/all-active-configs`) that returns a list of all `ConfigVersionResponseDto` objects that are currently active for that environment. This payload includes the default value and all targeting rules for each config.
*   **SDK Action:** The SDK calls this endpoint upon startup, populates its in-memory cache, and then relies on SSE for subsequent delta updates.

This incremental approach, from basic organizational units to complex real-time evaluation and update mechanisms, forms the core of the ConfX service. 


## What's Next?
### Step by step commands to set it up locally:
[Setup Guide](https://github.com/mehta/ConfX/blob/master/Setup.md)

### Ready to test cURL requests:
[CaseStudy1](https://github.com/mehta/ConfX/blob/master/Usage1.md)

[CaseStudy2](https://github.com/mehta/ConfX/blob/master/Usage2.md)

[CaseStudy3](https://github.com/mehta/ConfX/blob/master/Usage3.md)
