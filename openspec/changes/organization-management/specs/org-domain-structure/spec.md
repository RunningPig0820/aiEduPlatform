## ADDED Requirements

### Requirement: School aggregate root
The system SHALL define `SchoolAggregate` as the aggregate root for school organization in the domain layer, encapsulating all school business rules.

#### Scenario: Create school via factory method
- **WHEN** the domain service creates a new school using the factory method
- **THEN** a valid `SchoolAggregate` instance is created with all invariants satisfied

#### Scenario: School aggregate prevents invalid state
- **WHEN** an operation would put the school aggregate into an invalid state (e.g., empty name)
- **THEN** the aggregate throws a domain exception

### Requirement: School value objects
The system SHALL define value objects for `SchoolType`, `SchoolStage`, and `SchoolId` within the organization bounded context.

#### Scenario: Value object immutability
- **WHEN** a value object is created
- **THEN** its state cannot be modified; a new instance must be created for any change

### Requirement: Repository interface
The system SHALL define `SchoolRepository` interface in the domain layer with methods for persistence operations.

#### Scenario: Save school aggregate
- **WHEN** the application service calls `schoolRepository.save(aggregate)`
- **THEN** the school aggregate is persisted

#### Scenario: Find school by ID
- **WHEN** the application service calls `schoolRepository.findById(id)`
- **THEN** the system returns the school aggregate or empty if not found

### Requirement: Package structure
The system SHALL follow the DDD package structure convention under `com.ai.edu.domain.organization/` with sub-packages: `model/entity/`, `model/valueobject/`, `model/aggregate/`, and `repository/`.

#### Scenario: Package structure compliance
- **WHEN** a new developer navigates to the organization domain package
- **THEN** they find entities in `model/entity/`, value objects in `model/valueobject/`, aggregate roots in `model/aggregate/`, and repository interfaces in `repository/`

### Requirement: JPA repository implementation
The system SHALL implement the domain repository interface using Spring Data JPA, scanned from `com.ai.edu.infrastructure.persistence.jpa`.

#### Scenario: JPA entity mapping
- **WHEN** the school aggregate is saved via JPA
- **THEN** all fields are correctly mapped to database columns

### Requirement: Flyway migration
The system SHALL use Flyway for database schema management, with migration scripts for new tables.

#### Scenario: Clean migration execution
- **WHEN** the application starts with a fresh database
- **THEN** Flyway applies all migration scripts in order without errors
