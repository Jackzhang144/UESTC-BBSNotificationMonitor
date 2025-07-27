### 🤖 Assistant



# 清水河畔消息监控通知系统

## 项目简介

这是一个用于监控清水河畔论坛回复消息的 Java 应用程序，当有新回复时会发送通知。主要功能包括：

- 定期检查 BBS 论坛的新回复
- 首次运行会加载所有历史消息
- 检测到新回复时发送通知
- 将消息记录保存到 JSON 文件
- 可配置的检查间隔和通知方式

## 功能特点

✅ 基于时间戳的精确消息检测  
✅ 支持多回复帖子识别  
✅ 可配置的通知 API  
✅ 消息持久化存储  
✅ 详细的运行日志  

## 快速开始

### 前置要求

- Java 11 或更高版本
- Maven

### 安装步骤

1. 克隆仓库：
   ```bash
   git clone https://github.com/your-repo/bbs-notification-monitor.git
   cd bbs-notification-monitor
   ```

2. 配置应用程序：
   ```bash
   cp config.properties.example config.properties
   # 编辑 config.properties 文件，填入你的认证信息
   ```

3. 构建项目：
   ```bash
   mvn clean package
   ```

4. 运行程序：
   ```bash
   java -jar target/bbs-notification-monitor.jar
   ```

## 配置文件说明

编辑 `config.properties` 文件进行配置：

```properties
# BBS 认证配置
bbs.cookies=你的cookies
bbs.auth_token=你的认证token

# 通知配置
notification.api_template=http://api.example.com/{user_id}/您在“{subject}”帖子中收到“{author}”的回复
notification.user_id=你的用户ID

# 应用配置
output.file=new_messages.json  # 消息存储文件
check.interval.seconds=15     # 检查间隔(秒)
```

## 使用说明

程序启动后会持续运行，按配置的间隔时间检查新消息：

- 首次运行会加载所有历史消息
- 后续运行只检测新消息
- 发现新消息时会打印日志并发送通知
- 所有消息会保存到指定的 JSON 文件

### 控制台输出示例

```
⏳ 启动消息监控（每15秒检查一次）...
============================================================

🕒 检查时间: 2023-05-15 14:30:45
🔄 本次未发现新消息

🕒 检查时间: 2023-05-15 14:31:00
📨 发现 2 条新回复：
  - 在「测试帖子」帖子中收到新用户A的回复
  ✉️ 通知内容：您在「测试帖子」帖子中收到「用户A」的回复
    (API状态码: 200)
  - 在「求助问题」帖子中收到新用户B的回复
  ✉️ 通知内容：您在「求助问题」帖子中收到「用户B」的回复
    (API状态码: 200)
💾 已保存2条新消息到文件
```

## 项目结构

```
bbs-notification-monitor/
├── src/
│   └── main/
│       ├── java/
│       │   └── BBSNotificationMonitor.java  # 主程序
│       └── resources/
│           └── config.properties           # 配置文件
├── target/                                 # 构建输出
├── config.properties.example               # 配置示例文件
├── pom.xml                                 # Maven配置
└── README.md                               # 本文件
```

## 常见问题

**Q: 如何修改检查间隔？**  
A: 编辑 `config.properties` 中的 `check.interval.seconds` 值

**Q: 消息存储在哪里？**  
A: 默认存储在程序运行目录下的 `new_messages.json` 文件

**Q: 首次运行为什么加载所有历史消息？**  
A: 这是为了初始化消息数据库，避免重复通知

**Q: 如何停止程序？**  
A: 在控制台按 `Ctrl+C` 终止程序

## 参与贡献

欢迎提交 Issue 或 Pull Request

1. Fork 项目
2. 创建分支 (`git checkout -b feature/your-feature`)
3. 提交更改 (`git commit -am 'Add some feature'`)
4. 推送到分支 (`git push origin feature/your-feature`)
5. 创建 Pull Request

## 许可证

MIT License

