## ADDED Requirements

### Requirement: 创建部门

系统 SHALL 允许在学校下创建行政部门，支持树形层级结构。

#### Scenario: 创建根级部门
- **WHEN** 用户提交部门名称"教务处"、无上级部门
- **THEN** 系统创建部门，parent_id 为 NULL，department_path 为空，返回部门详情

#### Scenario: 创建子部门
- **WHEN** 用户提交部门名称"教务办公室"、上级部门ID为教务处ID
- **THEN** 系统创建部门，parent_id 为教务处ID，department_path 为教务处ID，返回部门详情

#### Scenario: 创建三级部门
- **WHEN** 用户提交部门名称"教学调度组"、上级部门ID为教务办公室ID
- **THEN** 系统创建部门，department_path 为"教务处ID_教务办公室ID"，返回部门详情

### Requirement: 更新部门

系统 SHALL 允许更新部门信息，包括名称、上级部门、排序、描述。

#### Scenario: 更新部门名称
- **WHEN** 用户将部门名称从"教务处"改为"教务管理处"
- **THEN** 系统更新部门名称，返回更新后的部门详情

#### Scenario: 更新上级部门
- **WHEN** 用户将部门的上级部门从A改为B
- **THEN** 系统更新 parent_id 为B的ID，department_path 相应更新，返回更新后的部门详情

#### Scenario: 防止自引用
- **WHEN** 用户将部门的上级部门设为自身
- **THEN** 系统返回错误，提示上级部门不能为自身

### Requirement: 删除部门

系统 SHALL 允许删除部门，采用逻辑删除。

#### Scenario: 删除无子部门的部门
- **WHEN** 用户删除一个无子部门的部门
- **THEN** 系统逻辑删除该部门，返回成功

#### Scenario: 删除有子部门的部门
- **WHEN** 用户删除一个有子部门的部门
- **THEN** 系统返回错误，提示存在子部门，需先删除子部门

### Requirement: 查询部门详情

系统 SHALL 允许按ID查询部门详情。

#### Scenario: 查询存在的部门
- **WHEN** 用户查询部门ID为有效ID
- **THEN** 系统返回部门详情（id, name, parent_id, department_path, school_id, sort_order, description）

#### Scenario: 查询不存在的部门
- **WHEN** 用户查询部门ID为不存在或已删除的ID
- **THEN** 系统返回错误，提示部门不存在

### Requirement: 查询部门树

系统 SHALL 返回学校下的部门树形结构。

#### Scenario: 查询学校部门树
- **WHEN** 用户查询学校ID为有效ID的部门列表
- **THEN** 系统返回该学校所有部门的树形结构，按 sort_order 排序

#### Scenario: 查询部门子树
- **WHEN** 用户查询指定部门ID的所有子部门
- **THEN** 系统通过 department_path 查询返回所有子部门列表