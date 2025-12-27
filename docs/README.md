# Documentation Build Instructions

This documentation is written in AsciiDoc format with PlantUML diagrams.

## Prerequisites

Install Asciidoctor with PlantUML support:

```bash
# macOS
brew install asciidoctor
gem install asciidoctor-pdf asciidoctor-diagram

# Ubuntu/Debian
sudo apt-get install asciidoctor
gem install asciidoctor-pdf asciidoctor-diagram

# Or use Docker (recommended)
docker pull asciidoctor/docker-asciidoctor
```

## Generate PDF

### Using local installation:

```bash
asciidoctor-pdf -r asciidoctor-diagram documentation.adoc -o Mavigo-Documentation.pdf
```

### Using Docker:

```bash
docker run --rm -v $(pwd):/documents asciidoctor/docker-asciidoctor \
    asciidoctor-pdf -r asciidoctor-diagram documentation.adoc -o Mavigo-Documentation.pdf
```

## Generate HTML

```bash
asciidoctor -r asciidoctor-diagram documentation.adoc -o documentation.html
```

## PlantUML Diagrams

The documentation includes the following UML diagrams:

- **Architecture globale**: System architecture overview
- **Class diagram**: Main model classes and relationships
- **Sequence diagrams**: Journey planning and disruption handling flows
- **Package structure**: Code organization

All diagrams are written in PlantUML and compiled automatically during PDF generation.
