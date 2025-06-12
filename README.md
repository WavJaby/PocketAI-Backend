# PocketAI-Backend

A Java backend application that integrates AI capabilities with image processing and LaTeX math rendering.

This repository is the backend for PocketAI devices:
- [PocketAI-ESP32Cam](https://github.com/WavJaby/PocketAI-ESP32Cam)
- [PocketAI-XIAO_ESP32](https://github.com/WavJaby/PocketAI-XIAO_ESP32)

## Features

- OpenAI API integration for AI-powered functionality
- LaTeX math formula rendering
- Image processing capabilities
- OTA (Over-The-Air) update server for device firmware updates

## Getting Started

### Configuration

Before running the application, you need to configure the `settings.properties` file:

1. On first run, the application will create a default `settings.properties` file in the project root
2. Edit the `settings.properties` file with your configuration:

```properties
OPEN_AI_KEY=your_openai_api_key_here
PROJECT_ID=your_openai_project_id
ORGANIZATION_ID=your_openai_organization_id
OTA_FILE=PocketAI-ESP32Cam.bin
PROBLEM_SOLVING_PROMPT=ProblemPrompt.md
RESULT_ANALYSIS_PROMPT=AnalysisPrompt.md
```

**Setting descriptions:**
- `OPEN_AI_KEY`: Your OpenAI API key for AI functionality
- `PROJECT_ID`: OpenAI project ID (optional)
- `ORGANIZATION_ID`: OpenAI organization ID (optional)
- `OTA_FILE`: Firmware binary file for over-the-air updates to ESP32 devices
- `PROBLEM_SOLVING_PROMPT`: Markdown file containing the AI prompt for problem solving (see example: `src/main/resources/CalculusPrompt.md`)
- `RESULT_ANALYSIS_PROMPT`: Markdown file containing the AI prompt for result analysis (see example: `src/main/resources/AnalysisPrompt.md`)

### Building the Project

```bash
./gradlew build
```

### Running the Application

```bash
# Build and run the shadow JAR
./gradlew shadowJar
java -jar build/libs/PocketAI-Backend-1.0-SNAPSHOT.jar
```

## Dependencies

- **Simple OpenAI**: Java client for OpenAI API
- **JLatexMath**: LaTeX math formula rendering