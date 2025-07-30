# 清水河畔BBS消息监控系统

一个基于Spring Boot的智能BBS消息监控系统，支持实时监控清水河畔论坛的回复消息，并提供Web界面进行配置管理和日志查看。

## ✨ 新功能特性

- 🚀 **Spring Boot架构**: 现代化的Web应用架构，易于部署和维护
- 🌐 **Web管理界面**: 直观的Web界面，支持实时配置管理和状态监控
- 📊 **实时日志显示**: WebSocket实时推送日志信息，无需刷新页面
- 🔔 **智能通知系统**: 支持推送通知开关，可选择性发送消息推送
- 📱 **响应式设计**: 基于Bootstrap 5的现代化UI，支持移动端访问
- 💾 **数据持久化**: 自动保存消息到JSON文件，支持历史数据加载
- 🔄 **启动优化**: 首次启动时自动加载历史消息，避免大量推送通知
- 🛡️ **错误恢复**: 智能处理文件损坏等异常情况，自动备份和恢复

## 🛠️ 技术栈

- **后端框架**: Spring Boot 3.2.0
- **模板引擎**: Thymeleaf
- **实时通信**: WebSocket (STOMP/SockJS)
- **HTTP客户端**: OkHttp
- **JSON处理**: Jackson
- **构建工具**: Maven
- **前端框架**: Bootstrap 5
- **图标库**: Bootstrap Icons

## 📋 系统要求

- Java 17 或更高版本
- Maven 3.6 或更高版本
- 网络连接（用于访问BBS API和推送服务）

## 🚀 快速开始

### 1. 克隆项目
```bash
git clone https://gitee.com/JackZhang144/UESTC-BBSNotificationMonitor.git
cd UESTC-BBS
```

### 2. 配置认证信息
编辑 `src/main/resources/application.yml` 文件，配置你的BBS认证信息。

**参考示例文件**: `src/main/resources/application-example.yml`

```yaml
bbs:
  cookies: "你的cookies字符串"
  auth-token: "你的认证token"
  notification-user-id: "你的推送服务用户ID"
  notification-api-template: "http://api.chuckfang.com/{user_id}/BBS消息通知/{author}回复了您的帖子\"{subject}\""
```

### 3. 获取认证信息
1. 登录 [清水河畔论坛](https://bbs.uestc.edu.cn)
2. 打开浏览器开发者工具 (F12)
3. 切换到 Network 标签页
4. 刷新页面或访问消息页面
5. 找到任意API请求，复制以下信息：
   - **Cookies**: 在请求头中找到 Cookie 字段的值
   - **Auth Token**: 在请求头中找到 Authorization 字段的值

### 4. 运行应用
```bash
mvn spring-boot:run
```

### 5. 访问Web界面
打开浏览器访问: http://localhost:8080

## 🌐 Web界面使用

### 主页面 (Dashboard)
- **系统状态**: 显示消息数量、日志条数、监控状态、推送通知状态
- **实时日志**: 实时显示系统运行日志，支持自动滚动
- **快速操作**: 快速访问消息列表、日志页面、配置管理

### 消息列表页面
- 显示所有监控到的BBS回复消息
- 支持搜索和筛选功能
- 按时间倒序排列

### 日志页面
- 查看完整的系统运行日志
- 支持日志清空功能
- 实时更新

### 配置管理页面
- **监控状态**: 开启/关闭消息监控
- **推送通知**: 开启/关闭消息推送功能
- **检查间隔**: 设置检查新消息的时间间隔（秒）
- **认证配置**: 管理BBS认证信息
- **通知配置**: 配置推送服务参数

## ⚙️ 配置文件说明

### application.yml 主要配置项

```yaml
bbs:
  cookies: "BBS认证cookies"
  auth-token: "BBS认证token"
  output-file: "new_messages.json"  # 消息保存文件
  notification-api-template: "推送API模板"
  notification-user-id: "推送服务用户ID"
  check-interval-seconds: 15  # 检查间隔（秒）
  enabled: true  # 是否启用监控
  notification-enabled: true  # 是否启用推送通知
```

## 🔧 高级配置

### 推送通知配置
系统支持多种推送服务，当前配置为ChuckFang推送服务：

- **API地址**: http://api.chuckfang.com
- **格式**: `http://api.chuckfang.com/{user_id}/{title}/{message}`
- **支持方式**: GET/POST请求

### 自定义推送服务
如需使用其他推送服务，可修改 `notification-api-template` 配置项。

## 📁 项目结构

```
UESTC-BBS/
├── src/main/java/com/jackzhang144/bbs/
│   ├── BbsMonitorApplication.java      # 主启动类
│   ├── config/
│   │   ├── AppConfig.java              # 配置属性类
│   │   └── WebSocketConfig.java        # WebSocket配置
│   ├── controller/
│   │   └── WebController.java          # Web控制器
│   ├── model/
│   │   └── BbsMessage.java             # 消息数据模型
│   └── service/
│       ├── BbsMonitorService.java      # 核心监控服务
│       ├── NotificationService.java    # 推送通知服务
│       └── LogService.java             # 日志管理服务
├── src/main/resources/
│   ├── application.yml                 # 配置文件
│   └── templates/                      # Thymeleaf模板
│       ├── index.html                  # 主页面
│       ├── messages.html               # 消息列表页面
│       ├── logs.html                   # 日志页面
│       └── config.html                 # 配置页面
├── pom.xml                             # Maven配置
└── README.md                           # 项目说明
```

## 🔍 功能特性详解

### 智能消息检测
- 基于时间戳 (`dateline`) 的新消息检测
- 使用内存缓存提高性能
- 支持多页历史消息加载

### 启动优化
- 首次启动时自动加载历史消息
- 避免启动时发送大量推送通知
- 智能处理文件损坏情况

### 实时监控
- 定时检查新消息（可配置间隔）
- WebSocket实时推送状态更新
- 支持动态开启/关闭监控

### 数据管理
- JSON格式保存消息数据
- 自动备份损坏的文件
- 支持历史数据恢复

## 🐛 故障排除

### 常见问题

1. **应用无法启动**
   - 检查Java版本是否为17+
   - 确认Maven依赖下载完整
   - 查看启动日志中的错误信息

2. **无法获取BBS消息**
   - 检查cookies和auth-token是否有效
   - 确认网络连接正常
   - 查看日志中的API请求状态

3. **推送通知失败**
   - 检查推送服务配置是否正确
   - 确认用户ID有效
   - 查看推送服务的响应日志

4. **Web界面无法访问**
   - 确认应用已正常启动
   - 检查端口8080是否被占用
   - 查看防火墙设置

### 日志查看
- 控制台日志：启动时显示
- Web界面日志：访问 http://localhost:8080/logs
- 实时日志：主页面实时显示

## 🤝 贡献指南

欢迎提交Issue和Pull Request！

1. Fork 本项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 打开 Pull Request

## 📄 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情

## ⚠️ 免责声明

- 本项目仅供学习和研究使用
- 请遵守清水河畔论坛的使用条款
- 请合理使用推送服务，避免频繁请求
- 作者不对使用本软件造成的任何问题负责

## 📞 联系方式

如有问题或建议，请通过以下方式联系：
- 提交 GitHub Issue
- 发送邮件至：[jackzhang144@163.com]

---

**注意**: 使用前请确保已获得相关服务的授权，并遵守相关使用条款。

