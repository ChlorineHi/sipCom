package com.sipex.client.media;

import javax.sound.sampled.*;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;

/**
 * RTP音频发送器 - 使用JavaSound捕获麦克风并通过RTP发送
 */
public class RtpAudioSender implements Runnable {

    private final DatagramSocket socket;
    private final InetAddress remoteAddress;
    private final int remotePort;
    private TargetDataLine microphone;
    private volatile boolean running = false;
    private int sequenceNumber = 0;
    private long timestamp = 0;
    private final int ssrc; // 随机生成的源标识符

    // RTP参数
    private static final int SAMPLE_RATE = 8000; // 8kHz (G.711标准)
    private static final int FRAME_SIZE = 160; // 20ms @ 8kHz
    private static final int PAYLOAD_TYPE = 0; // PCMU (G.711 μ-law)

    public RtpAudioSender(int localPort, String remoteHost, int remotePort) throws Exception {
        this.socket = new DatagramSocket(localPort);
        this.remoteAddress = InetAddress.getByName(remoteHost);
        this.remotePort = remotePort;
        this.ssrc = (int) (Math.random() * Integer.MAX_VALUE);
        
        // 初始化麦克风
        AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
        
        if (!AudioSystem.isLineSupported(info)) {
            throw new RuntimeException("麦克风不支持");
        }
        
        microphone = (TargetDataLine) AudioSystem.getLine(info);
        microphone.open(format);
    }

    public void start() {
        running = true;
        microphone.start();
        new Thread(this, "RTP-Audio-Sender").start();
        System.out.println("✅ RTP音频发送器已启动: " + remoteAddress + ":" + remotePort);
    }

    public void stop() {
        running = false;
        if (microphone != null && microphone.isOpen()) {
            microphone.stop();
            microphone.close();
        }
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
        System.out.println("❌ RTP音频发送器已停止");
    }

    @Override
    public void run() {
        byte[] audioBuffer = new byte[FRAME_SIZE * 2]; // 16-bit samples
        
        while (running) {
            try {
                // 从麦克风读取音频数据
                int bytesRead = microphone.read(audioBuffer, 0, audioBuffer.length);
                
                if (bytesRead > 0) {
                    // 转换为G.711 μ-law (简化版：直接取高8位)
                    byte[] payload = new byte[bytesRead / 2];
                    for (int i = 0; i < payload.length; i++) {
                        // 简单转换：16-bit -> 8-bit (实际应使用G.711算法)
                        short sample = (short) ((audioBuffer[i * 2 + 1] << 8) | (audioBuffer[i * 2] & 0xFF));
                        payload[i] = linearToUlaw(sample);
                    }
                    
                    // 构建RTP包
                    byte[] rtpPacket = buildRtpPacket(payload);
                    
                    // 发送RTP包
                    DatagramPacket packet = new DatagramPacket(
                        rtpPacket, rtpPacket.length, remoteAddress, remotePort
                    );
                    socket.send(packet);
                    
                    // 更新RTP头信息
                    sequenceNumber++;
                    timestamp += FRAME_SIZE;
                    
                    // 控制发送速率 (20ms per frame)
                    Thread.sleep(20);
                }
            } catch (Exception e) {
                if (running) {
                    System.err.println("RTP发送错误: " + e.getMessage());
                }
            }
        }
    }

    /**
     * 构建RTP包
     */
    private byte[] buildRtpPacket(byte[] payload) {
        ByteBuffer buffer = ByteBuffer.allocate(12 + payload.length);
        
        // RTP Header (12 bytes)
        buffer.put((byte) 0x80); // V=2, P=0, X=0, CC=0
        buffer.put((byte) PAYLOAD_TYPE); // M=0, PT=0 (PCMU)
        buffer.putShort((short) sequenceNumber);
        buffer.putInt((int) timestamp);
        buffer.putInt(ssrc);
        
        // Payload
        buffer.put(payload);
        
        return buffer.array();
    }

    /**
     * 线性PCM转G.711 μ-law (简化算法)
     */
    private byte linearToUlaw(short sample) {
        // 简化版本，实际应使用标准G.711算法
        int sign = (sample >> 8) & 0x80;
        if (sign != 0) sample = (short) -sample;
        if (sample > 32635) sample = 32635;
        
        sample = (short) (sample + 0x84);
        int exponent = 7;
        for (int expMask = 0x4000; (sample & expMask) == 0; exponent--, expMask >>= 1);
        
        int mantissa = (sample >> (exponent + 3)) & 0x0F;
        int ulaw = ~(sign | (exponent << 4) | mantissa);
        
        return (byte) ulaw;
    }
}

