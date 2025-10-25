package com.sipex.client.ui;

import com.google.gson.Gson;
import com.sipex.client.config.ClientConfig;
import com.sipex.client.media.ConferenceMediaManager;
import com.sipex.client.service.HttpClientService;
import com.sipex.client.sip.ConferenceSipManager;
import com.sipex.common.dto.ApiResponse;
import com.sipex.common.dto.ConferenceRequest;
import com.sipex.common.dto.ConferenceResponse;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

import javax.sip.ServerTransaction;
import javax.sip.message.Request;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 群聊会议控制器
 */
public class ConferenceController implements ConferenceSipManager.ConferenceCallListener {
    
    @FXML private Label roomIdLabel;
    @FXML private Label participantCountLabel;
    @FXML private Button leaveButton;
    @FXML private GridPane videoGrid;
    @FXML private ListView<String> participantListView;
    @FXML private Button muteButton;
    @FXML private Button videoButton;
    @FXML private Button switchVideoButton;
    @FXML private Label statusLabel;
    
    private String currentRoomId;
    private String currentUsername;
    private ConferenceSipManager sipManager;
    private ConferenceMediaManager mediaManager;
    
    private boolean isMuted = false;
    private boolean isVideoEnabled = true;
    
    // 参与者视频显示映射
    private final Map<String, VBox> participantVideoBoxes = new ConcurrentHashMap<>();
    
    private Timer pollTimer; // 定时轮询参与者列表
    
    public void initialize() {
        System.out.println("群聊会议控制器初始化...");
        
        // 初始化媒体管理器
        mediaManager = new ConferenceMediaManager();
        
        // 初始化SIP管理器
        sipManager = new ConferenceSipManager(this);
        
        // 初始化参与者列表
        participantListView.setCellFactory(param -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    String displayText = item.equals(currentUsername) ? 
                        "☑ " + item + " (你)" : "☑ " + item;
                    setText(displayText);
                }
            }
        });
    }
    
    /**
     * 创建会议室
     */
    public void createConference(String username, String jwt) {
        this.currentUsername = username;
        
        try {
            // 初始化SIP管理器
            sipManager.initialize(username, "");
            
            // 调用服务器API创建会议室
            ConferenceRequest request = new ConferenceRequest();
            request.setUsername(username);
            request.setAction("create");
            
            var responseType = com.google.gson.reflect.TypeToken.getParameterized(
                ApiResponse.class, 
                ConferenceResponse.class
            ).getType();
            
            ApiResponse<ConferenceResponse> apiResponse = HttpClientService.post(
                "/api/conference/create",
                request,
                responseType
            );
            
            if (apiResponse.getCode() == 200) {
                ConferenceResponse conferenceData = apiResponse.getData();
                currentRoomId = conferenceData.getRoomId();
                
                Platform.runLater(() -> {
                    roomIdLabel.setText("会议室: " + currentRoomId);
                    statusLabel.setText("会议室已创建，等待其他人加入...");
                    updateParticipantList(conferenceData.getParticipants());
                });
                
                // 启动会议
                mediaManager.startConference();
                
                // 为自己创建视频显示框
                Platform.runLater(() -> {
                    VBox localVideoBox = createVideoBox(username + " (我)");
                    participantVideoBoxes.put(username, localVideoBox);
                    updateVideoGrid();
                });
                
                // 开始轮询参与者列表
                startPolling();
                
                System.out.println("✅ 会议室创建成功: " + currentRoomId);
            } else {
                showError("创建会议室失败: " + apiResponse.getMessage());
            }
            
        } catch (Exception e) {
            showError("创建会议室失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 加入会议室
     */
    public void joinConference(String username, String roomId, String jwt) {
        this.currentUsername = username;
        this.currentRoomId = roomId;
        
        try {
            // 初始化SIP管理器
            sipManager.initialize(username, "");
            
            // 调用服务器API加入会议室
            ConferenceRequest request = new ConferenceRequest();
            request.setUsername(username);
            request.setRoomId(roomId);
            request.setAction("join");
            
            var responseType = com.google.gson.reflect.TypeToken.getParameterized(
                ApiResponse.class,
                ConferenceResponse.class
            ).getType();
            
            ApiResponse<ConferenceResponse> apiResponse = HttpClientService.post(
                "/api/conference/join",
                request,
                responseType
            );
            
            if (apiResponse.getCode() == 200) {
                ConferenceResponse conferenceData = apiResponse.getData();
                
                Platform.runLater(() -> {
                    roomIdLabel.setText("会议室: " + currentRoomId);
                    statusLabel.setText("正在连接到其他参与者...");
                    updateParticipantList(conferenceData.getParticipants());
                });
                
                // 启动会议
                mediaManager.startConference();
                
                // 为自己创建视频显示框
                Platform.runLater(() -> {
                    VBox localVideoBox = createVideoBox(username + " (我)");
                    participantVideoBoxes.put(username, localVideoBox);
                    updateVideoGrid();
                });
                
                // 向所有已存在的参与者发起呼叫
                for (String participant : conferenceData.getParticipants()) {
                    if (!participant.equals(username)) {
                        inviteParticipant(participant);
                    }
                }
                
                // 开始轮询参与者列表
                startPolling();
                
                System.out.println("✅ 加入会议室成功: " + currentRoomId);
            } else {
                showError("加入会议室失败: " + apiResponse.getMessage());
            }
            
        } catch (Exception e) {
            showError("加入会议室失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 向参与者发起呼叫
     */
    private void inviteParticipant(String username) {
        try {
            int participantIndex = participantVideoBoxes.size();
            String sdp = mediaManager.createSdpOffer(true, participantIndex);
            sipManager.inviteParticipant(username, sdp);
        } catch (Exception e) {
            System.err.println("呼叫参与者失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 开始轮询参与者列表
     */
    private void startPolling() {
        pollTimer = new Timer("Conference-Poller", true);
        pollTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                pollParticipants();
            }
        }, 2000, 2000); // 每2秒轮询一次
    }
    
    /**
     * 轮询参与者列表
     */
    private void pollParticipants() {
        try {
            var responseType = com.google.gson.reflect.TypeToken.getParameterized(
                ApiResponse.class,
                ConferenceResponse.class
            ).getType();
            
            ApiResponse<ConferenceResponse> apiResponse = HttpClientService.get(
                "/api/conference/" + currentRoomId,
                responseType
            );
            
            if (apiResponse.getCode() == 200) {
                ConferenceResponse data = apiResponse.getData();
                Platform.runLater(() -> {
                    updateParticipantList(data.getParticipants());
                });
                
                // 检查是否有新参与者加入
                for (String participant : data.getParticipants()) {
                    if (!participant.equals(currentUsername) && 
                        !participantVideoBoxes.containsKey(participant)) {
                        // 新参与者，发起呼叫
                        inviteParticipant(participant);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("轮询参与者失败: " + e.getMessage());
        }
    }
    
    /**
     * 更新参与者列表
     */
    private void updateParticipantList(List<String> participants) {
        participantListView.getItems().clear();
        participantListView.getItems().addAll(participants);
        participantCountLabel.setText("参与者: " + participants.size() + "/5");
    }
    
    /**
     * 更新视频网格布局
     */
    private void updateVideoGrid() {
        Platform.runLater(() -> {
            videoGrid.getChildren().clear();
            
            int participantCount = participantVideoBoxes.size();
            int columns = calculateGridColumns(participantCount);
            int rows = (int) Math.ceil((double) participantCount / columns);
            
            int index = 0;
            for (Map.Entry<String, VBox> entry : participantVideoBoxes.entrySet()) {
                int row = index / columns;
                int col = index % columns;
                
                VBox videoBox = entry.getValue();
                videoBox.setMinSize(200, 150);
                videoBox.setPrefSize(300, 225);
                videoBox.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
                
                GridPane.setRowIndex(videoBox, row);
                GridPane.setColumnIndex(videoBox, col);
                GridPane.setHgrow(videoBox, Priority.ALWAYS);
                GridPane.setVgrow(videoBox, Priority.ALWAYS);
                
                videoGrid.getChildren().add(videoBox);
                index++;
            }
        });
    }
    
    /**
     * 计算网格列数
     */
    private int calculateGridColumns(int count) {
        if (count <= 2) return 2;
        if (count <= 4) return 2;
        return 3;
    }
    
    /**
     * 创建视频显示框
     */
    private VBox createVideoBox(String username) {
        VBox box = new VBox(5);
        box.setAlignment(Pos.CENTER);
        box.getStyleClass().add("video-pane");
        
        // 视频显示区域
        ImageView videoView = new ImageView();
        videoView.setFitWidth(280);
        videoView.setFitHeight(200);
        videoView.setPreserveRatio(true);
        
        // 用户名标签
        Label nameLabel = new Label(username);
        nameLabel.getStyleClass().add("video-label");
        
        // 状态标签
        Label statusLabel = new Label("🎤 📹");
        statusLabel.getStyleClass().add("video-status");
        
        box.getChildren().addAll(videoView, nameLabel, statusLabel);
        
        // 保存到媒体管理器
        mediaManager.setVideoView(username, videoView);
        
        return box;
    }
    
    // ========== SIP回调实现 ==========
    
    @Override
    public void onParticipantInvited(String username) {
        Platform.runLater(() -> {
            statusLabel.setText("正在呼叫 " + username + "...");
        });
    }
    
    @Override
    public void onParticipantConnected(String username, String sdp) {
        Platform.runLater(() -> {
            statusLabel.setText(username + " 已连接");
            
            // 创建视频显示框
            if (!participantVideoBoxes.containsKey(username)) {
                VBox videoBox = createVideoBox(username);
                participantVideoBoxes.put(username, videoBox);
                updateVideoGrid();
            }
            
            // 启动媒体流
            mediaManager.addParticipant(username, sdp, true);
        });
    }
    
    @Override
    public void onParticipantDisconnected(String username) {
        Platform.runLater(() -> {
            statusLabel.setText(username + " 已离开");
            
            // 移除视频显示
            participantVideoBoxes.remove(username);
            updateVideoGrid();
            
            // 停止媒体流
            mediaManager.removeParticipant(username);
        });
    }
    
    @Override
    public void onIncomingConferenceCall(String caller, String sdp, ServerTransaction transaction, Request request) {
        // 自动接受群聊呼叫
        Platform.runLater(() -> {
            try {
                // 创建视频显示框
                if (!participantVideoBoxes.containsKey(caller)) {
                    VBox videoBox = createVideoBox(caller);
                    participantVideoBoxes.put(caller, videoBox);
                    updateVideoGrid();
                }
                
                // 创建应答SDP
                int participantIndex = participantVideoBoxes.size() - 1;
                String answerSdp = mediaManager.createSdpOffer(sdp.contains("m=video"), participantIndex);
                
                // 发送200 OK
                javax.sip.message.Response okResponse = sipManager.getMessageFactory().createResponse(
                    javax.sip.message.Response.OK, 
                    request
                );
                
                okResponse.setContent(answerSdp, sipManager.getHeaderFactory().createContentTypeHeader("application", "sdp"));
                transaction.sendResponse(okResponse);
                
                // 启动媒体流
                mediaManager.addParticipant(caller, sdp, sdp.contains("m=video"));
                
                statusLabel.setText(caller + " 已加入会议");
                
            } catch (Exception e) {
                System.err.println("接受呼叫失败: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
    
    // ========== 控制按钮处理 ==========
    
    @FXML
    private void handleMute() {
        isMuted = !isMuted;
        muteButton.setText(isMuted ? "🎤 取消静音" : "🎤 静音");
        statusLabel.setText(isMuted ? "麦克风已静音" : "麦克风已开启");
        // TODO: 实现静音功能
    }
    
    @FXML
    private void handleVideo() {
        isVideoEnabled = !isVideoEnabled;
        videoButton.setText(isVideoEnabled ? "📹 关闭视频" : "📹 开启视频");
        statusLabel.setText(isVideoEnabled ? "视频已开启" : "视频已关闭");
        // TODO: 实现视频开关功能
    }
    
    @FXML
    private void handleSwitchVideo() {
        statusLabel.setText("切换视频源...");
        // TODO: 实现视频源切换
    }
    
    @FXML
    private void handleLeaveConference() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("离开会议");
        alert.setHeaderText("确定要离开会议吗？");
        alert.setContentText("离开后将断开与所有参与者的连接。");
        
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                leaveConference();
            }
        });
    }
    
    /**
     * 离开会议
     */
    private void leaveConference() {
        try {
            // 停止轮询
            if (pollTimer != null) {
                pollTimer.cancel();
            }
            
            // 通知服务器离开
            ConferenceRequest request = new ConferenceRequest();
            request.setUsername(currentUsername);
            request.setRoomId(currentRoomId);
            request.setAction("leave");
            
            var responseType = com.google.gson.reflect.TypeToken.getParameterized(
                ApiResponse.class,
                ConferenceResponse.class
            ).getType();
            
            HttpClientService.post(
                "/api/conference/leave",
                request,
                responseType
            );
            
            // 停止所有SIP连接
            sipManager.hangupAll();
            sipManager.shutdown();
            
            // 停止媒体流
            mediaManager.stopConference();
            
            // 关闭窗口
            Platform.runLater(() -> {
                leaveButton.getScene().getWindow().hide();
            });
            
            System.out.println("已离开会议: " + currentRoomId);
            
        } catch (Exception e) {
            System.err.println("离开会议失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void showError(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("错误");
            alert.setHeaderText("操作失败");
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
}

