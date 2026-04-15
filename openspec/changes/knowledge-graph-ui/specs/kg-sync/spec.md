## ADDED Requirements

### Requirement: Full sync from Neo4j to MySQL (按需触发 + UPSERT + 状态机)

The system SHALL provide a full synchronization capability that is manually triggered on-demand. It reads nodes and hierarchical relations from Neo4j and writes them to MySQL tables using UPSERT strategy by URI. The system SHALL NOT truncate or physically delete existing data.

#### Scenario: Trigger full sync with concurrency control
- **WHEN** user sends POST request to `/api/kg/sync/full`
- **THEN** system acquires sync lock, creates a sync record, connects to Neo4j, and begins full synchronization

#### Scenario: Trigger directed sync with parameters
- **WHEN** user sends POST request to `/api/kg/sync/full` with body `{"subject":"math","grade":"一年级"}`
- **THEN** system only syncs textbooks matching the specified subject and grade, and records the scope in the sync record

#### Scenario: Node UPSERT by URI
- **WHEN** Neo4j contains a textbook with uri="xxx" that exists in MySQL with different label
- **THEN** system UPDATEs the existing row's label field and increments updatedCount

#### Scenario: New node inserted by URI
- **WHEN** Neo4j contains a knowledge point with uri="xxx" that does not exist in MySQL
- **THEN** system INSERTs a new row in t_kg_knowledge_point and increments insertedCount

#### Scenario: Hierarchical relation full rebuild
- **WHEN** sync runs and Neo4j has CONTAINS relations between a textbook and its chapters
- **THEN** system DELETEs all existing rows from t_kg_textbook_chapter and INSERTs fresh rows from Neo4j

#### Scenario: Node status changed to deleted
- **WHEN** a knowledge point exists in MySQL with status='active' but is no longer present in Neo4j
- **THEN** system sets `status = 'deleted'` for that row and increments statusChangedCount. The row is NOT physically deleted.

#### Scenario: URI validation during sync
- **WHEN** Neo4j returns a node with empty or malformed URI
- **THEN** system skips that node, records the URI error in sync record details, and continues processing remaining nodes

#### Scenario: Sync completes successfully
- **WHEN** full sync completes without errors
- **THEN** system updates the sync record status to "success", records insertedCount/updatedCount/statusChangedCount, sets finished_at timestamp, and runs reconciliation check

#### Scenario: Sync fails
- **WHEN** Neo4j connection fails or data writing encounters an error during sync
- **THEN** system updates the sync record status to "failed", records the error message, releases the sync lock, and the entire transaction rolls back (no partial data left)

### Requirement: Sync reconciliation (对账校验)

After each successful sync, the system SHALL automatically reconcile MySQL data against Neo4j to verify consistency.

#### Scenario: Reconciliation matches
- **WHEN** sync completes and MySQL node counts match Neo4j node counts
- **THEN** system sets reconciliation_status to "matched" in the sync record

#### Scenario: Reconciliation mismatch
- **WHEN** sync completes but MySQL node counts differ from Neo4j
- **THEN** system sets reconciliation_status to "mismatched", records the counts difference in reconciliation_details JSON, and still marks sync as "success" (data was written, but flagged for review)

### Requirement: Data consistency during sync (事务原子性)

The system SHALL ensure that frontend queries are not affected by incomplete sync data. The entire sync process (node UPSERT + relation rebuild + status change + reconciliation) SHALL run within a single Spring `@Transactional` transaction. All read queries SHALL filter by `status = 'active'`.

#### Scenario: Frontend reads during sync
- **WHEN** a sync is in progress and a user queries the navigation API
- **THEN** system returns the old complete dataset (transaction not yet committed), never partial or empty data

#### Scenario: Sync failure rolls back completely
- **WHEN** sync fails halfway through (e.g., after clearing relation tables but before re-inserting)
- **THEN** the entire transaction rolls back, relation tables retain their previous data, no partial state persists

#### Scenario: Query excludes non-active data
- **WHEN** user queries `/api/kg/knowledge-points/{uri}` for a knowledge point with status='deleted'
- **THEN** system returns error code "70003" and message "知识点不存在" (treats non-active as non-existent)

### Requirement: Query sync status

The system SHALL allow users to query the current sync status including running, success, or failed state, with inserted/updated/deleted counts and reconciliation status.

#### Scenario: Query sync status while running
- **WHEN** user sends GET request to `/api/kg/sync/status` during an active sync
- **THEN** system returns status "running" with processedNodes count and startedAt timestamp

#### Scenario: Query sync status when idle
- **WHEN** user sends GET request to `/api/kg/sync/status` with no active sync
- **THEN** system returns status "idle" with last sync details including insertedCount/updatedCount/statusChangedCount/reconciliationStatus

### Requirement: View sync history

The system SHALL return a list of past sync records with their details including inserted/updated/deleted counts, scope, and reconciliation status.

#### Scenario: View sync history
- **WHEN** user sends GET request to `/api/kg/sync/records`
- **THEN** system returns a list of sync records sorted by started_at descending, including sync_type, scope, status, insertedCount, updatedCount, statusChangedCount, reconciliationStatus, and timestamps
