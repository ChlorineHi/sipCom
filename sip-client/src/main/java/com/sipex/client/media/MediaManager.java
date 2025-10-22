package com.sipex.client.media;

import com.sipex.client.config.ClientConfig;
import javafx.scene.image.ImageView;

import java.net.InetAddress;
import java.util.Random;

/**
 * 媒体管理器 - 负责音视频流的处理
 * 使用JavaSound + 手写RTP实现真实音频传输
 * 使用屏幕捕获 + JPEG over RTP实现视频传输
 */
public class MediaManager {

    private int localAudioPort;
    private int localVideoPort;
    private String localIp;
    
    private RtpAudioSender audioSender;
    private RtpAudioReceiver audioReceiver;
    private RtpVideoSender videoSender;
    private RtpVideoReceiver videoReceiver;
    private ImageView remoteVideoView; // 用于显示远程视频

    public MediaManager() {
        try {
            // 获取本地IP
            localIp = InetAddress.getLocalHost().getHostAddress();

            // 分配RTP端口
            Random random = new Random();
            localAudioPort = ClientConfig.RTP_PORT_START + random.nextInt(1000) * 2;
            localVideoPort = localAudioPort + 2;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String createSdpOffer(boolean includeVideo) {
        StringBuilder sdp = new StringBuilder();
        sdp.append("v=0\r\n");
        sdp.append("o=- ").append(System.currentTimeMillis()).append(" ").append(System.currentTimeMillis()).append(" IN IP4 ").append(localIp).append("\r\n");
        sdp.append("s=SIP Call\r\n");
        sdp.append("c=IN IP4 ").append(localIp).append("\r\n");
        sdp.append("t=0 0\r\n");

        // 音频流
        sdp.append("m=audio ").append(localAudioPort).append(" RTP/AVP 0 8 101\r\n");
        sdp.append("a=rtpmap:0 PCMU/8000\r\n");
        sdp.append("a=rtpmap:8 PCMA/8000\r\n");
        sdp.append("a=rtpmap:101 telephone-event/8000\r\n");
        sdp.append("a=sendrecv\r\n");

        // 视频流
        if (includeVideo) {
            sdp.append("m=video ").append(localVideoPort).append(" RTP/AVP 96\r\n");
            sdp.append("a=rtpmap:96 H264/90000\r\n");
            sdp.append("a=fmtp:96 profile-level-id=42e01f\r\n");
            sdp.append("a=sendrecv\r\n");
        }

        return sdp.toString();
    }

    /**
     * 启动音频流 - 使用真实RTP传输
     */
    public void startAudioStream(String remoteSdp) {
        System.out.println("启动音频流...");
        
        try {
            // 解析远程SDP
            String remoteIp = parseSdpIp(remoteSdp);
            int remoteAudioPort = parseSdpAudioPort(remoteSdp);
            
            System.out.println("远程音频地址: " + remoteIp + ":" + remoteAudioPort);
            System.out.println("本地音频端口: " + localAudioPort);
            
            // 启动音频接收器
            audioReceiver = new RtpAudioReceiver(localAudioPort);
            audioReceiver.start();
            
            // 启动音频发送器
            audioSender = new RtpAudioSender(localAudioPort + 1, remoteIp, remoteAudioPort);
            audioSender.start();
            
            System.out.println("✅ 真实RTP音频流已启动！");
            
        } catch (Exception e) {
            System.err.println("❌ 启动音频流失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 从SDP中解析IP地址
     */
    private String parseSdpIp(String sdp) {
        // 查找 c=IN IP4 xxx.xxx.xxx.xxx
        String[] lines = sdp.split("\r\n");
        for (String line : lines) {
            if (line.startsWith("c=IN IP4 ")) {
                return line.substring(9).trim();
            }
        }
        return "127.0.0.1";
    }
    
    /**
     * 从SDP中解析音频端口
     */
    private int parseSdpAudioPort(String sdp) {
        // 查找 m=audio xxxx RTP/AVP 0
        String[] lines = sdp.split("\r\n");
        for (String line : lines) {
            if (line.startsWith("m=audio ")) {
                String[] parts = line.split(" ");
                if (parts.length >= 2) {
                    return Integer.parseInt(parts[1]);
                }
            }
        }
        return 10000;
    }

    /**
     * 启动视频流 - 使用真实RTP传输
     */
    public void startVideoStream(String remoteSdp) {
        System.out.println("启动视频流...");
        
        try {
            // 解析远程SDP
            String remoteIp = parseSdpIp(remoteSdp);
            int remoteVideoPort = parseSdpVideoPort(remoteSdp);
            
            System.out.println("远程视频地址: " + remoteIp + ":" + remoteVideoPort);
            System.out.println("本地视频端口: " + localVideoPort);
            
            // 启动视频接收器
            if (remoteVideoView != null) {
                videoReceiver = new RtpVideoReceiver(localVideoPort, remoteVideoView);
                videoReceiver.start();
            }
            
            // 启动视频发送器（屏幕共享）
            videoSender = new RtpVideoSender(localVideoPort + 1, remoteIp, remoteVideoPort);
            videoSender.start();
            
            System.out.println("✅ 真实RTP视频流已启动（屏幕共享模式）！");
            
        } catch (Exception e) {
            System.err.println("❌ 启动视频流失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 从SDP中解析视频端口
     */
    private int parseSdpVideoPort(String sdp) {
        // 查找 m=video xxxx RTP/AVP 96
        String[] lines = sdp.split("\r\n");
        for (String line : lines) {
            if (line.startsWith("m=video ")) {
                String[] parts = line.split(" ");
                if (parts.length >= 2) {
                    return Integer.parseInt(parts[1]);
                }
            }
        }
        return localVideoPort; // 默认返回本地视频端口
    }
    
    /**
     * 设置远程视频显示控件
     */
    public void setRemoteVideoView(ImageView videoView) {
        this.remoteVideoView = videoView;
    }
    
    /**
     * 切换视频源（摄像头/屏幕）
     */
    public void setVideoSource(RtpVideoSender.VideoSource source) {
        if (videoSender != null) {
            videoSender.setVideoSource(source);
        }
    }

    /**
     * 停止所有媒体流
     */
    public void stopStreams() {
        System.out.println("停止所有媒体流...");
        
        if (audioSender != null) {
            audioSender.stop();
            audioSender = null;
        }
        
        if (audioReceiver != null) {
            audioReceiver.stop();
            audioReceiver = null;
        }
        
        if (videoSender != null) {
            videoSender.stop();
            videoSender = null;
        }
        
        if (videoReceiver != null) {
            videoReceiver.stop();
            videoReceiver = null;
        }
        
        System.out.println("✅ 所有媒体流已停止");
    }

    public int getLocalAudioPort() {
        return localAudioPort;
    }

    public int getLocalVideoPort() {
        return localVideoPort;
    }

    public String getLocalIp() {
        return localIp;
    }
}
