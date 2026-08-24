# user-id-card-encryption Specification

## Purpose
用户身份证号加密存储能力：`t_user` 表新增 `id_card` 列存储 AES-256 加密密文；`ai-edu-common` 模块提供 `EncryptUtil` 工具类（AES 加解密 + 身份证脱敏），密钥从配置项 `app.encrypt.aes-key` 加载并支持环境变量覆盖。

## Requirements

### Requirement: t_user 表支持身份证号字段

系统 SHALL 在 `t_user` 表新增 `id_card` 列，存储经 AES 加密的身份证号密文。

#### Scenario: id_card 字段加密存储
- **WHEN** 系统写入学生身份证号到 t_user 表
- **THEN** 存储的值为 AES-256 加密后的密文，非明文

#### Scenario: id_card 字段可为空
- **WHEN** 系统创建非学生角色用户（教师、家长等）
- **THEN** id_card 字段为空，不影响其他角色正常使用

### Requirement: AES 加解密工具

系统 SHALL 提供 `EncryptUtil` 工具类，位于 `ai-edu-common` 模块，支持 AES 对称加密与解密，提供身份证脱敏方法。

#### Scenario: 加密字符串
- **WHEN** 调用 `EncryptUtil.encrypt("110101200001011234")`
- **THEN** 返回 Base64 编码的 AES 密文字符串

#### Scenario: 解密字符串
- **WHEN** 调用 `EncryptUtil.decrypt(encryptedString)`
- **THEN** 返回原始明文字符串

#### Scenario: 身份证脱敏
- **WHEN** 调用 `EncryptUtil.maskIdCard("110101200001011234")`
- **THEN** 返回 "110101****011234"（保留前 6 位和后 6 位，中间用 * 替换）

#### Scenario: 加密密钥配置化
- **WHEN** 系统启动时加载 AES 密钥
- **THEN** 密钥从 `application.yml` 的 `app.encrypt.aes-key` 配置项读取，支持环境变量覆盖
