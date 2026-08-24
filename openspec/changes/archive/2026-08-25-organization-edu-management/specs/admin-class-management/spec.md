## ADDED Requirements

### Requirement: Department type differentiation
The system SHALL support a `department_type` field on `t_department` to distinguish `ORG` (normal organizational department) from `ADMIN_CLASS` (administrative class node).

#### Scenario: Create admin class department
- **WHEN** a department is created with `department_type = ADMIN_CLASS`
- **THEN** the department is persisted in `t_department` with the correct type and participates in the department tree via `parent_id`

#### Scenario: Default department type
- **WHEN** a department is created without specifying `department_type`
- **THEN** the system SHALL default to `ORG`

### Requirement: Educational extension table
The system SHALL provide a `t_department_edu` table that stores educational attributes via `dept_id` reverse association to `t_department`, including stage_code, stage_year_code, grade_code, and enrollment_year.

#### Scenario: Create education profile for admin class node
- **WHEN** an admin class node of dept_type = 3 (学段) is created
- **THEN** a `t_department_edu` record is created with `dept_id` pointing to the department and `stage_code`, `stage_year_code` filled

#### Scenario: Create grade node with educational attributes
- **WHEN** an admin class node of dept_type = 4 (年级) is created
- **THEN** a `t_department_edu` record is created with `stage_code`, `stage_year_code`, `grade_code`, and `enrollment_year` filled

#### Scenario: Create class node with educational attributes
- **WHEN** an admin class node of dept_type = 5 (班级) is created
- **THEN** a `t_department_edu` record is created with `stage_code`, `stage_year_code`, `grade_code`, and `enrollment_year` filled

### Requirement: Admin class tree hierarchy
The system SHALL reuse `t_department.parent_id` and `department_path` to manage the admin class hierarchy (学段 → 年级 → 班级) without introducing additional relationship tables.

#### Scenario: Build admin class tree
- **WHEN** admin class departments are queried by school_id with `department_type = ADMIN_CLASS`
- **THEN** the system SHALL return the full tree structure including 学段, 年级, and 班级 levels based on `parent_id` relationships

#### Scenario: Move admin class node
- **WHEN** a 班级 node is moved from one 年级 to another
- **THEN** the `parent_id` and `department_path` SHALL be recalculated following the existing department path algorithm

### Requirement: Admin class CRUD API
The system SHALL provide REST API endpoints to create, read, update, and delete admin class nodes with their educational extension attributes.

#### Scenario: Create a stage node
- **WHEN** POST request is sent to create a 学段 node with `stage_code = PRIMARY` and `stage_year_code = 4`
- **THEN** a `t_department` row is created with `department_type = ADMIN_CLASS` and a `t_department_edu` row is created with `dept_type = 3`

#### Scenario: Create a grade node under a stage
- **WHEN** POST request is sent to create a 年级 node with `parent_id` pointing to the 学段 and `grade_code = 1`, `enrollment_year = 2024`
- **THEN** a `t_department` row and `t_department_edu` row (dept_type = 4) are created

#### Scenario: Create a class node under a grade
- **WHEN** POST request is sent to create a 班级 node with `parent_id` pointing to the 年级
- **THEN** a `t_department` row and `t_department_edu` row (dept_type = 5) are created

#### Scenario: Query admin class tree by school
- **WHEN** GET request is sent with a school_id
- **THEN** the system SHALL return the full admin class tree for that school

### Requirement: Stage year code enumeration
The system SHALL define a `StageYearCode` value object with the following codified values: 4 (小学六年制), 5 (初中三年制), 7 (高中三年制).

#### Scenario: Validate stage year code
- **WHEN** a stage_year_code is provided
- **THEN** the system SHALL accept only defined values (4, 5, 7) and reject invalid codes

### Requirement: Grade code reuse existing GradeLevel
The system SHALL reuse the existing 1-12 `GradeLevel` numbering for `grade_code` in `t_department_edu`.

#### Scenario: Map grade code to display name
- **WHEN** grade_code = 1 in stage_code = PRIMARY
- **THEN** the system SHALL map this to "小学一年级" for display purposes
