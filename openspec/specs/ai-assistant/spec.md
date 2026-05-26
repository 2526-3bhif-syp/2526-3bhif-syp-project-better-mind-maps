## Purpose
Defines the Google Gemini integration for auto-generating hierarchically structured mind maps, alongside local mock fallsafes.

## Requirements
### Requirement: AI-Generated Mind Maps
The system SHALL request hierarchical content from the Gemini 2.5 Flash API and parse the indented text line-by-line into visual nodes and parent relations.

#### Scenario: Requesting and parsing AI mind map structure
- **GIVEN** a valid Gemini API key is configured
- **WHEN** the user inputs a topic and triggers AI generation
- **THEN** the system fetches, parses, and renders the hierarchy into nodes on the canvas

### Requirement: Smart Failsafe Local Fallback
If no API Key is provided or the connection fails, the system MUST dynamically generate structured fallback maps matching common subjects.

#### Scenario: Mock fallback generation for Programming
- **GIVEN** no Gemini API key is configured
- **WHEN** the user inputs the topic "Java Programming"
- **THEN** the application falls back and instantly populates the canvas with pre-defined Programming concept nodes
