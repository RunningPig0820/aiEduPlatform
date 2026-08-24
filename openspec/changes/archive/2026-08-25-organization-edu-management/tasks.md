## 1. Domain Layer - Value Objects

- [x] 1.1 Create `DepartmentType` value object (ORG, ADMIN_CLASS) in `com.ai.edu.domain.organization.model.valueobject`
- [x] 1.2 Create `DeptEduType` value object (3-学段, 4-年级, 5-班级) in `com.ai.edu.domain.organization.model.valueobject`
- [x] 1.3 Create `StageYearCode` value object (4-小学六年制, 5-初中三年制, 7-高中三年制) in `com.ai.edu.domain.organization.model.valueobject`

## 2. Domain Layer - Entity & Repository

- [x] 2.1 Add `departmentType` field to existing `Department` entity with default `DepartmentType.ORG`
- [x] 2.2 Create `DepartmentEdu` entity in `com.ai.edu.domain.organization.model.entity` with fields: deptId, schoolId, deptType, stageCode, stageYearCode, gradeCode, enrollmentYear, audit fields, factory methods for each dept_type (stage/grade/class)
- [x] 2.3 Create `DepartmentEduId` value object in `com.ai.edu.domain.organization.model.valueobject`
- [x] 2.4 Create `DepartmentEduRepository` interface in `com.ai.edu.domain.organization.repository` with methods: findByDeptId, findBySchoolId, save, deleteByDeptId
- [x] 2.5 Create `AdminClassAggregate` aggregate root encapsulating Department + DepartmentEdu, with factory methods for creating stage/grade/class nodes

## 3. Infrastructure Layer - Database Migration

- [x] 3.1 Create Flyway migration `V5__alter_t_department_add_type.sql`: ALTER TABLE t_department ADD COLUMN department_type VARCHAR(20) NOT NULL DEFAULT 'ORG'
- [x] 3.2 Create Flyway migration `V6__create_t_department_edu.sql`: CREATE TABLE t_department_edu with columns (id, dept_id, school_id, dept_type, stage_code, stage_year_code, grade_code, enrollment_year) and indexes on dept_id, school_id

## 4. Infrastructure Layer - Persistence

- [x] 4.1 Create `DepartmentEduPO` MyBatis-Plus entity mapped to `t_department_edu`
- [x] 4.2 Create `DepartmentEduMapper` extending `BaseMapper<DepartmentEduPO>`
- [x] 4.3 Create `DepartmentEduRepositoryImpl` implementing `DepartmentEduRepository`, bridging PO ↔ Domain entity
- [x] 4.4 Update `DepartmentPO` to include `departmentType` field
- [x] 4.5 Update `DepartmentRepositoryImpl` to handle `departmentType` in PO-to-entity conversion

## 5. Application Layer

- [x] 5.1 Create `CreateAdminClassNodeCommand` DTO with validation: deptType, name, parentId, schoolId, stageCode, stageYearCode, gradeCode, enrollmentYear
- [x] 5.2 Create `UpdateAdminClassNodeCommand` DTO
- [x] 5.3 Create `AdminClassNodeDTO` response object including both department and educational extension fields
- [x] 5.4 Create `AdminClassAppService` with `createNode()` — creates Department + DepartmentEdu in a transaction
- [x] 5.5 Add `updateNode()` to `AdminClassAppService`
- [x] 5.6 Add `getNodeTree(schoolId)` to `AdminClassAppService` — query departments by school_id + department_type=ADMIN_CLASS, build tree with edu attributes
- [x] 5.7 Add `getNodeDetail(deptId)` to `AdminClassAppService`
- [x] 5.8 Add `deleteNode(deptId)` to `AdminClassAppService` — soft delete both Department and DepartmentEdu

## 6. Interface Layer - REST API

- [x] 6.1 Create `AdminClassController` in `com.ai.edu.interfaces.api` with `@RestController` and `@RequestMapping("/api/admin-classes")`
- [x] 6.2 Implement `POST /api/admin-classes` — create admin class node, return `ApiResponse<AdminClassNodeDTO>`
- [x] 6.3 Implement `PUT /api/admin-classes/{id}` — update admin class node
- [x] 6.4 Implement `GET /api/admin-classes/{id}` — get node detail
- [x] 6.5 Implement `GET /api/admin-classes?schoolId={schoolId}` — get admin class tree for a school
- [x] 6.6 Implement `DELETE /api/admin-classes/{id}` — soft delete node

## 7. Integration & Testing

- [x] 7.1 Write unit tests for value objects: DepartmentType, DeptEduType, StageYearCode validation
- [x] 7.2 Write unit test for `DepartmentEdu` entity factory methods (stage/grade/class creation)
- [x] 7.3 Write unit test for `AdminClassAggregate` creation and invariants
- [x] 7.4 Write integration test for `AdminClassAppService.createNode()` — verify Department + DepartmentEdu created in transaction
- [x] 7.5 Write integration test for `AdminClassAppService.getNodeTree()` — verify correct tree structure
- [x] 7.6 Write integration test for admin class CRUD API endpoints
