## ADDED Requirements

### Requirement: 学科判定先于答疑决策
系统 SHALL 在每次新题（拍题建会话 / 换题）进入答疑流程时、Python decide 之前，先判定题目学科（调用学科无关的 `subject-classify`，支持文本和图片）。当学科为 `math` 时 SHALL 继续走数学答疑；学科非 `math` 时 SHALL 跳过——不建/不续会话、不落题目记录、不更新掌握度、不写错误事件，并返回「仅支持数学」提示。

#### Scenario: 拍题物理题跳过
- **WHEN** 学生发起会话上传一道物理题（文字或图片），subject-classify 返回 `subject=physics`
- **THEN** 系统不建会话、不调用数学 decide/generate，返回「仅支持数学」提示

#### Scenario: 换题时出现非数学题
- **WHEN** 数学会话中学生上传一道化学题图片，subject-classify 返回 `subject=chemistry`
- **THEN** 该新题被跳过（不结算、不记录），返回「仅支持数学」提示，原数学会话不受影响

#### Scenario: 数学题正常进入
- **WHEN** subject-classify 返回 `subject=math`（文本或图片题）
- **THEN** 系统建/续会话（subject=math）、调用数学 decide→generate，题目/掌握度正常落库

### Requirement: 学科分类器支持文本和图片
系统 SHALL 的学科分类端点同时接受纯文本题目与图片题目（或图文混合），模型与 decide / question_understand 统一（`doubao-seed-2-0-mini-260428`，temp 0.3）。

#### Scenario: 文本物理题分类
- **WHEN** 学生发纯文字物理题（"自由落体运动的问题…"）
- **THEN** subject-classify 返回 `subject=physics`

#### Scenario: 图片题目分类
- **WHEN** 学生上传题目图片（受力分析图等）
- **THEN** subject-classify 走多模态（图+文）返回学科

### Requirement: 学科分类失败降级放行
当 subject-classify 异常/超时/返回空时，系统 SHALL 按 `math` 放行，不阻断答疑流程（宁可漏拦非数学题，不误拦数学题）。

#### Scenario: 分类器超时
- **WHEN** subject-classify 调用超时或返回空结果
- **THEN** 系统按 math 放行，走正常数学答疑流程

### Requirement: 学科分类幂等且无副作用
同一道非数学题被重复发送，系统 SHALL 每次都跳过且不产生任何记录。

#### Scenario: 重复发送物理题
- **WHEN** 同一道物理题重复上传
- **THEN** 每次均跳过，无题目记录、无掌握度更新、无会话建立
