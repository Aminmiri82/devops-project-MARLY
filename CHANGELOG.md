# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [v1.0a] - 2026-02-24

### Changed
- Updated frontend flows across sign-in/login, journey search, and departure time handling
- Accessibility profiles now start with no profile enabled by default
- Google Tasks controller cleanup

### Security
- Hardened password validation against regex-based vulnerability issues
- Improved OAuth/JWT security wiring (authorized client persistence and JWT filter behavior)
- Fixed blocker-level issues in eco-score flow and related geocoding paths

### Testing
- Expanded and refactored automated test coverage (including parameterized controller/geocoding tests)
- Fixed failing UI tests in journey optimization flows

### CI/CD & Documentation
- Updated CI workflow configuration
- Refreshed v0.3 documentation artifacts on the dev line

## [v0.3] - 2026-02-17

### Added
- Green mode with eco-score dashboard and gamification features
- Badges and accessibility-related UI improvements
- Smart journey suggestions based on user preferences
- JWT authentication support
- New tests for journey optimization, deserialization, and UI

### Changed
- Renamed comfort mode to comfort/accessibility profiles
- Added wheelchair support with frontend toggle
- Restructured JavaScript files and updated security configurations
- Updated v0.3 documentation
- Improved SonarQube coverage reporting

### Fixed
- Broken UI tests
- Added JWT security mocks to controller tests
- Removed dead code
- Boosted overall test coverage to 92%

## [v0.2] - 2026-02-01

### Added
- Comfort mode (#12)
- Preferences (#15)

### Changed
- Routing refactor (#13)

### Fixed
- Dev fix task in path (#17)

## [v0.1] - 2025-12-27

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
