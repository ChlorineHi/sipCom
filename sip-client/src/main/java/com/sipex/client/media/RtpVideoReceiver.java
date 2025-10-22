package com.sipex.client.media;

import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;

/**
 * RTP视频接收器 - 接收RTP视频包并显示
 */
public class RtpVideoReceiver implements Runnable {

    private final DatagramSocket socket;
    private volatile boolean running = false;
    private ImageView displayView; // JavaFX ImageView用于显示
    private ByteArrayOutputStream frameBuffer = new ByteArrayOutputStream();

    private static final int BUFFER_SIZE = 2048;

    public RtpVideoReceiver(int localPort, ImageView displayView) throws Exception {
        this.socket = new DatagramSocket(localPort);
        this.displayView = displayView;
    }

    public void start() {
        running = true;
        new Thread(this, "RTP-Video-Receiver").start();
        System.out.println("✅ RTP视频接收器已启动: 端口 " + socket.getLocalPort());
    }

    public void stop() {
        running = false;
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
        System.out.println("❌ RTP视频接收器已停止");
    }

    @Override
    public void run() {
        byte[] buffer = new byte[BUFFER_SIZE];
        
        while (running) {
            try {
                // 接收RTP包
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                
                // 解析RTP头
                ByteBuffer bb = ByteBuffer.wrap(packet.getData(), 0, packet.getLength());
                
                byte b0 = bb.get();
                byte b1 = bb.get();
                boolean marker = (b1 & 0x80) != 0; // Marker bit表示帧结束
                short seq = bb.getShort();
                int timestamp = bb.getInt();
                int ssrc = bb.getInt();
                
                // 提取payload
                int payloadLength = packet.getLength() - 12;
                byte[] payload = new byte[payloadLength];
                bb.get(payload);
                
                // 累积帧数据
                frameBuffer.write(payload);
                
                // 如果是帧的最后一个包，显示图像
                if (marker) {
                    displayFrame(frameBuffer.toByteArray());
                    frameBuffer.reset();
                }
                
            } catch (Exception e) {
                if (running) {
                    System.err.println("RTP视频接收错误: " + e.getMessage());
                }
            }
        }
    }

    /**
     * 显示接收到的视频帧
     */
    private void displayFrame(byte[] jpegData) {
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(jpegData);
            BufferedImage bufferedImage = ImageIO.read(bais);
            
            if (bufferedImage != null && displayView != null) {
                // 转换为JavaFX Image并在UI线程显示
                Platform.runLater(() -> {
                    try {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        ImageIO.write(bufferedImage, "png", baos);
                        Image image = new Image(new ByteArrayInputStream(baos.toByteArray()));
                        displayView.setImage(image);
                    } catch (Exception e) {
                        // 忽略显示错误
                    }
                });
            }
        } catch (Exception e) {
            // 忽略解码错误
        }
    }
}

