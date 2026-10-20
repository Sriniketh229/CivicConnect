# CivicConnect

**Digital Municipal Service Management System**

CivicConnect is a web application designed to help citizens report local civic issues, track complaints, and provide municipal staff with a workspace for managing and resolving reported issues.

## Features

### Citizen
- Report local issues with a title, category, description, and locality.
- Attach an image to a complaint.
- Track a complaint using its reference code.
- View submitted complaints and their status.

### Municipal staff
- View and search complaints.
- Filter complaints by status, category, and locality.
- Update complaint status.
- Assign complaints to departments.
- Review complaint history.

### Supported complaint categories
- Garbage Collection
- Roads & Potholes
- Drainage Problems
- Streetlight Issues
- Water Supply

## Technology Stack

| Component | Technology |
|---|---|
| Frontend | React 19, TypeScript 5.7 |
| Build tool | Vite 8 |
| Styling | Tailwind CSS v4 |
| Backend | Java, Spring Boot 3.3.4 |
| Database | Supabase PostgreSQL |
| Database migrations | Flyway |
| Image storage | Supabase Storage |
| Backend testing | JUnit / Spring Boot Test |
| UI testing | Playwright |

## Project Structure

```text
CivicConnect/
├── backend/                 # Spring Boot backend
│   ├── src/
│   ├── application-local.properties.example
│   ├── .env.example
│   └── pom.xml
├── public/                  # Static frontend assets
├── src/                     # React application
├── tests/                   # Playwright tests
├── .gitignore
├── package.json
├── pnpm-lock.yaml
├── playwright.config.ts
└── README.md
```

## Prerequisites

Install the following before running the project:

- Node.js and pnpm
- Java Development Kit (JDK) 21 or compatible
- Apache Maven 3.9 or later
- A Supabase project with PostgreSQL access

## Getting Started

### 1. Frontend

From the project root, install dependencies and start the development server:

```bash
pnpm install
pnpm run dev
```

Open the local URL printed by Vite, normally:

`http://localhost:5173`

To create a production build:

```bash
pnpm run build
```

### 2. Backend configuration

Open a terminal in the `backend` directory.

Copy the example configuration to a local configuration file:

**PowerShell:**

```powershell
Copy-Item application-local.properties.example application-local.properties
```

Edit `application-local.properties` locally and configure the required values for:

- Supabase PostgreSQL connection
- Supabase project URL
- Supabase service-role key
- Private storage bucket name
- JWT secret and expiration
- Allowed CORS origins

Use the Supabase dashboard to obtain the appropriate connection details. Generate a strong, random JWT secret for local development.

**Never commit `application-local.properties` or expose database passwords, service-role keys, or JWT secrets.** The local configuration file is excluded by `.gitignore`.

### 3. Start the backend

From the `backend` directory:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

The backend normally runs at:

`http://localhost:8080`

Health check:

`http://localhost:8080/api/actuator/health`

The frontend and backend should both be running for full application functionality.

## API Overview

The backend provides REST endpoints for complaint management and departments.

| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/api/v1/departments` | List departments |
| POST | `/api/v1/complaints` | Create a complaint |
| GET | `/api/v1/complaints` | List complaints |
| GET | `/api/v1/complaints/my?userId=` | List complaints for a user ID |
| GET | `/api/v1/complaints/ref/{ref}` | Retrieve a complaint by reference |
| GET | `/api/v1/complaints/track/{code}` | Track a complaint |
| PATCH | `/api/v1/complaints/{id}/status` | Update complaint status |
| PATCH | `/api/v1/complaints/{id}/department?departmentId={uuid}` | Assign a department |
| GET | `/api/v1/complaints/{id}/history` | Retrieve complaint history |

Check the backend source for request payloads, validation rules, and supported status transitions.

## Testing

### Frontend production build

```bash
pnpm run build
```

### Frontend browser tests

```bash
pnpm exec playwright test
```

### Backend tests

From the `backend` directory:

```bash
mvn test
```

## Security and Configuration Notes

- Store credentials in local configuration or environment variables, never in source control.
- Keep Supabase service-role credentials exclusively on the backend.
- Configure the storage bucket as private when storing complaint attachments.
- The current project includes security-related dependencies, but the authentication and authorization implementation must be reviewed before production deployment.
- A user or staff workspace selector should not be treated as proof of identity or authorization.
- Configure appropriate CORS origins and secure production secrets before deployment.

## Project Status

CivicConnect is a course-end project under development. Validate the complete complaint submission, image upload, tracking, department assignment, and resolution workflows in the intended environment before deployment.

## License

No license has been specified yet. Add a license before distributing the project for reuse.
