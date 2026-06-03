## ADDED Requirements

### Requirement: Flyway migration script organization

The system SHALL organize Flyway migration scripts into separate directories per database: `db/migration/user/` for `ai_edu_user` and `db/migration/kg/` for `ai_edu_kg`.

#### Scenario: User migration scripts location
- **WHEN** Flyway scans for user database migrations
- **THEN** it reads scripts from `classpath:db/migration/user/`

#### Scenario: KG migration scripts location
- **WHEN** Flyway scans for knowledge graph database migrations
- **THEN** it reads scripts from `classpath:db/migration/kg/`

### Requirement: Secondary Flyway instance for KG database

The system SHALL provide a second Flyway instance (as a custom Spring Bean) that connects to the `ai_edu_kg` datasource and runs migrations from `classpath:db/migration/kg/`.

#### Scenario: KG Flyway bean initialization
- **WHEN** application starts with `spring.flyway-kg.enabled=true`
- **THEN** a custom Flyway bean is created, connects to `ai_edu_kg`, and executes pending migrations from `db/migration/kg/`

#### Scenario: KG Flyway disabled by default
- **WHEN** `spring.flyway-kg.enabled=false` (default)
- **THEN** no Flyway migration runs against `ai_edu_kg`

### Requirement: Initial knowledge graph migration script

The system SHALL provide an initial migration script `V1__Init_Knowledge_Graph.sql` under `db/migration/kg/` that creates all 7 knowledge graph tables (4 node tables + 3 relation tables + 1 sync record table) in `ai_edu_kg`.

#### Scenario: Run initial KG migration
- **WHEN** `V1__Init_Knowledge_Graph.sql` is executed against `ai_edu_kg`
- **THEN** tables `t_kg_textbook`, `t_kg_chapter`, `t_kg_section`, `t_kg_knowledge_point`, `t_kg_textbook_chapter`, `t_kg_chapter_section`, `t_kg_section_kp`, and `t_kg_sync_record` are created with correct columns, indexes, and constraints
