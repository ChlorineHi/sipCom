package com.sipex.client.media;

import com.github.sarxos.webcam.Webcam;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;

/**
 * RTP视频发送器 - 支持摄像头和屏幕捕获
 * 可以通过setVideoSource()切换视频源
 */
public class RtpVideoSender implements Runnable {

    private final DatagramSocket socket;
    private final InetAddress remoteAddress;
    private final int remotePort;
    private volatile boolean running = false;
    private int sequenceNumber = 0;
    private long timestamp = 0;
    private final int ssrc;
    private Robot robot; // 用于屏幕捕获
    private Webcam webcam; // 摄像头
    private VideoSource videoSource = VideoSource.WEBCAM; // 默认使用摄像头

    // RTP参数
    private static final int PAYLOAD_TYPE = 26; // JPEG
    private static final int FPS = 15; // 15帧/秒
    private static final int FRAME_INTERVAL = 1000 / FPS; // 66ms
    private static final int VIDEO_WIDTH = 640;
    private static final int VIDEO_HEIGHT = 480;
    private static final int MTU = 1400; // 最大传输单元

    public enum VideoSource {
        WEBCAM,    // 摄像头
        SCREEN     // 屏幕捕获
    }

    public RtpVideoSender(int localPort, String remoteHost, int remotePort) throws Exception {
        this.socket = new DatagramSocket(localPort);
        this.remoteAddress = InetAddress.getByName(remoteHost);
        this.remotePort = remotePort;
        this.ssrc = (int) (Math.random() * Integer.MAX_VALUE);
        this.robot = new Robot(); // 屏幕捕获
        
        // 初始化摄像头
        try {
            webcam = Webcam.getDefault();
            if (webcam != null) {
                Dimension size = new Dimension(VIDEO_WIDTH, VIDEO_HEIGHT);
                webcam.setViewSize(size);
                System.out.println("✅ 检测到摄像头: " + webcam.getName());
            } else {
                System.out.println("⚠️ 未检测到摄像头，将使用屏幕捕获");
                videoSource = VideoSource.SCREEN;
            }
        } catch (Exception e) {
            System.out.println("⚠️ 摄像头初始化失败，将使用屏幕捕获: " + e.getMessage());
            videoSource = VideoSource.SCREEN;
        }
    }

    /**
     * 设置视频源
     */
    public void setVideoSource(VideoSource source) {
        this.videoSource = source;
        System.out.println("切换视频源到: " + (source == VideoSource.WEBCAM ? "摄像头" : "屏幕捕获"));
    }

    public void start() {
        running = true;
        
        // 如果使用摄像头，先打开
        if (videoSource == VideoSource.WEBCAM && webcam != null && !webcam.isOpen()) {
            webcam.open();
        }
        
        new Thread(this, "RTP-Video-Sender").start();
        System.out.println("✅ RTP视频发送器已启动: " + remoteAddress + ":" + remotePort);
        System.out.println("   视频源: " + (videoSource == VideoSource.WEBCAM ? "摄像头" : "屏幕捕获"));
    }

    public void stop() {
        running = false;
        
        // 关闭摄像头
        if (webcam != null && webcam.isOpen()) {
            webcam.close();
        }
        
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
        System.out.println("❌ RTP视频发送器已停止");
    }

    @Override
    public void run() {
        Rectangle screenRect = new Rectangle(0, 0, VIDEO_WIDTH, VIDEO_HEIGHT);
        
        while (running) {
            try {
                long startTime = System.currentTimeMillis();
                
                // 根据视频源捕获图像
                BufferedImage frame;
                if (videoSource == VideoSource.WEBCAM && webcam != null && webcam.isOpen()) {
                    // 从摄像头捕获
                    frame = webcam.getImage();
                    if (frame == null) {
                        Thread.sleep(FRAME_INTERVAL);
                        continue;
                    }
                } else {
                    // 从屏幕捕获
                    frame = robot.createScreenCapture(screenRect);
                }
                
                // 调整大小并转换为JPEG
                BufferedImage resized = resizeImage(frame, VIDEO_WIDTH, VIDEO_HEIGHT);
                byte[] jpegData = imageToJpeg(resized);
                
                // 分片发送（如果超过MTU）
                if (jpegData.length <= MTU) {
                    // 单个包
                    byte[] rtpPacket = buildRtpPacket(jpegData, true);
                    sendPacket(rtpPacket);
                } else {
                    // 分片发送
                    sendFragmented(jpegData);
                }
                
                sequenceNumber++;
                timestamp += 90000 / FPS; // 90kHz时钟
                
                // 控制帧率
                long elapsed = System.currentTimeMillis() - startTime;
                if (elapsed < FRAME_INTERVAL) {
                    Thread.sleep(FRAME_INTERVAL - elapsed);
                }
                
            } catch (Exception e) {
                if (running) {
                    System.err.println("RTP视频发送错误: " + e.getMessage());
                }
            }
        }
    }

    /**
     * 构建RTP包
     */
    private byte[] buildRtpPacket(byte[] payload, boolean marker) {
        ByteBuffer buffer = ByteBuffer.allocate(12 + payload.length);
        
        // RTP Header
        buffer.put((byte) 0x80); // V=2, P=0, X=0, CC=0
        buffer.put((byte) (marker ? (PAYLOAD_TYPE | 0x80) : PAYLOAD_TYPE)); // M bit
        buffer.putShort((short) sequenceNumber);
        buffer.putInt((int) timestamp);
        buffer.putInt(ssrc);
        
        // Payload
        buffer.put(payload);
        
        return buffer.array();
    }

    /**
     * 分片发送大的视频帧
     */
    private void sendFragmented(byte[] data) throws Exception {
        int offset = 0;
        int fragmentSize = MTU - 12; // 减去RTP头
        
        while (offset < data.length) {
            int chunkSize = Math.min(fragmentSize, data.length - offset);
            byte[] chunk = new byte[chunkSize];
            System.arraycopy(data, offset, chunk, 0, chunkSize);
            
            boolean isLast = (offset + chunkSize >= data.length);
            byte[] rtpPacket = buildRtpPacket(chunk, isLast);
            sendPacket(rtpPacket);
            
            offset += chunkSize;
            sequenceNumber++;
        }
    }

    /**
     * 发送RTP包
     */
    private void sendPacket(byte[] data) throws Exception {
        DatagramPacket packet = new DatagramPacket(data, data.length, remoteAddress, remotePort);
        socket.send(packet);
    }

    /**
     * 调整图像大小
     */
    private BufferedImage resizeImage(BufferedImage original, int width, int height) {
        BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resized.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(original, 0, 0, width, height, null);
        g.dispose();
        return resized;
    }

    /**
     * 将图像转换为JPEG字节数组
     */
    private byte[] imageToJpeg(BufferedImage image) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", baos);
        return baos.toByteArray();
    }
}
