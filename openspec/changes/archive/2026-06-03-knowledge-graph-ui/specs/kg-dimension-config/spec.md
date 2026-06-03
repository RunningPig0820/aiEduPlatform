## ADDED Requirements

### Requirement: Dropdown selectors from enums + MySQL

The system SHALL provide subject, grade, phase, and textbook dropdown options. Subject, phase, and textbook lists SHALL be derived from Java enum classes (fixed values). Grade list SHALL be queried from the existing `t_kg_textbook` table using DISTINCT.

#### Scenario: Query subject list from enum
- **WHEN** user sends GET request to `/api/kg/dimensions/subjects`
- **THEN** system returns all entries from `KgSubjectEnum`, sorted by orderIndex ascending

#### Scenario: Query grade list from MySQL
- **WHEN** user sends GET request to `/api/kg/dimensions/grades`
- **THEN** system returns DISTINCT `grade` values from `t_kg_textbook` where `status='active'`

#### Scenario: Query phase list from enum
- **WHEN** user sends GET request to `/api/kg/dimensions/phases`
- **THEN** system returns all entries from `KgPhaseEnum`, sorted by orderIndex ascending

#### Scenario: Query textbook list from enum
- **WHEN** user sends GET request to `/api/kg/dimensions/textbooks`
- **THEN** system returns all entries from `KgTextbookEnum`, sorted by orderIndex ascending, each entry containing uri, label, subject, grade, phase, orderIndex fields

#### Scenario: No textbook data exists (first use)
- **WHEN** `t_kg_textbook` table is empty (no sync has been run)
- **THEN** the grades API returns an empty array, but subjects, phases, and textbooks APIs still return their enum values

### Requirement: Navigation tree subject root nodes

The system SHALL provide a subject list endpoint for the navigation tree root, returning subject labels from enum.

#### Scenario: Get navigation subjects
- **WHEN** user sends GET request to `/api/kg/subjects`
- **THEN** system returns an array of subject label strings from `KgSubjectEnum`

### Requirement: Navigation tree subject-to-grade mapping

The system SHALL return the list of grades that have actual textbook data under a given subject.

#### Scenario: Get grades under a subject
- **WHEN** user sends GET request to `/api/kg/subjects/{subject}/grades`
- **THEN** system queries `t_kg_textbook WHERE subject=?` for DISTINCT grades and returns them as a string array

#### Scenario: Subject has no textbooks
- **WHEN** user requests grades for a subject with no associated textbooks
- **THEN** system returns an empty array `[]`

### Requirement: Navigation tree grade-to-textbook mapping

The system SHALL return all active textbooks belonging to a given grade.

#### Scenario: Get textbooks under a grade
- **WHEN** user sends GET request to `/api/kg/grades/{grade}/textbooks`
- **THEN** system returns active textbooks from `t_kg_textbook` where `grade` matches, sorted by uri or label

#### Scenario: Grade has no textbooks
- **WHEN** user requests textbooks for a grade with no textbooks
- **THEN** system returns an empty array `[]`

### Requirement: Knowledge point graph relations

The system SHALL return the graph data (nodes and edges) for a given knowledge point, querying Neo4j for related concepts and textbook hierarchies.

#### Scenario: Get knowledge point graph with relations
- **WHEN** user sends GET request to `/api/kg/knowledge-points/{uri}/graph`
- **THEN** system returns the knowledge point as the center node, plus related concept nodes and textbook hierarchy nodes, with labeled edges representing relationships

#### Scenario: Knowledge point has no graph relations
- **WHEN** the knowledge point exists but has no Neo4j relations
- **THEN** system returns the knowledge point as the only node with an empty edges array

#### Scenario: Knowledge point does not exist
- **WHEN** user requests graph for a non-existent knowledge point uri
- **THEN** system returns empty nodes and edges arrays `{"nodes": [], "edges": []}`
