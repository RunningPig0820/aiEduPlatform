# org-domain-structure Specification

## Purpose
组织域核心结构：定义学校聚合根（`SchoolAggregate`）、学校值对象（`SchoolType`/`SchoolStage`/`SchoolId`）与仓储接口，并扩展行政班教育结构——`DepartmentType`/`DeptEduType`/`StageYearCode` 值对象、`DepartmentEdu` 实体、`Department.departmentType` 字段及对应 Flyway 迁移，支撑学段/年级/班级行政班树。

## Requirements
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
The system SHALL follow the DDD package structure convention under `com.ai.edu.domain.organization/` with sub-packages: `model/entity/`, `model/valueobject/`, `model/aggregate/`, and `repository/`. New value objects `DepartmentType`, `DeptEduType`, `StageYearCode` and entity `DepartmentEdu` SHALL be placed under the corresponding sub-packages.

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

### Requirement: Department type value object
The system SHALL define a `DepartmentType` value object with values `ORG` and `ADMIN_CLASS` to distinguish department categories.

#### Scenario: Department type validation
- **WHEN** a `DepartmentType` is created with a valid value
- **THEN** the system SHALL accept `ORG` and `ADMIN_CLASS` and reject any other value

### Requirement: DeptEduType value object
The system SHALL define a `DeptEduType` value object with values 3 (学段), 4 (年级), 5 (班级) to identify the admin class node level.

#### Scenario: DeptEduType validation
- **WHEN** a `DeptEduType` is created
- **THEN** the system SHALL accept only 3, 4, or 5 and reject other values

### Requirement: StageYearCode value object
The system SHALL define a `StageYearCode` value object with values 4 (小学六年制), 5 (初中三年制), 7 (高中三年制).

#### Scenario: StageYearCode validation
- **WHEN** a `StageYearCode` is created
- **THEN** the system SHALL accept only 4, 5, or 7 and reject other values

### Requirement: DepartmentEdu entity
The system SHALL define a `DepartmentEdu` entity in the organization domain representing the educational extension of a department, containing `deptId`, `schoolId`, `deptType`, `stageCode`, `stageYearCode`, `gradeCode`, and `enrollmentYear`.

#### Scenario: DepartmentEdu creation
- **WHEN** a `DepartmentEdu` entity is created with valid field values
- **THEN** the entity SHALL be correctly initialized with all invariants satisfied

### Requirement: Department entity extension
The system SHALL add a `departmentType` field to the existing `Department` entity with a default value of `ORG` to maintain backward compatibility.

#### Scenario: Existing department without type
- **WHEN** an existing `Department` is loaded without a `department_type` value
- **THEN** the system SHALL treat it as `ORG`

### Requirement: DepartmentEdu repository
The system SHALL define a `DepartmentEduRepository` interface in the domain layer with methods: `findByDeptId`, `save`, `deleteByDeptId`, `findBySchoolId`.

#### Scenario: Save and retrieve DepartmentEdu
- **WHEN** a `DepartmentEdu` entity is saved via the repository
- **THEN** the system SHALL persist it to `t_department_edu` and allow retrieval by `dept_id`

### Requirement: Flyway migration for department type
The system SHALL add a Flyway migration script `V5__alter_t_department_add_type.sql` that adds the `department_type` column to `t_department` with a default value of `ORG`.

#### Scenario: Migration adds department_type
- **WHEN** the migration runs on an existing database
- **THEN** the `department_type` column SHALL be added to `t_department` and all existing rows SHALL be set to `ORG`

### Requirement: Flyway migration for department edu table
The system SHALL add a Flyway migration script `V6__create_t_department_edu.sql` that creates the `t_department_edu` table with appropriate indexes.

#### Scenario: Migration creates educational extension table
- **WHEN** the migration runs on a clean database
- **THEN** the `t_department_edu` table SHALL be created with columns: id, dept_id, school_id, dept_type, stage_code, stage_year_code, grade_code, enrollment_year, and audit fields (created_by, created_on, modified_by, modified_on, is_deleted)
