# school-permission-scope Specification

## Purpose
TBD - created by archiving change organization-management. Update Purpose after archive.
## Requirements
### Requirement: Associate user with school
The system SHALL allow associating a user with a school organization and assigning a role type for permission scoping.

#### Scenario: Successful user-school association
- **WHEN** an administrator associates a user with a school and assigns a role
- **THEN** the system creates a school-user association record

#### Scenario: Duplicate association rejected
- **WHEN** a user is already associated with a school and an attempt is made to create a duplicate association
- **THEN** the system returns a conflict error

### Requirement: Resolve user's school scope
The system SHALL determine which schools a user has access to and their role within each school upon authentication.

#### Scenario: User with single school association
- **WHEN** a user who is associated with exactly one school logs in
- **THEN** the system sets that school as the user's default school scope

#### Scenario: User with multiple school associations
- **WHEN** a user who is associated with multiple schools logs in
- **THEN** the system returns the list of schools and the user selects one as the current scope

#### Scenario: User with no school association
- **WHEN** a user who has no school association attempts to access school-scoped features
- **THEN** the system denies access and returns a permission error

### Requirement: Enforce school-scoped permissions
The system SHALL enforce that access to school-scoped features (教职工管理, 学生管理, 学年学期管理) requires the user to be associated with the target school.

#### Scenario: Access school feature without association
- **WHEN** a user attempts to access a school feature without being associated with that school
- **THEN** the system returns a 403 Forbidden response

#### Scenario: Access school feature with valid association
- **WHEN** a user with a valid school association accesses a school feature
- **THEN** the system allows access and scopes data to the user's school

### Requirement: Role-based school access
The system SHALL support different role types for school access: ADMIN (管理员), TEACHER (教师), STUDENT (学生), PARENT (家长).

#### Scenario: Admin full access
- **WHEN** a user with ADMIN role accesses any school feature
- **THEN** the system grants full access

#### Scenario: Teacher limited access
- **WHEN** a user with TEACHER role accesses school features
- **THEN** the system grants access limited to teacher-scoped functionality

