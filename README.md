# SIP即时通信系统

基于Kamailio的Java即时通信系统，实现音视频通话、多媒体消息和群聊功能。

## 项目结构

```
sipEx/
├── sip-common/          # 共享实体类和工具
├── sip-server/          # Spring Boot服务器端
├── sip-client/          # JavaFX客户端
├── db/                  # 数据库脚本
│   ├── schema.sql       # 数据库结构
│   └── init-data.sql    # 初始化数据
└── docs/                # 文档
```

## 技术栈

- **服务器端**: Spring Boot 2.7.18 + MyBatis + WebSocket + MySQL
- **客户端**: JavaFX 17 + JAIN-SIP 1.3 + OkHttp
- **SIP服务器**: Kamailio (Docker部署)
- **数据库**: MySQL 8.0

## 功能特性

### 已实现功能（90-100分档）

1. ✅ **基于SIP的音视频通话**
   - 点对点音频通话
   - 点对点视频通话
   - SDP协商和RTP媒体传输
   - 呼叫控制（接听、拒绝、挂断）

2. ✅ **多媒体消息**
   - 文字消息发送和接收
   - 图片上传和发送
   - 文件传输
   - 消息持久化

3. ✅ **群聊功能**
   - 群组创建和管理
   - 群消息收发
   - 群成员管理
   - 多方音视频通话（基础架构）

4. ✅ **后台管理与统计**
   - 用户数量统计
   - 消息数量统计
   - 今日通话时长统计
   - 用户管理
   - 群组管理
   - 通话记录查询

5. ✅ **与Linphone互通**
   - 标准SIP MESSAGE方法
   - 兼容RFC 3261规范
   - 支持与Linphone收发消息和通话

## 环境要求

- JDK 17+
- Maven 3.6+
- MySQL 8.0+
- Docker（用于Kamailio）

## 快速开始

### 1. 数据库初始化

```bash
# 登录MySQL
mysql -u root -p123456

# 执行数据库脚本
source db/schema.sql
source db/init-data.sql
```

### 2. 启动服务器

```bash
# 编译项目
mvn clean install

# 启动服务器
cd sip-server
mvn spring-boot:run
```

服务器将在端口8080启动。

### 3. 启动客户端

```bash
# 在新的终端窗口
cd sip-client
mvn javafx:run
```

### 4. 测试账号

系统已预置测试用户（密码均为：123456）：
- alice
- bob
- charlie
- david

## 配置说明

### Kamailio配置

在`sip-server/src/main/resources/application.yml`中配置Kamailio地址：

```yaml
kamailio:
  host: 172.22.189.160
  port: 5060
```

在`sip-client/src/main/java/com/sipex/client/config/ClientConfig.java`中配置：

```java
public static final String KAMAILIO_HOST = "172.22.189.160";
public static final int KAMAILIO_PORT = 5060;
```

### 数据库配置

在`sip-server/src/main/resources/application.yml`中：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/sipex
    username: root
    password: 123456
```

## 使用说明

### 登录

1. 启动客户端
2. 输入用户名和密码（测试账号见上方）
3. 点击"登录"按钮
4. 系统自动向Kamailio注册SIP账号

### 发送消息

1. 在左侧联系人列表选择联系人
2. 在消息输入框输入内容
3. 点击"发送"按钮
4. 消息通过SIP MESSAGE方法发送并保存到数据库

### 音视频通话

1. 选择联系人
2. 点击"语音通话"或"视频通话"按钮
3. 等待对方接听
4. 建立RTP媒体流传输

### 群聊

1. 切换到"群组"标签
2. 选择群组
3. 发送消息到群组
4. 所有群成员接收消息

### 管理后台

1. 点击"管理后台"按钮
2. 查看系统统计信息
3. 查看用户列表、群组列表、通话记录

## 与Linphone互通测试

### 配置Linphone

1. 下载并安装Linphone
2. 配置SIP账号：
   - 用户名：任意测试账号（如alice）
   - 域名：172.22.189.160
   - 传输：UDP

### 测试消息收发

1. 在本系统客户端登录账号alice
2. 在Linphone登录账号bob
3. 从本系统向bob发送消息
4. Linphone应能收到消息
5. 反向测试：从Linphone向alice发送消息

### 测试音视频通话

1. 从本系统发起对Linphone的呼叫
2. Linphone接听
3. 验证音视频流是否正常

## 项目亮点

1. **完整的SIP实现**: 使用JAIN-SIP库实现标准SIP协议，支持REGISTER、INVITE、MESSAGE、BYE等方法
2. **双通道架构**: SIP用于信令控制，WebSocket用于实时消息推送，HTTP用于文件传输
3. **音视频支持**: 实现SDP协商和RTP媒体传输框架
4. **数据持久化**: 所有消息、通话记录保存到MySQL数据库
5. **现代化UI**: 使用JavaFX构建美观的桌面客户端
6. **后台管理**: 完善的统计和管理功能
7. **互通性**: 与Linphone等标准SIP客户端互通

## 架构说明

### 信令流程

```
客户端A -> SIP REGISTER -> Kamailio (注册)
客户端A -> SIP MESSAGE -> Kamailio -> 客户端B (消息)
客户端A -> SIP INVITE -> Kamailio -> 客户端B (呼叫)
客户端B -> SIP 200 OK -> Kamailio -> 客户端A (应答)
客户端A -> RTP媒体流 <-> 客户端B (音视频传输)
```

### 消息流程

```
客户端 -> HTTP POST /api/messages -> 服务器 (保存消息)
服务器 -> WebSocket -> 目标客户端 (推送消息)
客户端 -> SIP MESSAGE -> Kamailio -> 目标客户端 (SIP消息)
```

## 注意事项

1. 确保Kamailio Docker容器正常运行
2. 确保MySQL服务启动并已执行初始化脚本
3. 确保端口8080（服务器）和5070+（客户端SIP）未被占用
4. 防火墙需开放相关端口
5. 音视频功能需要麦克风和摄像头权限

## 故障排除

### SIP注册失败
- 检查Kamailio是否运行
- 检查网络连接到172.22.189.160:5060
- 查看客户端日志文件siplog_*.txt

### 消息发送失败
- 检查服务器是否启动（端口8080）
- 检查数据库连接
- 查看服务器日志

### 音视频无法建立
- 检查RTP端口范围10000-20000是否开放
- 检查SDP协商是否成功
- 查看媒体流日志

## 开发团队

本项目为课程大作业，重点实现功能完整性和互通性。

## 许可证

本项目仅用于学习和研究目的。

