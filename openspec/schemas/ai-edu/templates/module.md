## Module Overview

<!-- Brief description of this module's purpose and scope -->

## Bounded Context

<!-- Which bounded context does this module belong to? (User / Organization / Question / Homework / Learning / KnowledgeGraph) -->

## Package Structure

<!--
Expected package structure under com.ai.edu.{layer}.*
Example:
com.ai.edu.domain.knowledgeGraph/
├── model/
│   ├── entity/        # KnowledgeNode, KnowledgeEdge
│   └── valueobject/   # NodeType, EdgeType
├── repository/        # KnowledgeNodeRepository, KnowledgeEdgeRepository
└── service/           # KnowledgeGraphDomainService
-->

## External Dependencies

<!--
Modules/APIs that this module depends on. Fill in the contract for each.
-->

| Dependency | Type | Interface | Required Fields |
|------------|------|-----------|-----------------|
| e.g. User Domain | Domain Service | UserService.getUserById() | userId |
| e.g. LLM Gateway | REST API | POST /api/v1/llm/chat | { prompt, model } |

## Provided Capabilities

<!--
What this module provides to other modules.
-->

### Domain Services

<!--
List of domain services exposed by this module.
-->

| Service | Method | Input | Output | Description |
|---------|--------|-------|--------|-------------|
| e.g. KnowledgeGraphService | buildGraph() | { scope, depth } | GraphDTO | Build knowledge graph for given scope |

### REST APIs

<!--
List of REST API endpoints provided by this module.
Only list endpoints that other modules or frontend may call.
-->

| Method | Path | Auth | Input | Output | Description |
|--------|------|------|-------|--------|-------------|
| e.g. GET | /api/v1/kg/{id} | Yes | path: id | KnowledgeNodeDTO | Get knowledge node by ID |

### Domain Events

<!--
Events published by this module (for event-driven integration).
-->

| Event | Trigger | Payload | Consumers |
|-------|---------|---------|-----------|
| e.g. KnowledgeNodeCreated | Node creation | { nodeId, parentId } | e.g. Learning Domain |

## Key Data Models

<!--
Core entities and DTOs with key fields.
-->

### Entity: {EntityName}

| Field | Type | Description | Constraints |
|-------|------|-------------|-------------|
| id | Long | Primary key | @Id, auto-increment |
| ... | | | |

### DTO: {DtoName}

| Field | Type | Description | Source |
|-------|------|-------------|--------|
| id | Long | Entity ID | Entity.id |
| ... | | | |

## Cross-Module Interaction

<!--
Sequence diagram or description of how this module interacts with others.
-->

```
Example:
  Frontend → KnowledgeGraphController → KnowledgeGraphService
    → KnowledgeNodeRepository (JPA)
    → UserDomainService (validate ownership)
    → return KnowledgeNodeDTO
```

## Implementation Notes

<!--
Any specific implementation details that other modules should be aware of.
- Database table names
- Cache key patterns
- Rate limiting rules
- etc.
-->
