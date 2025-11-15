package com.sipex.client.media;

import com.sipex.client.config.ClientConfig;
import javafx.scene.image.ImageView;

import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 群聊会议媒体管理器
 * 管理多个参与者的音视频流
 */
public class ConferenceMediaManager {
    
    private String localIp;
    private int baseAudioPort;
    private int baseVideoPort;

    // 端口池管理
    private final Set<Integer> allocatedPorts = ConcurrentHashMap.newKeySet();
    private final Object portLock = new Object();
    
    // 参与者连接管理
    private final Map<String, ParticipantConnection> participants;
    
    // 音频混音器
    private AudioMixer audioMixer;
    
    // 本地音频和视频发送器、接收器
    private SharedAudioCapture sharedAudioCapture;
    private RtpVideoSender localVideoSender;
    private RtpVideoReceiver localVideoReceiver;
    
    // 视频显示映射
    private final Map<String, ImageView> videoViews;
    
    /**
     * 参与者连接信息
     */
    private static class ParticipantConnection {
        String username;
        String remoteIp;
        int remoteAudioPort;
        int remoteVideoPort;
        int localAudioPort;
        int localVideoPort;
        
        RtpAudioForwarder audioForwarder;
        RtpAudioReceiver audioReceiver;
        RtpVideoSender videoSender;
        RtpVideoReceiver videoReceiver;
        
        int audioMixerIndex = -1; // 在混音器中的索引
    }
    
    public ConferenceMediaManager() {
        this.participants = new ConcurrentHashMap<>();
        this.videoViews = new ConcurrentHashMap<>();

        try {
            // 获取本地IP
            localIp = InetAddress.getLocalHost().getHostAddress();

            // 分配基础RTP端口
            Random random = new Random();
            baseAudioPort = ClientConfig.RTP_PORT_START + random.nextInt(1000) * 10;
            baseVideoPort = baseAudioPort + 1000;

            // 初始化音频混音器
            audioMixer = new AudioMixer();

            // 添加shutdown hook，确保资源清理
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("检测到JVM关闭，清理会议资源...");
                stopConference();
            }, "ConferenceMediaManager-ShutdownHook"));

            System.out.println("会议媒体管理器初始化完成");
            System.out.println("本地IP: " + localIp);
            System.out.println("基础音频端口: " + baseAudioPort);
            System.out.println("基础视频端口: " + baseVideoPort);

        } catch (Exception e) {
            System.err.println("初始化会议媒体管理器失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 创建SDP offer（用于群聊）
     */
    public String createSdpOffer(boolean includeVideo, int participantIndex) {
        int audioPort = baseAudioPort + participantIndex * 2;
        int videoPort = baseVideoPort + participantIndex * 2;
        
        StringBuilder sdp = new StringBuilder();
        sdp.append("v=0\r\n");
        sdp.append("o=- ").append(System.currentTimeMillis()).append(" ")
           .append(System.currentTimeMillis()).append(" IN IP4 ").append(localIp).append("\r\n");
        sdp.append("s=Conference Call\r\n");
        sdp.append("c=IN IP4 ").append(localIp).append("\r\n");
        sdp.append("t=0 0\r\n");
        
        // 音频流
        sdp.append("m=audio ").append(audioPort).append(" RTP/AVP 0 8 101\r\n");
        sdp.append("a=rtpmap:0 PCMU/8000\r\n");
        sdp.append("a=rtpmap:8 PCMA/8000\r\n");
        sdp.append("a=rtpmap:101 telephone-event/8000\r\n");
        sdp.append("a=sendrecv\r\n");
        
        // 视频流
        if (includeVideo) {
            sdp.append("m=video ").append(videoPort).append(" RTP/AVP 96\r\n");
            sdp.append("a=rtpmap:96 H264/90000\r\n");
            sdp.append("a=fmtp:96 profile-level-id=42e01f\r\n");
            sdp.append("a=sendrecv\r\n");
        }
        
        return sdp.toString();
    }
    
    /**
     * 添加参与者
     */
    public void addParticipant(String username, String remoteSdp, boolean includeVideo) {
        // 检查参与者是否已存在
        if (participants.containsKey(username)) {
            System.out.println("参与者 " + username + " 已存在，跳过添加");
            return;
        }
        
        try {
            int participantIndex = participants.size();
            
            ParticipantConnection conn = new ParticipantConnection();
            conn.username = username;
            conn.remoteIp = parseSdpIp(remoteSdp);
            conn.remoteAudioPort = parseSdpAudioPort(remoteSdp);
            // 分配端口，确保不冲突
            conn.localAudioPort = findAvailablePort(baseAudioPort + (participantIndex + 1) * 2);
            conn.localVideoPort = findAvailablePort(baseVideoPort + (participantIndex + 1) * 2);
            
            // 添加到混音器
            conn.audioMixerIndex = audioMixer.addAudioSource();
            
            // 启动音频接收器（带回调，将数据传递给混音器）
            conn.audioReceiver = new RtpAudioReceiver(conn.localAudioPort, 
                audioData -> audioMixer.addAudioData(conn.audioMixerIndex, audioData));
            conn.audioReceiver.start();
            
            // 为这个参与者创建音频转发器（发送共享音频到该参与者）
            conn.audioForwarder = new RtpAudioForwarder(
                conn.localAudioPort + 1, 
                conn.remoteIp, 
                conn.remoteAudioPort
            );
            conn.audioForwarder.start();
            
            System.out.println("为参与者 " + username + " 创建音频转发器: " + conn.remoteIp + ":" + conn.remoteAudioPort);
            
            // 处理视频
            if (includeVideo && remoteSdp.contains("m=video")) {
                conn.remoteVideoPort = parseSdpVideoPort(remoteSdp);
                
                // 启动视频接收器
                ImageView videoView = videoViews.get(username);
                if (videoView != null) {
                    conn.videoReceiver = new RtpVideoReceiver(conn.localVideoPort, videoView);
                    conn.videoReceiver.start();
                }
                
                // 启动视频发送器
                conn.videoSender = new RtpVideoSender(
                    conn.localVideoPort + 1,
                    conn.remoteIp,
                    conn.remoteVideoPort
                );
                conn.videoSender.start();
            }
            
            participants.put(username, conn);
            System.out.println("✅ 添加参与者 " + username + " 音视频流");
            System.out.println("   远程地址: " + conn.remoteIp + ":" + conn.remoteAudioPort);
            
        } catch (Exception e) {
            System.err.println("❌ 添加参与者失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    
    /**
     * 移除参与者
     */
    public void removeParticipant(String username) {
        ParticipantConnection conn = participants.remove(username);
        if (conn != null) {
            try {
                // 停止音频转发器
                if (conn.audioForwarder != null) {
                    try {
                        conn.audioForwarder.stop();
                    } catch (Exception e) {
                        System.err.println("停止音频转发器失败: " + e.getMessage());
                    }
                    conn.audioForwarder = null;
                }

                // 停止音频接收器
                if (conn.audioReceiver != null) {
                    try {
                        conn.audioReceiver.stop();
                    } catch (Exception e) {
                        System.err.println("停止音频接收器失败: " + e.getMessage());
                    }
                    conn.audioReceiver = null;
                }

                // 停止视频发送器
                if (conn.videoSender != null) {
                    try {
                        conn.videoSender.stop();
                    } catch (Exception e) {
                        System.err.println("停止视频发送器失败: " + e.getMessage());
                    }
                    conn.videoSender = null;
                }

                // 停止视频接收器
                if (conn.videoReceiver != null) {
                    try {
                        conn.videoReceiver.stop();
                    } catch (Exception e) {
                        System.err.println("停止视频接收器失败: " + e.getMessage());
                    }
                    conn.videoReceiver = null;
                }

                // 从混音器移除
                if (conn.audioMixerIndex >= 0) {
                    try {
                        audioMixer.removeAudioSource(conn.audioMixerIndex);
                    } catch (Exception e) {
                        System.err.println("从混音器移除音频源失败: " + e.getMessage());
                    }
                }

                // 释放端口
                if (conn.localAudioPort > 0) {
                    releasePort(conn.localAudioPort);
                    releasePort(conn.localAudioPort + 1);
                }
                if (conn.localVideoPort > 0) {
                    releasePort(conn.localVideoPort);
                    releasePort(conn.localVideoPort + 1);
                }

                System.out.println("✅ 参与者 " + username + " 资源已清理");

            } catch (Exception e) {
                System.err.println("移除参与者 " + username + " 时发生错误: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    /**
     * 设置视频显示控件
     */
    public void setVideoView(String username, ImageView imageView) {
        videoViews.put(username, imageView);
        
        // 如果是本地用户且本地视频发送器已启动，启动本地视频接收器
        if (localVideoSender != null && localVideoReceiver == null) {
            try {
                localVideoReceiver = new RtpVideoReceiver(baseVideoPort + 1, imageView);
                localVideoReceiver.start();
                System.out.println("本地视频预览已启动");
            } catch (Exception e) {
                System.err.println("启动本地视频预览失败: " + e.getMessage());
            }
        }
    }
    
    /**
     * 启动会议
     */
    public void startConference() {
        audioMixer.start();
        
        // 启动共享音频采集器
        try {
            sharedAudioCapture = SharedAudioCapture.getInstance();
            System.out.println("共享音频采集器已准备就绪");
        } catch (Exception e) {
            System.err.println("启动共享音频采集器失败: " + e.getMessage());
        }
        
        // 启动本地视频采集和预览
        try {
            // 创建本地视频发送器（发送到本地回环地址用于自己预览）
            localVideoSender = new RtpVideoSender(baseVideoPort, "127.0.0.1", baseVideoPort + 1);
            localVideoSender.start();
            
            System.out.println("本地视频采集已启动");
        } catch (Exception e) {
            System.err.println("启动本地视频失败: " + e.getMessage());
        }
        
        System.out.println("会议已启动");
    }
    
    /**
     * 停止会议
     */
    public void stopConference() {
        System.out.println("开始停止会议，清理所有资源...");

        try {
            // 停止所有参与者连接
            for (String username : new ArrayList<>(participants.keySet())) {
                removeParticipant(username);
            }

            // 停止混音器
            if (audioMixer != null) {
                try {
                    audioMixer.stop();
                } catch (Exception e) {
                    System.err.println("停止混音器失败: " + e.getMessage());
                }
                audioMixer = null;
            }

            // 停止本地音频采集器
            if (sharedAudioCapture != null) {
                try {
                    // 共享音频采集器会自动管理生命周期
                    sharedAudioCapture = null;
                } catch (Exception e) {
                    System.err.println("停止音频采集器失败: " + e.getMessage());
                }
            }

            // 停止本地视频发送器
            if (localVideoSender != null) {
                try {
                    localVideoSender.stop();
                } catch (Exception e) {
                    System.err.println("停止本地视频发送器失败: " + e.getMessage());
                }
                localVideoSender = null;
            }

            // 停止本地视频接收器
            if (localVideoReceiver != null) {
                try {
                    localVideoReceiver.stop();
                } catch (Exception e) {
                    System.err.println("停止本地视频接收器失败: " + e.getMessage());
                }
                localVideoReceiver = null;
            }

            // 清空视频视图映射
            videoViews.clear();

            System.out.println("✅ 会议已停止，所有资源已清理");

        } catch (Exception e) {
            System.err.println("停止会议过程中发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 获取本地IP
     */
    public String getLocalIp() {
        return localIp;
    }
    
    /**
     * 获取参与者数量
     */
    public int getParticipantCount() {
        return participants.size();
    }
    
    // ========== SDP解析辅助方法 ==========
    
    private String parseSdpIp(String sdp) {
        String[] lines = sdp.split("\r\n");
        for (String line : lines) {
            if (line.startsWith("c=IN IP4 ")) {
                return line.substring(9).trim();
            }
        }
        return "127.0.0.1";
    }
    
    private int parseSdpAudioPort(String sdp) {
        String[] lines = sdp.split("\r\n");
        for (String line : lines) {
            if (line.startsWith("m=audio ")) {
                String[] parts = line.split(" ");
                if (parts.length >= 2) {
                    return Integer.parseInt(parts[1]);
                }
            }
        }
        return 5004;
    }
    
    private int parseSdpVideoPort(String sdp) {
        String[] lines = sdp.split("\r\n");
        for (String line : lines) {
            if (line.startsWith("m=video ")) {
                String[] parts = line.split(" ");
                if (parts.length >= 2) {
                    return Integer.parseInt(parts[1]);
                }
            }
        }
        return 5006;
    }
    
    /**
     * 查找可用端口（改进版：使用端口池和锁机制）
     */
    private int findAvailablePort(int startPort) {
        synchronized (portLock) {
            for (int port = startPort; port < startPort + 100; port += 2) {
                // 检查端口是否已被分配
                if (allocatedPorts.contains(port)) {
                    continue;
                }

                // 检查端口是否真正可用
                if (isPortAvailable(port)) {
                    allocatedPorts.add(port);
                    System.out.println("分配端口: " + port);
                    return port;
                }
            }
            throw new RuntimeException("无法找到可用端口，起始端口: " + startPort);
        }
    }

    /**
     * 释放端口
     */
    private void releasePort(int port) {
        synchronized (portLock) {
            if (allocatedPorts.remove(port)) {
                System.out.println("释放端口: " + port);
            }
        }
    }
    
    /**
     * 检查端口是否可用
     */
    private boolean isPortAvailable(int port) {
        try (java.net.DatagramSocket socket = new java.net.DatagramSocket(port)) {
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}

