## ADDED Requirements

### Requirement: Navigate textbook hierarchy (6-level tree)

The system SHALL support a 6-level navigation tree: **学科 → 年级 → 教材 → 章节 → 小节 → 知识点**. Each level provides the data needed to render the next level.

#### Scenario: Get subject root nodes
- **WHEN** user sends GET request to `/api/kg/subjects`
- **THEN** system returns an array of subject label strings: `["数学", "语文", "英语", ...]`

#### Scenario: Get grades under a subject
- **WHEN** user sends GET request to `/api/kg/subjects/{subject}/grades`
- **THEN** system returns `{uri, label}` pairs for grades that have actual textbook data under the specified subject

#### Scenario: Get textbooks under a grade
- **WHEN** user sends GET request to `/api/kg/grades/{grade}/textbooks`
- **THEN** system returns active textbooks belonging to the grade with uri, label, grade, subject, phase, status fields

### Requirement: List textbooks with filters

The system SHALL return a list of textbooks, optionally filtered by subject, grade, and phase.

#### Scenario: Get all textbooks
- **WHEN** user sends GET request to `/api/kg/textbooks`
- **THEN** system returns all textbooks with uri, label, grade, phase, and subject fields

#### Scenario: Filter by phase
- **WHEN** user sends GET request to `/api/kg/textbooks?phase=primary`
- **THEN** system returns only textbooks with phase "primary"

#### Scenario: Filter by subject
- **WHEN** user sends GET request to `/api/kg/textbooks?subject=math`
- **THEN** system returns only textbooks with subject "math"

### Requirement: Navigate textbook chapter tree

The system SHALL return the hierarchical structure of a textbook with chapters, sections, and knowledge point counts.

#### Scenario: Get textbook chapter tree
- **WHEN** user sends GET request to `/api/kg/textbooks/{uri}/chapters` with a URL-encoded textbook uri
- **THEN** system returns chapters with their nested sections, each showing knowledge point count

#### Scenario: Empty chapters filtered out
- **WHEN** user requests chapters for a textbook where some chapters have no sections
- **THEN** system returns only chapters that have at least one section (empty chapters auto-filtered)

#### Scenario: Textbook not found
- **WHEN** user requests chapters for a non-existent textbook uri
- **THEN** system returns error code "70001" and message "教材不存在"

### Requirement: Get section knowledge points

The system SHALL return all knowledge points belonging to a given section.

#### Scenario: Get section knowledge points
- **WHEN** user sends GET request to `/api/kg/sections/{uri}/points` with a URL-encoded section uri
- **THEN** system returns knowledge points with uri, label, difficulty, importance, cognitive_level fields

#### Scenario: Section not found
- **WHEN** user requests points for a non-existent section uri
- **THEN** system returns error code "70004" and message "小节不存在"

### Requirement: Get knowledge point details

The system SHALL return full details for a single knowledge point, including 2 levels of parent context (section label and chapter label). The response SHALL NOT include higher-level parents like textbook or grade.

#### Scenario: Get knowledge point details with 2 parent levels
- **WHEN** user sends GET request to `/api/kg/knowledge-points/{uri}` with a URL-encoded knowledge point uri
- **THEN** system returns the knowledge point with uri, label, difficulty, importance, sectionLabel, and chapterLabel fields, but NOT textbookLabel or grade

#### Scenario: Knowledge point not found
- **WHEN** user requests details for a non-existent knowledge point uri
- **THEN** system returns error code "70003" and message "知识点不存在"

#### Scenario: Soft-deleted knowledge point
- **WHEN** user requests details for a knowledge point with status='deleted'
- **THEN** system returns error code "70003" and message "知识点不存在" (treats status≠active as non-existent)

#### Scenario: Merged knowledge point
- **WHEN** user requests details for a knowledge point with status='merged'
- **THEN** system returns error code "70003" and message "知识点不存在" (treats status≠active as non-existent)

### Requirement: Neo4j health check

The system SHALL provide a Neo4j health check endpoint to verify connectivity.

#### Scenario: Neo4j is available
- **WHEN** user sends GET request to `/api/kg/neo4j/health`
- **THEN** system returns `{"available": true, "responseTimeMs": 50}`

#### Scenario: Neo4j is unavailable
- **WHEN** Neo4j connection times out or fails
- **THEN** system returns `{"available": false, "error": "connection timeout"}` with HTTP 200 (not 500, since MySQL navigation still works)

### Requirement: Batch concept relations query

The system SHALL allow querying relations for multiple concept URIs in a single request to avoid N+1 queries.

#### Scenario: Batch query concept relations
- **WHEN** user sends POST request to `/api/kg/concepts/batch-relations` with body `{"uris": ["uri1", "uri2", "uri3"]}`
- **THEN** system queries Neo4j for all URIs and returns a map of uri → relations (with Redis cache, TTL 5min)

#### Scenario: Neo4j unavailable during batch query
- **WHEN** Neo4j is down and user requests batch relations
- **THEN** system returns `{"available": false}` and the frontend hides the graph module (graceful degradation)
