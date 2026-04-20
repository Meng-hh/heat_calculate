## ADDED Requirements

### Requirement: 接受食物图片上传
系统 SHALL 通过 `POST /api/v1/calories/analyze` 接口接收 multipart/form-data 格式的图片文件，字段名为 `image`，并支持可选的文字备注字段 `note`。

#### Scenario: 上传合法图片
- **WHEN** 用户上传一张格式为 JPG、PNG 或 WEBP、大小不超过 10MB 的图片
- **THEN** 系统 SHALL 接受该图片并进入识别流程

#### Scenario: 上传不支持的格式
- **WHEN** 用户上传 GIF、BMP 或其他非支持格式的文件
- **THEN** 系统 SHALL 返回 HTTP 400，错误信息说明支持的格式

#### Scenario: 上传超大文件
- **WHEN** 用户上传大小超过 10MB 的图片
- **THEN** 系统 SHALL 返回 HTTP 400，错误信息说明文件大小限制

#### Scenario: 未上传图片
- **WHEN** 请求中缺少 `image` 字段
- **THEN** 系统 SHALL 返回 HTTP 400，提示图片为必填项

### Requirement: 图片预处理
系统 SHALL 在将图片传递给大模型前，对图片进行内存压缩处理，确保传输体积合理，不将图片持久化到磁盘。

#### Scenario: 图片内存处理
- **WHEN** 合法图片上传成功
- **THEN** 系统 SHALL 在内存中完成 Base64 编码，不写入任何临时文件
