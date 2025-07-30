# 部署说明

## 快速部署

### 1. 环境准备
- Java 17+
- Maven 3.6+

### 2. 构建项目
```bash
mvn clean package
```

### 3. 运行应用
```bash
java -jar target/UESTC-BBS-1.0-SNAPSHOT.jar
```

## Docker 部署

### 1. 构建镜像
```bash
docker build -t uestc-bbs-monitor .
```

### 2. 运行容器
```bash
docker run -d -p 8080:8080 --name bbs-monitor uestc-bbs-monitor
```

## 生产环境配置

### 1. 修改端口
编辑 `application.yml`:
```yaml
server:
  port: 你的端口号
```

### 2. 配置日志
```yaml
logging:
  file:
    name: logs/bbs-monitor.log
  level:
    com.jackzhang144.bbs: INFO
```

### 3. 系统服务配置
创建 systemd 服务文件 `/etc/systemd/system/bbs-monitor.service`:

```ini
[Unit]
Description=BBS Monitor Service
After=network.target

[Service]
Type=simple
User=bbs-monitor
ExecStart=/usr/bin/java -jar /opt/bbs-monitor/UESTC-BBS-1.0-SNAPSHOT.jar
WorkingDirectory=/opt/bbs-monitor
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

启用服务:
```bash
sudo systemctl enable bbs-monitor
sudo systemctl start bbs-monitor
```

## 安全注意事项

1. **敏感信息保护**
   - 不要在代码中硬编码认证信息
   - 使用环境变量或外部配置文件
   - 定期更新认证信息

2. **网络安全**
   - 配置防火墙规则
   - 使用HTTPS（生产环境）
   - 限制访问IP

3. **数据备份**
   - 定期备份 `new_messages.json` 文件
   - 监控磁盘空间使用

## 监控和维护

### 1. 健康检查
访问 `http://localhost:8080` 查看系统状态

### 2. 日志监控
```bash
tail -f logs/bbs-monitor.log
```

### 3. 性能监控
- 监控内存使用
- 检查磁盘空间
- 观察网络连接

## 故障排除

### 1. 应用无法启动
- 检查Java版本
- 确认端口未被占用
- 查看启动日志

### 2. 无法获取BBS消息
- 验证认证信息有效性
- 检查网络连接
- 查看API请求日志

### 3. 推送通知失败
- 检查推送服务配置
- 验证用户ID有效性
- 查看推送响应日志 