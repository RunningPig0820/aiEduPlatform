## ADDED Requirements

### Requirement: Build grade knowledge system

The system SHALL build a complete knowledge system for a given grade, including all textbooks, chapters, sections, and knowledge points for that grade.

#### Scenario: Get grade knowledge system
- **WHEN** user sends GET request to `/api/kg/system/grade/一年级`
- **THEN** system returns the complete knowledge structure: all textbooks for that grade, with chapters, sections, and knowledge points nested

#### Scenario: Grade not found
- **WHEN** user requests system for a non-existent grade
- **THEN** system returns empty structure with zero counts

### Requirement: Grade knowledge system statistics

The system SHALL provide statistical breakdown of knowledge points for a given grade.

#### Scenario: Get grade knowledge statistics
- **WHEN** user sends GET request to `/api/kg/system/stats/一年级`
- **THEN** system returns total knowledge points count, difficulty distribution (easy/medium/hard counts), importance distribution, cognitive level distribution, and concept match rate

#### Scenario: Statistics for grade with no data
- **WHEN** user requests stats for a grade with no synced data
- **THEN** system returns all counts as zero

### Requirement: Knowledge system grouped by topic

The system SHALL allow grouping the grade knowledge system by chapter topic (专题).

#### Scenario: Get knowledge system grouped by topic
- **WHEN** user sends GET request to `/api/kg/system/grade/一年级?groupBy=topic`
- **THEN** system returns knowledge points grouped by topic (e.g., "数与代数", "图形与几何") instead of by textbook
