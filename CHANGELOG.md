# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [v0.1] - 2024-12-27

### Added
- **Journey Planning**: Integration with PRIM API (Ile-de-France Mobilites) for real-time journey planning in Paris
- **User Management**: User authentication and profile management with OAuth2 Google integration
- **Google Tasks Integration**: Create and manage tasks linked to your journeys
- **Real-time Perturbations**: Live disruption alerts and reroutage suggestions
- **Progress Tracking**: Track your current journey with live location updates
- **Metro/Bus Display**: Show number of metro and bus connections in journey plans
- **Frontend**: Responsive web interface for journey planning and task management

### Features
- Journey planning with departure/arrival selection
- Real-time disruption notifications
- Live reroutage when perturbations occur
- Google Tasks synchronization
- Location-aware task management
- User location tracking during journeys
- Progress bar for current journey

### Technical
- Spring Boot 3.5.7 backend with Java 21
- H2 database for development
- OAuth2 authentication with Google
- PRIM API client for journey data
- Disruption API client for real-time alerts
- RESTful API endpoints for all features

### Infrastructure
- GitHub Actions CI/CD pipeline
- Automated build and test workflow
- Apache License 2.0
