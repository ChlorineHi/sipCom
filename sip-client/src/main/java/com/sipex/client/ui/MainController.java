package com.sipex.client.ui;

import com.google.gson.reflect.TypeToken;
import com.sipex.client.config.ClientConfig;
import com.sipex.client.media.MediaManager;
import com.sipex.client.service.HttpClientService;
import com.sipex.client.service.MessageService;
import com.sipex.client.util.GsonUtil;
import com.sipex.client.sip.SipCallListener;
import com.sipex.client.sip.SipManager;
import com.sipex.client.sip.SipMessageListener;
import com.sipex.common.dto.ApiResponse;
import com.sipex.common.dto.LoginRequest;
import com.sipex.common.dto.MessageDTO;
import com.sipex.common.entity.Group;
import com.sipex.common.entity.Message;
import com.sipex.common.entity.User;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import javax.sip.ServerTransaction;
import javax.sip.message.Request;
import javax.sip.message.Response;
import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainController implements SipCallListener, SipMessageListener {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Label statusLabel;
    @FXML private Label currentUserLabel;
    @FXML private Label sipStatusLabel;
    @FXML private Button conferenceButton;

    @FXML private ListView<User> contactListView;
    @FXML private ListView<Group> groupListView;
    @FXML private ListView<String> messageListView;
    @FXML private Label chatTitleLabel;
    @FXML private TextArea messageInput;
    @FXML private Button sendButton;
    @FXML private Label contactInfoLabel;
    @FXML private Button hangupButton;
    @FXML private Button switchVideoSourceButton;
    @FXML private javafx.scene.image.ImageView remoteVideoView;
    
    private boolean isUsingWebcam = true; // 当前是否使用摄像头

    private SipManager sipManager;
    private MediaManager mediaManager;
    private User currentUser;
    private User currentContact;
    private Group currentGroup;
    private String jwtToken; // JWT token

    private ServerTransaction currentCallTransaction;
    private Request currentCallRequest;
    private boolean inCall = false; // 通话状态标志
    private javax.sip.Dialog currentDialog; // 当前通话的Dialog

    @FXML
    public void initialize() {
        // 初始化SIP和媒体管理器
        mediaManager = new MediaManager();
        sipManager = new SipManager(this, this);
        
        // 设置视频显示控件
        if (remoteVideoView != null) {
            mediaManager.setRemoteVideoView(remoteVideoView);
        }

        // 设置联系人列表显示
        contactListView.setCellFactory(param -> new ListCell<User>() {
            @Override
            protected void updateItem(User user, boolean empty) {
                super.updateItem(user, empty);
                if (empty || user == null) {
                    setText(null);
                } else {
                    setText(user.getUsername() + " [" + user.getStatus() + "]");
                }
            }
        });

        // 设置群组列表显示
        groupListView.setCellFactory(param -> new ListCell<Group>() {
            @Override
            protected void updateItem(Group group, boolean empty) {
                super.updateItem(group, empty);
                if (empty || group == null) {
                    setText(null);
                } else {
                    setText(group.getGroupName());
                }
            }
        });
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            showAlert("错误", "请输入用户名和密码");
            return;
        }

        try {
            // 登录到服务器
            LoginRequest loginRequest = new LoginRequest(username, password);
            var type = new TypeToken<ApiResponse<Map<String, Object>>>(){}.getType();
            ApiResponse<Map<String, Object>> response = HttpClientService.post("/api/auth/login", loginRequest, type);

            if (response.getCode() == 200) {
                Map<String, Object> data = response.getData();
                // 使用Gson解析User对象
                currentUser = GsonUtil.getGson().fromJson(
                    GsonUtil.getGson().toJson(data.get("user")), 
                    User.class
                );
                
                // 保存JWT token
                if (data.containsKey("token")) {
                    jwtToken = (String) data.get("token");
                }

                currentUserLabel.setText("当前用户: " + currentUser.getUsername());
                statusLabel.setText("已连接");

                // 初始化并注册SIP
                sipManager.initialize(username, password);
                sipManager.register();
                sipStatusLabel.setText("已注册");

                // 加载联系人和群组
                loadContacts();
                loadGroups();

                // 禁用登录表单
                usernameField.setDisable(true);
                passwordField.setDisable(true);
                loginButton.setDisable(true);
                
                // 显示群聊会议按钮
                if (conferenceButton != null) {
                    conferenceButton.setVisible(true);
                }

                showAlert("成功", "登录成功！");
            } else {
                showAlert("错误", response.getMessage());
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("错误", "登录失败: " + e.getMessage());
        }
    }

    private void loadContacts() {
        try {
            var type = new TypeToken<ApiResponse<List<User>>>(){}.getType();
            ApiResponse<List<User>> response = HttpClientService.get(
                "/api/users/" + currentUser.getId() + "/friends", 
                type
            );
            if (response.getCode() == 200 && response.getData() != null) {
                Platform.runLater(() -> {
                    contactListView.getItems().clear();
                    contactListView.getItems().addAll(response.getData());
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadGroups() {
        try {
            var type = new TypeToken<ApiResponse<List<Group>>>(){}.getType();
            ApiResponse<List<Group>> response = HttpClientService.get(
                "/api/groups/user/" + currentUser.getId(), 
                type
            );
            if (response.getCode() == 200 && response.getData() != null) {
                Platform.runLater(() -> {
                    groupListView.getItems().clear();
                    groupListView.getItems().addAll(response.getData());
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleContactClick() {
        currentContact = contactListView.getSelectionModel().getSelectedItem();
        currentGroup = null;
        if (currentContact != null) {
            chatTitleLabel.setText("与 " + currentContact.getUsername() + " 的聊天");
            contactInfoLabel.setText(
                "用户名: " + currentContact.getUsername() + "\n" +
                "SIP URI: " + currentContact.getSipUri() + "\n" +
                "状态: " + currentContact.getStatus()
            );
            loadChatHistory(currentContact.getUsername());
        }
    }

    @FXML
    private void handleGroupClick() {
        currentGroup = groupListView.getSelectionModel().getSelectedItem();
        currentContact = null;
        if (currentGroup != null) {
            chatTitleLabel.setText("群聊: " + currentGroup.getGroupName());
            contactInfoLabel.setText(
                "群名称: " + currentGroup.getGroupName() + "\n" +
                "描述: " + currentGroup.getDescription()
            );
            // TODO: 加载群聊消息
        }
    }

    private void loadChatHistory(String contactUsername) {
        try {
            List<Message> messages = MessageService.getChatHistory(
                currentUser.getUsername(), 
                contactUsername, 
                50
            );
            Platform.runLater(() -> {
                messageListView.getItems().clear();
                for (Message msg : messages) {
                    String displayText = formatMessage(msg);
                    messageListView.getItems().add(0, displayText);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String formatMessage(Message msg) {
        String time = msg.getCreatedAt().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        String prefix = msg.getFromUser().equals(currentUser.getUsername()) ? "[我]" : "[" + msg.getFromUser() + "]";
        return time + " " + prefix + ": " + msg.getContent();
    }

    @FXML
    private void handleSendMessage() {
        String content = messageInput.getText().trim();
        if (content.isEmpty()) {
            return;
        }

        if (currentContact == null && currentGroup == null) {
            showAlert("提示", "请先选择联系人或群组");
            return;
        }

        try {
            String toUser = currentContact != null ? currentContact.getUsername() : currentGroup.getId().toString();
            boolean isGroup = currentGroup != null;

            // 通过SIP发送消息
            if (currentContact != null) {
                sipManager.sendMessage(currentContact.getUsername(), content);
            }

            // 同时保存到服务器
            MessageDTO messageDTO = new MessageDTO();
            messageDTO.setFromUser(currentUser.getUsername());
            messageDTO.setToUser(toUser);
            messageDTO.setContent(content);
            messageDTO.setType("TEXT");
            messageDTO.setIsGroup(isGroup);

            MessageService.sendMessage(messageDTO);

            // 显示在界面上
            String displayText = formatMessage(new Message(
                null, currentUser.getUsername(), toUser, content, "TEXT", null, isGroup, false, java.time.LocalDateTime.now()
            ));
            messageListView.getItems().add(displayText);

            messageInput.clear();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("错误", "发送消息失败: " + e.getMessage());
        }
    }

    @FXML
    private void handleSendImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("选择图片");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("图片文件", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );
        File file = fileChooser.showOpenDialog(null);
        if (file != null) {
            uploadAndSendFile(file, "IMAGE");
        }
    }

    @FXML
    private void handleSendFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("选择文件");
        File file = fileChooser.showOpenDialog(null);
        if (file != null) {
            uploadAndSendFile(file, "FILE");
        }
    }

    private void uploadAndSendFile(File file, String type) {
        try {
            // 上传文件到服务器
            ApiResponse<Map<String, String>> uploadResponse = HttpClientService.uploadFile(file);
            if (uploadResponse.getCode() == 200) {
                // 获取文件URL并发送消息
                String fileUrl = uploadResponse.getData().get("url");
                
                String toUser = currentContact != null ? currentContact.getUsername() : currentGroup.getId().toString();
                boolean isGroup = currentGroup != null;

                MessageDTO messageDTO = new MessageDTO();
                messageDTO.setFromUser(currentUser.getUsername());
                messageDTO.setToUser(toUser);
                messageDTO.setContent("[" + type + "] " + file.getName());
                messageDTO.setType(type);
                messageDTO.setFileUrl(fileUrl);
                messageDTO.setIsGroup(isGroup);

                MessageService.sendMessage(messageDTO);

                showAlert("成功", "文件发送成功");
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("错误", "文件发送失败: " + e.getMessage());
        }
    }

    @FXML
    private void handleAudioCall() {
        if (currentContact == null) {
            showAlert("提示", "请先选择联系人");
            return;
        }

        try {
            // 先停止之前的媒体流
            mediaManager.stopStreams();
            
            String sdp = mediaManager.createSdpOffer(false);
            sipManager.makeCall(currentContact.getUsername(), sdp);
            showAlert("呼叫", "正在呼叫 " + currentContact.getUsername() + "...");
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("错误", "发起呼叫失败: " + e.getMessage());
        }
    }

    @FXML
    private void handleVideoCall() {
        if (currentContact == null) {
            showAlert("提示", "请先选择联系人");
            return;
        }

        try {
            // 先停止之前的媒体流
            mediaManager.stopStreams();
            
            String sdp = mediaManager.createSdpOffer(true);
            sipManager.makeCall(currentContact.getUsername(), sdp);
            showAlert("呼叫", "正在视频呼叫 " + currentContact.getUsername() + "...");
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("错误", "发起视频呼叫失败: " + e.getMessage());
        }
    }

    @FXML
    private void handleSwitchVideoSource() {
        // 切换视频源
        isUsingWebcam = !isUsingWebcam;
        
        if (isUsingWebcam) {
            mediaManager.setVideoSource(com.sipex.client.media.RtpVideoSender.VideoSource.WEBCAM);
            switchVideoSourceButton.setText("切换到屏幕");
        } else {
            mediaManager.setVideoSource(com.sipex.client.media.RtpVideoSender.VideoSource.SCREEN);
            switchVideoSourceButton.setText("切换到摄像头");
        }
    }

    @FXML
    private void handleHangup() {
        if (!inCall) {
            showAlert("提示", "当前没有通话");
            return;
        }

        try {
            // 发送BYE请求挂断
            sipManager.terminateCall();
            
            // 停止媒体流
            mediaManager.stopStreams();
            
            // 更新UI
            inCall = false;
            hangupButton.setVisible(false);
            switchVideoSourceButton.setVisible(false);
            remoteVideoView.setVisible(false);
            currentDialog = null;
            
            showAlert("通话", "已挂断");
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("错误", "挂断失败: " + e.getMessage());
        }
    }

    @FXML
    private void handleOpenAdmin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/admin.fxml"));
            VBox root = loader.load();
            
            Stage stage = new Stage();
            stage.setTitle("管理后台");
            stage.setScene(new Scene(root, 800, 600));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("错误", "打开管理后台失败: " + e.getMessage());
        }
    }

    // SipCallListener接口实现
    @Override
    public void onIncomingCall(String caller, String sdp, ServerTransaction transaction, Request request) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("来电");
            alert.setHeaderText(caller + " 正在呼叫您");
            alert.setContentText("是否接听？");

            alert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    try {
                        // 先停止之前的媒体流（避免端口冲突）
                        mediaManager.stopStreams();
                        
                        // 接听电话
                        String answerSdp = mediaManager.createSdpOffer(sdp.contains("m=video"));
                        Response okResponse = sipManager.getMessageFactory().createResponse(Response.OK, request);
                        
                        // 添加Contact头（必需）
                        javax.sip.address.Address contactAddress = sipManager.addressFactory.createAddress(
                            sipManager.addressFactory.createSipURI(
                                sipManager.getUsername(), 
                                sipManager.listeningPoint.getIPAddress() + ":" + sipManager.listeningPoint.getPort()
                            )
                        );
                        javax.sip.header.ContactHeader contactHeader = sipManager.getHeaderFactory().createContactHeader(contactAddress);
                        okResponse.addHeader(contactHeader);
                        
                        okResponse.setContent(answerSdp, sipManager.getHeaderFactory().createContentTypeHeader("application", "sdp"));
                        transaction.sendResponse(okResponse);

                        // 保存Dialog用于挂断
                        currentDialog = transaction.getDialog();
                        sipManager.setCurrentCallDialog(currentDialog);
                        inCall = true;
                        hangupButton.setVisible(true);

                        mediaManager.startAudioStream(sdp);
                        if (sdp.contains("m=video")) {
                            remoteVideoView.setVisible(true);
                            switchVideoSourceButton.setVisible(true);
                            mediaManager.startVideoStream(sdp);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    try {
                        // 拒绝电话
                        Response busyResponse = sipManager.getMessageFactory().createResponse(Response.BUSY_HERE, request);
                        transaction.sendResponse(busyResponse);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        });
    }

    @Override
    public void onCallEstablished(String remoteSdp) {
        Platform.runLater(() -> {
            showAlert("通话", "通话已建立");
            inCall = true;
            hangupButton.setVisible(true);
            
            mediaManager.startAudioStream(remoteSdp);
            if (remoteSdp.contains("m=video")) {
                remoteVideoView.setVisible(true);
                switchVideoSourceButton.setVisible(true);
                mediaManager.startVideoStream(remoteSdp);
            }
        });
    }

    @Override
    public void onRinging() {
        Platform.runLater(() -> {
            statusLabel.setText("对方响铃中...");
        });
    }

    @Override
    public void onCallEnded() {
        Platform.runLater(() -> {
            showAlert("通话", "通话已结束");
            inCall = false;
            hangupButton.setVisible(false);
            switchVideoSourceButton.setVisible(false);
            remoteVideoView.setVisible(false);
            currentDialog = null;
            mediaManager.stopStreams();
            statusLabel.setText("已连接");
        });
    }

    // SipMessageListener接口实现
    @Override
    public void onMessageReceived(String sender, String content) {
        Platform.runLater(() -> {
            // 如果当前正在和发送者聊天，显示消息
            if (currentContact != null && currentContact.getUsername().equals(sender)) {
                Message msg = new Message(null, sender, currentUser.getUsername(), content, "TEXT", null, false, false, java.time.LocalDateTime.now());
                messageListView.getItems().add(formatMessage(msg));
            }
            
            // 显示通知
            showAlert("新消息", sender + ": " + content);
        });
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.show();
    }
    
    /**
     * 处理群聊会议按钮点击
     */
    @FXML
    private void handleConference() {
        if (currentUser == null) {
            showAlert("提示", "请先登录");
            return;
        }
        
        // 检查是否在通话中
        if (inCall) {
            showAlert("提示", "请先结束当前通话");
            return;
        }
        
        // 弹出选择对话框：创建会议室或加入会议室
        Alert choiceAlert = new Alert(Alert.AlertType.CONFIRMATION);
        choiceAlert.setTitle("群聊会议");
        choiceAlert.setHeaderText("选择操作");
        choiceAlert.setContentText("是否创建新会议室？");
        
        ButtonType createButton = new ButtonType("创建会议室");
        ButtonType joinButton = new ButtonType("加入会议室");
        ButtonType cancelButton = new ButtonType("取消", ButtonBar.ButtonData.CANCEL_CLOSE);
        
        choiceAlert.getButtonTypes().setAll(createButton, joinButton, cancelButton);
        
        choiceAlert.showAndWait().ifPresent(response -> {
            if (response == createButton) {
                createConferenceRoom();
            } else if (response == joinButton) {
                joinConferenceRoom();
            }
        });
    }
    
    /**
     * 创建会议室
     */
    private void createConferenceRoom() {
        try {
            // 加载会议界面
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/conference.fxml"));
            BorderPane root = loader.load();
            
            // 获取控制器
            ConferenceController controller = loader.getController();
            
            // 创建新窗口
            Stage conferenceStage = new Stage();
            conferenceStage.setTitle("群聊会议");
            conferenceStage.setScene(new Scene(root, 1000, 700));
            
            // 窗口关闭时清理资源
            conferenceStage.setOnCloseRequest(event -> {
                // 会议控制器会自动清理
            });
            
            // 显示窗口
            conferenceStage.show();
            
            // 创建会议室
            controller.createConference(currentUser.getUsername(), jwtToken);
            
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("错误", "创建会议室失败: " + e.getMessage());
        }
    }
    
    /**
     * 加入会议室
     */
    private void joinConferenceRoom() {
        // 弹出输入对话框
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("加入会议室");
        dialog.setHeaderText("输入会议室号");
        dialog.setContentText("请输入会议室号（如：ROOM-1234）：");
        
        dialog.showAndWait().ifPresent(roomId -> {
            if (roomId.trim().isEmpty()) {
                showAlert("提示", "请输入会议室号");
                return;
            }
            
            try {
                // 加载会议界面
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/conference.fxml"));
                BorderPane root = loader.load();
                
                // 获取控制器
                ConferenceController controller = loader.getController();
                
                // 创建新窗口
                Stage conferenceStage = new Stage();
                conferenceStage.setTitle("群聊会议 - " + roomId);
                conferenceStage.setScene(new Scene(root, 1000, 700));
                
                // 显示窗口
                conferenceStage.show();
                
                // 加入会议室
                controller.joinConference(currentUser.getUsername(), roomId.trim(), jwtToken);
                
            } catch (Exception e) {
                e.printStackTrace();
                showAlert("错误", "加入会议室失败: " + e.getMessage());
            }
        });
    }
}

