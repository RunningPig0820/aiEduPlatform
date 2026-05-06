## 1. Domain Layer - Value Objects & Entities

- [ ] 1.1 Create `SchoolId` value object in `com.ai.edu.domain.organization.model.valueobject`
- [ ] 1.2 Create `SchoolType` enum (PUBLIC, PRIVATE, TRAINING_INSTITUTE)
- [ ] 1.3 Create `SchoolStage` enum (PRIMARY, JUNIOR_HIGH, SENIOR_HIGH, UNIVERSITY)
- [ ] 1.4 Create `SchoolAggregate` aggregate root with factory method `SchoolAggregate.create()`
- [ ] 1.5 Add school business rules: name uniqueness validation, non-empty stages validation
- [ ] 1.6 Create `SchoolUserAssociationId` value object
- [ ] 1.7 Create `SchoolUserRole` enum (ADMIN, TEACHER, STUDENT, PARENT)
- [ ] 1.8 Create `SchoolUserAssociation` entity for user-school relationship

## 2. Domain Layer - Repository Interfaces

- [ ] 2.1 Create `SchoolRepository` interface in `com.ai.edu.domain.organization.repository`
- [ ] 2.2 Define repository methods: `findById`, `save`, `deleteById`, `findByName`, `findAll`, `existsByName`
- [ ] 2.3 Create `SchoolUserRepository` interface for user-school association queries
- [ ] 2.4 Define repository methods: `findByUserId`, `findBySchoolId`, `save`, `existsBySchoolIdAndUserId`

## 3. Infrastructure Layer - Persistence

- [ ] 3.1 Create JPA entity `SchoolJpaEntity` mapped to `school` table
- [ ] 3.2 Create JPA entity `SchoolUserJpaEntity` mapped to `school_user` table
- [ ] 3.3 Create `SchoolJpaRepository` extending JpaRepository
- [ ] 3.4 Create `SchoolUserJpaRepository` extending JpaRepository
- [ ] 3.5 Implement `SchoolRepositoryImpl` bridging JPA to domain
- [ ] 3.6 Implement `SchoolUserRepositoryImpl` bridging JPA to domain

## 4. Infrastructure Layer - Database Migration

- [ ] 4.1 Create Flyway migration script `V{version}__create_school_table.sql`
- [ ] 4.2 Create Flyway migration script `V{version}__create_school_user_table.sql`
- [ ] 4.3 Verify migration runs cleanly on fresh MySQL instance

## 5. Application Layer

- [ ] 5.1 Create `CreateSchoolCommand` DTO with validation annotations
- [ ] 5.2 Create `UpdateSchoolCommand` DTO
- [ ] 5.3 Create `SchoolDTO` response object
- [ ] 5.4 Create `SchoolApplicationService` with `createSchool()` method
- [ ] 5.5 Create `SchoolApplicationService.updateSchool()` method
- [ ] 5.6 Create `SchoolApplicationService.getSchoolById()` method
- [ ] 5.7 Create `SchoolApplicationService.listSchools()` method
- [ ] 5.8 Create `AssociateUserWithSchoolCommand` DTO
- [ ] 5.9 Create `OrganizationApplicationService.associateUserWithSchool()` method
- [ ] 5.10 Create `OrganizationApplicationService.checkUserSchoolPermission()` method

## 6. Interface Layer - REST API

- [ ] 6.1 Create `SchoolController` in `com.ai.edu.interface_.api`
- [ ] 6.2 Implement `POST /api/schools` - create school endpoint
- [ ] 6.3 Implement `PUT /api/schools/{id}` - update school endpoint
- [ ] 6.4 Implement `GET /api/schools/{id}` - get school details endpoint
- [ ] 6.5 Implement `GET /api/schools` - list schools endpoint (paginated)
- [ ] 6.6 Implement `POST /api/schools/{schoolId}/users` - associate user with school
- [ ] 6.7 Implement `GET /api/users/{userId}/schools` - get user's schools
- [ ] 6.8 All endpoints return `ApiResponse<T>` wrapper

## 7. Integration - Spring Security

- [ ] 7.1 Create `@SchoolScoped` custom annotation for method-level school permission check
- [ ] 7.2 Create `SchoolPermissionInterceptor` that intercepts requests and validates school association
- [ ] 7.3 Integrate school ID into SecurityContext for current session

## 8. Module Configuration

- [ ] 8.1 Register organization domain as a Spring Modulith module
- [ ] 8.2 Configure JPA repository scanning for organization entities
- [ ] 8.3 Add module documentation
