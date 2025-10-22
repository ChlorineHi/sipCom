# Linphone 互通测试指南

## 📱 测试目标
验证我们的SIP客户端能够与标准SIP客户端（Linphone）正常通信。

---

## 🔧 准备工作

### 1. 下载并安装 Linphone
- **官网**：https://www.linphone.org/
- **Windows版本**：https://www.linphone.org/releases/windows/app/
- **推荐版本**：Linphone Desktop 5.x

### 2. 配置 Linphone

#### 步骤1：打开 Linphone，点击"设置" → "SIP账户"

#### 步骤2：添加新账户
- **用户名**：`charlie`（或任意未使用的用户名）
- **密码**：`123456`
- **域**：`172.22.189.160`
- **传输协议**：UDP
- **端口**：5060

#### 步骤3：高级设置（可选）
- **启用STUN**：关闭
- **ICE**：关闭
- **视频编解码器**：启用 JPEG（如果可用）
- **音频编解码器**：启用 PCMU (G.711 μ-law)

#### 步骤4：保存并注册
- 确保Linphone显示"已注册"状态

---

## 🧪 测试场景

### 场景 1：Linphone 呼叫 我们的客户端

#### 步骤：
1. **启动我们的客户端**（双击 `start-client.bat`）
   - 登录用户：`alice` / `123456`
   - 等待显示"注册成功"

2. **在 Linphone 中拨号**
   - 输入：`alice@172.22.189.160`
   - 点击"呼叫"

3. **在我们的客户端接听**
   - 会弹出："charlie 正在呼叫您"
   - 点击"确定"接听

4. **验证音频**
   - Linphone 说话 → 我们的客户端扬声器应有声音
   - 我们的客户端说话 → Linphone 应收到音频

#### 预期结果：
```
✅ Linphone 显示"通话中"
✅ 我们的客户端控制台显示：
   收到请求: INVITE
   ✅ RTP音频接收器已启动
   ✅ RTP音频发送器已启动
✅ 双向音频正常
```

---

### 场景 2：我们的客户端 呼叫 Linphone

#### 步骤：
1. **确保 Linphone 已注册**
   - 用户名：`charlie`
   - 显示"在线"状态

2. **在我们的数据库中添加 charlie 用户**
   ```sql
   USE sipex;
   INSERT INTO users (username, password, nickname, email, status) 
   VALUES ('charlie', '$2a$10$abcdefg...', 'Charlie', 'charlie@test.com', 'ONLINE');
   ```
   或在客户端注册页面注册 charlie 用户

3. **在我们的客户端中呼叫**
   - 选择联系人"charlie"
   - 点击"语音通话"

4. **在 Linphone 中接听**
   - Linphone 会弹出来电通知
   - 点击"接听"

5. **验证音频**
   - 双向音频测试

#### 预期结果：
```
✅ 我们的客户端显示"对方响铃中..."
✅ Linphone 显示来电通知
✅ 接听后双向音频正常
```

---

### 场景 3：视频通话测试

#### Linphone 配置：
1. 进入"设置" → "视频"
2. 启用视频通话
3. 选择摄像头设备

#### 测试步骤：
1. **我们的客户端呼叫 Linphone（视频通话）**
   - 点击"视频通话"按钮
   - Linphone 接听

2. **验证视频**
   - 我们的客户端：显示 Linphone 的摄像头画面
   - Linphone：显示我们客户端的摄像头/屏幕画面

3. **测试视频源切换**
   - 在我们的客户端点击"切换到屏幕"
   - Linphone 应该看到屏幕共享画面
   - 再点击"切换到摄像头"
   - Linphone 应该看到摄像头画面

#### 预期结果：
```
✅ 双向视频传输正常
✅ 视频源切换成功
✅ Linphone 能正常显示我们的视频
✅ 我们的客户端能显示 Linphone 的视频
```

---

### 场景 4：SIP消息互通

#### 测试步骤：
1. **Linphone 发送消息**
   - 在 Linphone 聊天窗口输入：`alice@172.22.189.160`
   - 发送消息："Hello from Linphone"

2. **验证接收**
   - 我们的客户端应在消息列表显示该消息

3. **我们的客户端回复**
   - 在聊天框输入："Hello from SipEx"
   - 点击"发送"

4. **验证 Linphone 接收**
   - Linphone 应显示收到的消息

#### 预期结果：
```
✅ SIP MESSAGE 双向传输正常
✅ 消息内容完整无误
```

---

## 🐛 常见问题

### 问题1：Linphone无法注册
**解决方案：**
- 检查 Kamailio 是否运行：`docker ps | findstr kamailio`
- 确认IP地址正确：`172.22.189.160`
- 确认防火墙未阻止端口5060

### 问题2：能呼叫但无音频
**解决方案：**
- 检查 RTP 端口范围是否开放（10000-20000）
- 查看控制台是否有"RTP音频接收器已启动"
- 确认麦克风和扬声器权限已授予

### 问题3：视频无法显示
**解决方案：**
- Linphone 设置中启用 JPEG 编解码器
- 确认摄像头权限已授予
- 查看控制台错误日志

### 问题4：Linphone提示"不可达"
**解决方案：**
- 确认对方已在 Kamailio 注册
- 使用完整 SIP URI：`user@172.22.189.160`
- 检查 Kamailio 日志：`docker logs kamailio --tail 50`

---

## 📊 测试结果记录表

| 测试项 | 预期结果 | 实际结果 | 备注 |
|--------|---------|---------|------|
| Linphone → 我们的客户端（语音） | ✅ | | |
| 我们的客户端 → Linphone（语音） | ✅ | | |
| Linphone → 我们的客户端（视频） | ✅ | | |
| 我们的客户端 → Linphone（视频） | ✅ | | |
| SIP消息互通 | ✅ | | |
| 挂断功能 | ✅ | | |
| 视频源切换 | ✅ | | |

---

## 🎯 成功标准

- ✅ **SIP注册**：双方都能成功注册到 Kamailio
- ✅ **呼叫建立**：能够正常发起和接听呼叫
- ✅ **音频传输**：双向音频清晰可听
- ✅ **视频传输**：双向视频可见（如果支持）
- ✅ **消息传输**：SIP MESSAGE 正常收发
- ✅ **呼叫终止**：挂断功能正常工作

---

## 📞 联系方式

如有问题，请查看控制台日志或 Kamailio 日志：
```bash
# 查看我们的客户端日志
# PowerShell 窗口会实时显示

# 查看 Kamailio 日志
docker logs kamailio --tail 100

# 查看 Kamailio 实时日志
docker logs kamailio -f
```

