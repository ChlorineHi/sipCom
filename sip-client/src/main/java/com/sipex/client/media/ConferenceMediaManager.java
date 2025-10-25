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
    
    // 参与者连接管理
    private final Map<String, ParticipantConnection> participants;
    
    // 音频混音器
    private AudioMixer audioMixer;
    
    // 本地视频发送器和接收器
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
        
        RtpAudioSender audioSender;
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
        try {
            int participantIndex = participants.size();
            
            ParticipantConnection conn = new ParticipantConnection();
            conn.username = username;
            conn.remoteIp = parseSdpIp(remoteSdp);
            conn.remoteAudioPort = parseSdpAudioPort(remoteSdp);
            conn.localAudioPort = baseAudioPort + participantIndex * 2;
            conn.localVideoPort = baseVideoPort + participantIndex * 2;
            
            // 启动音频接收器
            conn.audioReceiver = new RtpAudioReceiver(conn.localAudioPort);
            conn.audioReceiver.start();
            
            // 添加到混音器
            conn.audioMixerIndex = audioMixer.addAudioSource();
            
            // 创建音频接收线程（将数据传递给混音器）
            startAudioReceiving(conn);
            
            // 启动音频发送器
            conn.audioSender = new RtpAudioSender(
                conn.localAudioPort + 1, 
                conn.remoteIp, 
                conn.remoteAudioPort
            );
            conn.audioSender.start();
            
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
     * 启动音频接收线程（传递给混音器）
     */
    private void startAudioReceiving(ParticipantConnection conn) {
        new Thread(() -> {
            try {
                while (participants.containsKey(conn.username)) {
                    // 这里简化处理，实际应该从RtpAudioReceiver读取数据
                    // 由于当前RtpAudioReceiver直接播放，需要修改为提供数据接口
                    Thread.sleep(20);
                }
            } catch (Exception e) {
                System.err.println("音频接收线程错误: " + e.getMessage());
            }
        }, "Audio-Receive-" + conn.username).start();
    }
    
    /**
     * 移除参与者
     */
    public void removeParticipant(String username) {
        ParticipantConnection conn = participants.remove(username);
        if (conn != null) {
            // 停止音频
            if (conn.audioSender != null) conn.audioSender.stop();
            if (conn.audioReceiver != null) conn.audioReceiver.stop();
            
            // 停止视频
            if (conn.videoSender != null) conn.videoSender.stop();
            if (conn.videoReceiver != null) conn.videoReceiver.stop();
            
            // 从混音器移除
            if (conn.audioMixerIndex >= 0) {
                audioMixer.removeAudioSource(conn.audioMixerIndex);
            }
            
            System.out.println("移除参与者 " + username);
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
        // 停止所有参与者连接
        for (String username : new ArrayList<>(participants.keySet())) {
            removeParticipant(username);
        }
        
        // 停止混音器
        if (audioMixer != null) {
            audioMixer.stop();
        }
        
        // 停止本地视频发送器和接收器
        if (localVideoSender != null) {
            localVideoSender.stop();
            localVideoSender = null;
        }
        if (localVideoReceiver != null) {
            localVideoReceiver.stop();
            localVideoReceiver = null;
        }
        
        System.out.println("会议已停止");
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
}

