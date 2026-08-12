# Leave Portal Application — Full Report

## 1. Project Overview

The **Leave Portal Application** is a Spring Boot (Java 17) REST API that lets authenticated users upload leave request files (`.txt`) through a browser. The application:

1. Authenticates users via **Google OAuth 2.0 / OpenID Connect**.
2. Reads the uploaded `.txt` file content and **categorizes** the leave type based on keyword matching (sick, annual, emergency, maternity, unpaid, or other).
3. Uploads the raw file to **OCI Object Storage**.
4. Returns a structured response containing the user, file name, detected category, matched keywords, timestamp, and OCI object references.
5. Provides a JPA entity + repository to persist upload metadata in an **Oracle Database** (Autonomous Database via JDBC thin driver).

It is the solution for the **Agile Oracles OCI Evaluation Task** (Codeline).

---

## 2. Tech Stack

| Layer            | Technology |
|------------------|------------|
| Language         | Java 17 |
| Framework        | Spring Boot 4.1.0 |
| Security         | Spring Security + OAuth2 Client (Google login) |
| Web              | Spring MVC (`spring-boot-starter-webmvc`) |
| Persistence      | Spring Data JPA + Oracle Database (`ojdbc11`) |
| Cloud Storage    | OCI Java SDK — Object Storage (`oci-java-sdk-objectstorage` 3.80.3) |
| HTTP Client (OCI)| OCI Java SDK Jersey client (`oci-java-sdk-common-httpclient-jersey` 2.47) |
| Build Tool       | Maven (with `spring-boot-maven-plugin`) |
| Boilerplate      | Lombok |
| Frontend (test)  | Static HTML/JS test page (`upload-test.html`) |

---

## 3. Project Structure

```
leave-portal-app/
├── pom.xml
├── mvnw / mvnw.cmd
├── .env                                   # Environment variables (Google + DB credentials)
├── HELP.md
├── REPORT.md
└── src/
    ├── main/
    │   ├── java/com/agileoracles/leave_portal_app/
    │   │   ├── LeavePortalAppApplication.java     # Spring Boot entry point
    │   │   ├── config/
    │   │   │   └── SecurityConfig.java            # Security filter chain + OAuth2 login
    │   │   ├── controller/
    │   │   │   └── LeaveController.java           # REST endpoints (/api/leave/**)
    │   │   ├── service/
    │   │   │   ├── CategorizationService.java     # Keyword-based leave categorization
    │   │   │   └── OciStorageService.java         # OCI Object Storage upload
    │   │   ├── model/
    │   │   │   ├── LeaveCategory.java             # Enum of leave categories
    │   │   │   ├── LeaveResponse.java             # API response record
    │   │   │   └── LeaveUploadRecord.java         # JPA entity (LEAVE_UPLOADS table)
    │   │   ├── repository/
    │   │   │   └── LeaveUploadRepository.java     # JPA repository
    │   │   └── exception/
    │   │       └── GlobalExceptionHandler.java    # @RestControllerAdvice error mapping
    │   ├── resources/
    │   │   ├── application.properties              # App, OCI, OAuth, DB configuration
    │   │   └── static/
    │   │       └── upload-test.html               # Browser test page for uploads
    └── test/
        └── java/.../LeavePortalAppApplicationTests.java   # Context-load smoke test
```

---

## 4. Application Flow

```
Browser ──OAuth2 login──▶ Google
    │ (callback: /login/oauth2/code/google)
    ▼
Authenticated user loads /upload-test.html (static page)
    │ POST /api/leave/upload (multipart .txt file)
    ▼
LeaveController.upload()
    ├── Validates file extension (.txt)                       ──▶ 400 on failure
    ├── Reads file bytes as UTF-8 string
    ├── CategorizationService.categorize()                    ──▶ LeaveCategory
    ├── CategorizationService.findMatchedKeywords()           ──▶ matched keywords
    ├── OciStorageService.uploadFile()                        ──▶ OCI Object Storage
    └── Builds LeaveResponse (user, file, category, keywords, timestamp, OCI refs)
    ▼
200 OK + JSON LeaveResponse (or error handled by GlobalExceptionHandler)
```

---

## 5. API Endpoints

### `GET /api/leave/status`
Returns the email address of the currently authenticated user.

**Security:** authenticated (redirects to Google login otherwise).

**Sample response (text/plain):**
```
Logged in as: user@example.com
```

---

### `POST /api/leave/upload`
Uploads a `.txt` leave request file.

**Parameters:**
| Name   | Type          | Required | Description |
|--------|---------------|----------|-------------|
| `file` | `MultipartFile` | Yes    | The `.txt` file to upload |

**Security:** authenticated.

**Validations:**
- File must end with `.txt` (case-insensitive) → otherwise `400 Bad Request`.

**Sample request:**
```
curl -X POST http://localhost:8080/api/leave/upload \
     -H "Cookie: <session>" \
     -F "file=@leave-request.txt"
```

**Sample response (200 OK):**
```json
{
  "authenticatedUser": "user@example.com",
  "fileName": "leave-request.txt",
  "category": "SICK_LEAVE",
  "matchedKeywords": "sick, doctor",
  "uploadTimestamp": "2026-08-11T10:15:30.123Z",
  "ociObjectName": "leave-request.txt",
  "ociObjectId": "ocid1.object.oc1.us-ashburn-1.idqag2xakgns/sick-leaves-bucket/leave-request.txt"
}
```

---

## 6. Leave Categorization Logic

`CategorizationService` uses a keyword map. The first matching category is returned; if nothing matches, `OTHER` is returned.

| Category            | Keywords |
|---------------------|----------|
| `SICK_LEAVE`        | sick, fever, medical, ill, doctor, hospital |
| `ANNUAL_LEAVE`      | annual, vacation, holiday, leave, trip |
| `EMERGENCY_LEAVE`   | emergency, urgent, accident, crisis |
| `MATERNITY_LEAVE`   | maternity, pregnancy, baby, birth, newborn |
| `UNPAID_LEAVE`      | unpaid, without pay, no pay |
| `OTHER` (default)   | — |

Matching is case-insensitive (`content.toLowerCase()`).

---

## 7. OCI Object Storage Integration

`OciStorageService` uploads the file using the OCI Java SDK:

- Builds a `ConfigFileAuthenticationDetailsProvider` from the OCI config file + profile.
- Creates an `ObjectStorageClient` for the configured region.
- Sends a `PutObjectRequest` to the configured namespace/bucket.
- Returns `objectName` (the file name) and a constructed `objectId` string.

**Configuration keys (`application.properties`):**

| Key                 | Example Value                     | Description |
|---------------------|-----------------------------------|-------------|
| `oci.region`        | `us-ashburn-1`                    | OCI region |
| `oci.namespace`     | `idqag2xakgns`                    | Object Storage namespace |
| `oci.bucket-name`   | `sick-leaves-bucket`              | Target bucket |
| `oci.config-file`   | `C:/Users/Codeline/.oci/config`   | OCI config file path |
| `oci.profile`       | `DEFAULT`                         | OCI config profile |

> Note: `sick-leaves-bucket` is used as the target bucket for all uploads.

---

## 8. Database Layer

- **Driver:** Oracle JDBC thin driver (`ojdbc11`), runtime scope.
- **Dialect:** `OracleDialect`
- **DDL strategy:** `update` (Hibernate creates/updates tables automatically).
- **DataSource:** `jdbc:oracle:thin:@//129.213.126.140:1529/FREEPDB1`
- **Credentials:** from environment variables `DB_USERNAME` / `DB_PASSWORD` (loaded via `.env`).

### Table: `LEAVE_UPLOADS`

| Column                | Type            | Notes |
|-----------------------|-----------------|-------|
| `id`                  | `NUMBER` PK     | Auto-generated identity |
| `user_email`          | `VARCHAR`       | Authenticated user email |
| `attached_filename`   | `VARCHAR`       | Original uploaded file name |
| `reason_for_leave`    | `VARCHAR`       | Reason extracted from file |
| `leave_category`      | `VARCHAR`       | Enum stored as string |
| `created_at`          | `TIMESTAMP`     | Upload time (Instant) |
| `oci_object_name`     | `VARCHAR`       | OCI object name |
| `oci_object_id`       | `VARCHAR`       | OCI object identifier |
| `oci_bucket_name`     | `VARCHAR`       | Bucket name used |

`LeaveUploadRepository` (Spring Data JPA) provides `findByUserEmail(String userEmail)`.

> **Status:** The repository/entity are in place, but the controller currently does **not** persist records to the DB. Extending `LeaveController.upload()` to save a `LeaveUploadRecord` before returning the response is a straightforward next step.

---

## 9. Security Configuration

`SecurityConfig`:

- **CSRF:** disabled.
- **Permitted paths:** `/`, `/error`, `/login/**`, `/oauth2/**`.
- **All other requests:** authenticated.
- **OAuth2 login** with Google as the provider (client id/secret from `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` env vars), scopes `openid, profile, email`.
- On successful login the user is redirected to `/api/leave/status`.

---

## 10. Error Handling

`GlobalExceptionHandler` (`@RestControllerAdvice`):

| Exception              | HTTP Status | Body |
|------------------------|-------------|------|
| `IllegalArgumentException` (e.g., non-.txt upload) | `400 Bad Request` | `{"error": "<message>"}` |
| Any other `Exception`   | `500 Internal Server Error` | `{"error": "<message>"}` |

---

## 11. Configuration & Environment Variables

### `application.properties` — key settings
```properties
spring.application.name=leave-portal-app
spring.config.import=optional:file:.env[.properties]

oci.region=us-ashburn-1
oci.namespace=idqag2xakgns
oci.bucket-name=sick-leaves-bucket
oci.config-file=C:/Users/Codeline/.oci/config
oci.profile=DEFAULT

spring.security.oauth2.client.registration.google.client-id=${GOOGLE_CLIENT_ID}
spring.security.oauth2.client.registration.google.client-secret=${GOOGLE_CLIENT_SECRET}
spring.security.oauth2.client.registration.google.scope=openid,profile,email

spring.datasource.url=jdbc:oracle:thin:@//129.213.126.140:1529/FREEPDB1
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
spring.datasource.driver-class-name=oracle.jdbc.OracleDriver
spring.jpa.database-platform=org.hibernate.dialect.OracleDialect
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

server.port=8080
```

### `.env` variables
| Variable              | Description |
|-----------------------|-------------|
| `GOOGLE_CLIENT_ID`    | Google OAuth2 client ID |
| `GOOGLE_CLIENT_SECRET`| Google OAuth2 client secret |
| `DB_USERNAME`         | Oracle DB username |
| `DB_PASSWORD`         | Oracle DB password |

> The OCI credentials are read from the machine's OCI config file (default: `C:/Users/Codeline/.oci/config`).

---

## 12. How to Run

### Prerequisites
- JDK 17
- Maven (or use the bundled `mvnw` wrapper)
- An OCI config file with valid credentials at the configured path
- A configured Google OAuth2 app (client id + secret)
- Network access to the Oracle database endpoint

### Steps
```bash
# 1. From the project folder
cd leave-portal-app

# 2. Build the project
./mvnw clean package

# 3. Run the application
./mvnw spring-boot:run
```

The app starts on **`http://localhost:8080`**.

### Test flow
1. Open `http://localhost:8080` — you will be redirected to Google login.
2. After login you land on `/api/leave/status`, which shows your email.
3. Open `http://localhost:8080/upload-test.html` and upload a `.txt` file.
4. The response JSON shows the detected category, matched keywords, and OCI object references.

### Run tests
```bash
./mvnw test
```
The test suite currently contains a single `@SpringBootTest` context-load smoke test.

---

## 13. Dependencies (pom.xml summary)

**Runtime:**
- `spring-boot-starter-security`
- `spring-boot-starter-security-oauth2-client`
- `spring-boot-starter-webmvc`
- `spring-boot-starter-data-jpa`
- `oci-java-sdk-objectstorage` (3.80.3)
- `oci-java-sdk-common` (3.80.3)
- `oci-java-sdk-common-httpclient-jersey` (3.80.3)
- `javax.ws.rs-api` (2.1.1)
- `lombok`
- `ojdbc11` (runtime)

**Test:**
- `spring-boot-starter-security-oauth2-client-test`
- `spring-boot-starter-security-test`
- `spring-boot-starter-webmvc-test`

**Build plugin:** `spring-boot-maven-plugin`

---

## 14. Observations & Suggested Next Steps

1. **Persist uploads** — Wire `LeaveUploadRepository` into `LeaveController.upload()` to save `LeaveUploadRecord` rows (set `userEmail`, `attachedFilename`, `leaveCategory`, `createdAt`, OCI object/bucket fields).
2. **Extract `reasonForLeave`** — currently not populated; could parse the file body text.
3. **File size/type hardening** — enforce a max upload size in `application.properties` (`spring.servlet.multipart.max-file-size`).
4. **Repository-driven history endpoint** — e.g., `GET /api/leave/records` returning the user's past uploads.
5. **Better OCI object IDs** — the current `objectId` is a constructed string; use the real OCID returned by the SDK where available.
6. **Integration tests** — add tests for the categorization logic and mocked OCI upload.

---

*Generated for the Agile Oracles OCI Evaluation Task — branch `ALharithALk-86eyhdw6c`.*
