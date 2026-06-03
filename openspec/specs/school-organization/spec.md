# school-organization Specification

## Purpose
TBD - created by archiving change organization-management. Update Purpose after archive.
## Requirements
### Requirement: Create school organization
The system SHALL allow authorized users to create a new school organization with name, icon URL, school type, and school stages.

#### Scenario: Successful school creation
- **WHEN** an authorized user submits a valid school creation request with name, type, and stages
- **THEN** the system creates a new school record and returns the school ID

#### Scenario: Duplicate school name rejected
- **WHEN** a user attempts to create a school with a name that already exists
- **THEN** the system returns a conflict error

#### Scenario: Missing required fields rejected
- **WHEN** a user submits a school creation request without required fields (name, type, stages)
- **THEN** the system returns a validation error listing the missing fields

### Requirement: Update school organization
The system SHALL allow authorized users to update school attributes including name, icon URL, type, and stages.

#### Scenario: Successful school update
- **WHEN** an authorized user submits a valid update request for an existing school
- **THEN** the system updates the school record and returns the updated school data

#### Scenario: Update non-existent school rejected
- **WHEN** a user attempts to update a school that does not exist
- **THEN** the system returns a not found error

### Requirement: Query school organization
The system SHALL allow users to query school details and list all schools.

#### Scenario: Get school details
- **WHEN** a user requests details for a specific school by ID
- **THEN** the system returns the school information including name, icon, type, and stages

#### Scenario: List all schools
- **WHEN** a user requests the list of all schools
- **THEN** the system returns a paginated list of schools

### Requirement: School type enumeration
The system SHALL support the following school types: PUBLIC (公立), PRIVATE (私立), TRAINING_INSTITUTE (培训机构).

#### Scenario: School type assignment
- **WHEN** a school is created or updated with a valid school type
- **THEN** the system accepts and stores the type value

#### Scenario: Invalid school type rejected
- **WHEN** a user attempts to assign an invalid school type
- **THEN** the system returns a validation error

### Requirement: School stages
The system SHALL support school stages as a list of values: PRIMARY (小学), JUNIOR_HIGH (初中), SENIOR_HIGH (高中), UNIVERSITY (大学).

#### Scenario: Multiple stages assignment
- **WHEN** a school is created with multiple stages (e.g., PRIMARY and JUNIOR_HIGH)
- **THEN** the system stores all stages for the school

#### Scenario: Empty stages rejected
- **WHEN** a school is created with an empty stages list
- **THEN** the system returns a validation error

