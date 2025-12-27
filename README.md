# Mavigo - Paris Public Transport Assistant

[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![CI](https://github.com/Aminmiri82/devops-project-MARLY/actions/workflows/ci.yml/badge.svg)](https://github.com/Aminmiri82/devops-project-MARLY/actions/workflows/ci.yml)

## About

Mavigo is a personal public transport assistant for the city of Paris. It helps users navigate the Parisian transport network with intelligent journey planning and quality-of-life features including:

- Journey planning with real-time data from PRIM (Ile-de-France Mobilites API)
- Real-time disruption alerts
- Google Tasks integration for trip management

## Team

| Name |
|------|
| Marie BIBI |
| Raphael MEIMOUN |
| Seyed Amineddin MIRI |
| Malado SOW |

## Technologies

### Backend
- **Framework**: Spring Boot 3.5.7
- **Language**: Java 21
- **Database**: H2 (development) / PostgreSQL compatible
- **Build Tool**: Gradle 9.1
- **Authentication**: OAuth2 with Google

### Frontend
- **Stack**: Vanilla JavaScript, HTML5, CSS3 (no framework)
- **Served by**: Spring Boot static resources

### External APIs
- PRIM (Ile-de-France Mobilites) for journey planning
- Google Tasks API for task management

## Getting Started

### Prerequisites

- Java 21 or higher
- Gradle 9.1+ (or use the included wrapper)

### Configuration

1. Clone the repository:
   ```bash
   git clone https://github.com/Aminmiri82/devops-project-MARLY.git
   cd devops-project-MARLY
   ```

2. Configure environment variables in `Mavigo/local.env`:
   ```
   PRIM_API_KEY=your_prim_api_key
   GOOGLE_CLIENT_ID=your_google_client_id
   GOOGLE_CLIENT_SECRET=your_google_client_secret
   ```

### Running the Application

Navigate to the Mavigo directory and run:

```bash
cd Mavigo
./gradlew bootRun
```

The application will start on **http://localhost:8080**

### Building

To build a JAR file:

```bash
cd Mavigo
./gradlew build
```

The JAR will be created in `Mavigo/build/libs/`

### Running Tests

```bash
cd Mavigo
./gradlew test
```

## Project Structure

```
devops-project-MARLY/
├── Mavigo/                     # Main application
│   ├── src/main/java/          # Java source code
│   │   └── org/marly/mavigo/
│   │       ├── controller/     # REST API controllers
│   │       ├── service/        # Business logic
│   │       ├── client/         # External API clients
│   │       ├── models/         # JPA entities
│   │       └── repository/     # Data repositories
│   ├── src/main/resources/
│   │   └── static/             # Frontend files
│   └── src/test/               # Unit and integration tests
├── docs/                       # Documentation
└── README.md
```

## API Endpoints

| Endpoint | Description |
|----------|-------------|
| `/api/journey` | Journey planning |
| `/api/user` | User management |
| `/api/tasks` | Google Tasks integration |
| `/api/perturbations` | Disruption information |

## Documentation

Full documentation is available in the [docs/](docs/) folder and as a PDF in the [releases](https://github.com/Aminmiri82/devops-project-MARLY/releases).

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.
