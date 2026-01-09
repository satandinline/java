# Cultural Resources Backend (Java Spring Boot)

这是公共文化资源系统的 Java Spring Boot 后端实现，与现有的 Python 后端并行存在。

## 项目结构

```
backend-java/
├── pom.xml                          # Maven 配置文件
├── README.md                        # 本文件
├── AIGC/                            # AIGC功能模块（Python）
│   ├── aigc_api_server.py          # AIGC API服务器
│   ├── auto_annotation.py          # 自动标注
│   ├── RAG.py                      # RAG检索增强生成
│   └── ...
└── src/
    └── main/
        ├── java/
        │   └── com/
        │       └── app/
        │           ├── CulturalResourcesApplication.java  # 主应用类
        │           ├── config/                            # 配置类
        │           │   └── WebConfig.java
        │           ├── controller/                        # REST 控制器
        │           │   ├── AuthController.java
        │           │   ├── CommentController.java
        │           │   ├── StatisticsController.java
        │           │   ├── UploadController.java
        │           │   ├── ResourceController.java
        │           │   ├── SearchController.java
        │           │   └── AnnotationTaskController.java
        │           ├── entity/                            # JPA 实体类
        │           │   ├── User.java
        │           │   ├── CulturalResource.java
        │           │   ├── CulturalResourceFromUser.java
        │           │   ├── UserComment.java
        │           │   ├── CommentReply.java
        │           │   ├── AnnotationTask.java
        │           │   └── UserBehaviorLog.java
        │           ├── repository/                        # JPA Repository
        │           │   ├── UserRepository.java
        │           │   ├── CulturalResourceRepository.java
        │           │   ├── CulturalResourceFromUserRepository.java
        │           │   ├── UserCommentRepository.java
        │           │   ├── CommentReplyRepository.java
        │           │   ├── AnnotationTaskRepository.java
        │           │   └── UserBehaviorLogRepository.java
        │           ├── service/                           # 业务逻辑层
        │           │   ├── AuthService.java
        │           │   ├── CommentService.java
        │           │   ├── StatisticsService.java
        │           │   ├── AnnotationTaskService.java
        │           │   └── UserLoggingService.java
        │           └── util/                              # 工具类
        │               └── PasswordUtil.java
        └── resources/
            └── application.yml                           # 应用配置文件
```

## 功能模块

### 1. 用户认证和管理 (AuthController, AuthService)
- 用户注册
- 用户登录
- 修改密码
- 修改昵称
- 修改个人签名
- 更换头像
- 安全问题管理
- 密码重置

### 2. 资源管理 (UploadController, ResourceController)
- 资源上传（文本/图像）
- 资源查询
- 首页资源列表（每页8条）
- 资源详情

### 3. 检索功能 (SearchController, ResourceController)
- 全文检索（`/api/search`）- 每页8条数据
- AI检索（`/api/ai_search`）- 每页8条数据

### 4. 评论系统 (CommentController, CommentService)
- 获取评论列表
- 创建评论
- 回复评论
- 点赞功能

### 5. 标注任务管理 (AnnotationTaskController, AnnotationTaskService)
- 获取标注任务列表（每页12条数据）
- 支持状态过滤
- 支持管理员/普通用户权限控制

### 6. 统计API (StatisticsController, StatisticsService)
- 访问人次统计
- AIGC 使用量统计
- 趋势数据

### 7. 通知系统 (NotificationController, NotificationService)
- 获取用户通知列表
- 标记通知为已读
- 标记所有通知为已读
- 评论点赞和回复时自动创建通知

### 8. 管理员功能 (AdminController, AdminService)
- 管理员面板统计（访问人次、AIGC使用量、趋势数据）
- 用户访问日志记录
- 用户管理
- 角色切换

### 9. 导出与下载 (ExportController, ExportService, DownloadController)
- 用户资源导出为Excel
- 批量导出用户资源
- 导出文件下载

### 10. 图片服务 (ImageController)
- 爬虫图片服务
- AIGC图片服务
- 用户上传图片服务

### 11. AIGC功能 (AIGCController, AIGCService)
- AIGC聊天（文本和图像生成）
- AIGC会话管理（创建、获取、删除、更新摘要）
- AIGC历史对话查询（支持用户消息和AI消息分离显示）
- 标题提取
- AIGC资源获取和保存
- 二次创作功能
- 会话历史从本地数据库读取（qa_sessions和qa_messages表）

### 12. 多模态搜索 (MultimodalSearchController)
- 文本+图片多模态搜索
- 转发请求到Python AIGC服务

### 13. 用户日志 (UserLoggingService)
- 用户行为日志记录
- 登录、注册、上传、搜索等行为记录

## 分页配置

- **检索结果**：每页 **8条** 数据（`/api/search`, `/api/ai_search`, `/api/home/resources`）
- **标注任务**：每页 **12条** 数据（`/api/annotation/tasks`）

## 数据库配置

在 `application.yml` 中配置数据库连接：

```yaml
spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/java_project?useUnicode=true&characterEncoding=utf8mb4&useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: your_password
```

## 运行说明

1. 确保已安装 Java 24 和 Maven 3.6+
2. 配置数据库连接（修改 `application.yml`）
3. 运行：
   ```bash
   mvn spring-boot:run
   ```
4. 服务将在 `http://localhost:7210` 启动

## 注意事项

- 本 Java 后端与 Python 后端功能完全一致，API端点相同
- Java 后端使用 Spring Boot + JPA/Hibernate 框架实现
- 所有功能模块均已实现，包括认证、资源管理、评论、标注任务、通知、管理员面板、导出下载等
- **AIGC功能**：
  - AIGC聊天功能通过调用Python服务实现，需要Python后端运行在 `http://localhost:7200`
  - AIGC历史对话从本地数据库（qa_sessions和qa_messages表）读取
  - 新建对话功能已优化，支持从请求头（X-User-Id）读取用户ID
  - AI回答显示区域自适应，最多占3/4屏幕（75vh高度，75%宽度）
  - AI消息和用户消息正确分离显示
- 多模态搜索功能会转发请求到Python AIGC服务
- 需要先初始化数据库才能正常使用

## API 接口

所有接口路径与 Python 版本保持一致，例如：
- `POST /api/auth/register` - 用户注册
- `POST /api/auth/login` - 用户登录
- `GET /api/comments?resource_id=xxx` - 获取评论
- `POST /api/comments` - 创建评论
- `GET /api/statistics?userId=xxx` - 获取统计数据
- `GET /api/search?q=xxx&page=1&page_size=8` - 全文检索（每页8条）
- `GET /api/ai_search?q=xxx&page=1&page_size=8` - AI检索（每页8条）
- `GET /api/annotation/tasks?user_id=xxx&page=1&page_size=12` - 获取标注任务（每页12条）
- `GET /api/notifications?user_id=xxx` - 获取通知列表
- `POST /api/export/user-resource` - 导出用户资源
- `GET /api/admin/dashboard/statistics?userId=xxx` - 管理员面板统计
- `POST /api/admin/log-access` - 记录访问日志
- `GET /api/admin/users?userId=xxx` - 获取用户列表（仅超级管理员）
- `POST /api/admin/auth/switch-role` - 切换用户角色（仅超级管理员）
- `POST /api/aigc/chat` - AIGC聊天
- `POST /api/aigc/sessions` - 创建AIGC会话
- `GET /api/aigc/sessions?user_id=xxx&page=1&page_size=20` - 获取AIGC会话列表
- `GET /api/aigc/sessions/{sessionId}/messages?page=1&page_size=20` - 获取AIGC会话消息
- `DELETE /api/aigc/sessions/{sessionId}` - 删除AIGC会话
- `POST /api/multimodal/search` - 多模态搜索