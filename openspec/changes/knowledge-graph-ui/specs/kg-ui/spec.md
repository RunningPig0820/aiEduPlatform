## ADDED Requirements

### Requirement: Knowledge point navigation page

The frontend SHALL display a knowledge point navigation page allowing users to browse textbooks, chapters, sections, and knowledge points in a tree view.

#### Scenario: Load navigation page
- **WHEN** user navigates to `/kg/navigation`
- **THEN** the page displays a subject filter (default: math), a phase filter (primary/middle/high), and a textbook tree panel

#### Scenario: Expand textbook in tree
- **WHEN** user clicks to expand a textbook node
- **THEN** the tree loads chapters from the API, and each chapter node shows the knowledge point count

#### Scenario: Click knowledge point in tree
- **WHEN** user clicks a knowledge point node in the tree
- **THEN** the right panel displays the knowledge point details including label, difficulty, importance, cognitive level, and matched concept info

### Requirement: Grade knowledge system page

The frontend SHALL display a grade-level knowledge system overview page.

#### Scenario: Load grade knowledge system page
- **WHEN** user navigates to `/kg/system/一年级`
- **THEN** the page displays a grade selector, statistics cards (total KPs, difficulty distribution), and the knowledge system tree grouped by textbook

#### Scenario: Switch grade
- **WHEN** user selects a different grade from the selector
- **THEN** the page reloads the knowledge system and statistics for the selected grade

#### Scenario: Group by topic
- **WHEN** user toggles the "按专题分组" option
- **THEN** the knowledge system reorganizes to group knowledge points by chapter topic instead of textbook

### Requirement: Responsive layout

The frontend layout SHALL adapt to different screen sizes.

#### Scenario: Desktop layout
- **WHEN** viewport width >= 1024px
- **THEN** the navigation page shows tree panel and details panel side-by-side

#### Scenario: Mobile layout
- **WHEN** viewport width < 768px
- **THEN** the page shows only one panel at a time with tab navigation between Tree and Details
