## ADDED Requirements

### Requirement: 图片上传界面
系统 SHALL 提供图片上传界面，支持用户通过拍照或相册选择图片，并进行前端格式和大小校验。

#### Scenario: 用户点击上传区域
- **WHEN** 用户点击上传区域
- **THEN** 系统 SHALL 唤起文件选择器，支持拍照和相册选择

#### Scenario: 选择合法图片
- **WHEN** 用户选择 JPG、PNG 或 WEBP 格式且大小不超过 10MB 的图片
- **THEN** 系统 SHALL 显示图片预览，并启用"开始分析"按钮

#### Scenario: 选择不支持的格式
- **WHEN** 用户选择 GIF、BMP 等非支持格式的图片
- **THEN** 系统 SHALL 显示错误提示"仅支持 JPG、PNG、WEBP 格式"

#### Scenario: 选择过大的图片
- **WHEN** 用户选择超过 10MB 的图片
- **THEN** 系统 SHALL 显示错误提示"图片大小不能超过 10MB"

#### Scenario: 添加备注信息
- **WHEN** 用户在备注输入框输入文字
- **THEN** 系统 SHALL 保存备注信息，随图片一起提交

### Requirement: 前端图片压缩
系统 SHALL 在上传前对过大的图片进行前端压缩，以优化上传速度。

#### Scenario: 图片超过 2MB
- **WHEN** 用户选择的图片超过 2MB
- **THEN** 系统 SHALL 自动压缩图片至 2MB 以下，保持可接受的画质

#### Scenario: 压缩失败
- **WHEN** 图片压缩过程中发生错误
- **THEN** 系统 SHALL 使用原图上传，并记录警告日志
