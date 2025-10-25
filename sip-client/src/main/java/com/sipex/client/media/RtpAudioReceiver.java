package com.sipex.client.media;

import javax.sound.sampled.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;

/**
 * RTP音频接收器 - 接收RTP包并通过扬声器播放
 */
public class RtpAudioReceiver implements Runnable {

    private final DatagramSocket socket;
    private SourceDataLine speaker;
    private volatile boolean running = false;
    private AudioDataCallback audioDataCallback;

    // RTP参数
    private static final int SAMPLE_RATE = 8000;
    private static final int BUFFER_SIZE = 1024;

    public RtpAudioReceiver(int localPort) throws Exception {
        this.socket = new DatagramSocket(localPort);
        initializeSpeaker();
    }
    
    public RtpAudioReceiver(int localPort, AudioDataCallback callback) throws Exception {
        this.socket = new DatagramSocket(localPort);
        this.audioDataCallback = callback;
        // 如果有回调，不初始化扬声器（数据将通过回调传递）
        if (callback == null) {
            initializeSpeaker();
        }
    }
    
    private void initializeSpeaker() throws Exception {
        // 初始化扬声器
        AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        
        if (!AudioSystem.isLineSupported(info)) {
            throw new RuntimeException("扬声器不支持");
        }
        
        speaker = (SourceDataLine) AudioSystem.getLine(info);
        speaker.open(format);
    }

    public void start() {
        running = true;
        if (speaker != null) {
            speaker.start();
        }
        new Thread(this, "RTP-Audio-Receiver").start();
        System.out.println("✅ RTP音频接收器已启动: 端口 " + socket.getLocalPort());
    }

    public void stop() {
        running = false;
        if (speaker != null && speaker.isOpen()) {
            speaker.drain();
            speaker.stop();
            speaker.close();
        }
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
        System.out.println("❌ RTP音频接收器已停止");
    }

    @Override
    public void run() {
        byte[] buffer = new byte[BUFFER_SIZE];
        
        while (running) {
            try {
                // 接收RTP包
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                
                // 解析RTP头（12字节）
                ByteBuffer bb = ByteBuffer.wrap(packet.getData(), 0, packet.getLength());
                
                byte b0 = bb.get();
                byte b1 = bb.get();
                short seq = bb.getShort();
                int timestamp = bb.getInt();
                int ssrc = bb.getInt();
                
                // 提取payload
                int payloadLength = packet.getLength() - 12;
                byte[] payload = new byte[payloadLength];
                bb.get(payload);
                
                // G.711 μ-law解码为PCM
                byte[] pcmData = new byte[payloadLength * 2];
                for (int i = 0; i < payloadLength; i++) {
                    short sample = ulawToLinear(payload[i]);
                    pcmData[i * 2] = (byte) (sample & 0xFF);
                    pcmData[i * 2 + 1] = (byte) ((sample >> 8) & 0xFF);
                }
                
                // 播放音频或通过回调传递数据
                if (audioDataCallback != null) {
                    // 通过回调传递数据（用于混音器）
                    audioDataCallback.onAudioData(pcmData);
                } else if (speaker != null) {
                    // 直接播放
                    speaker.write(pcmData, 0, pcmData.length);
                }
                
            } catch (Exception e) {
                if (running) {
                    System.err.println("RTP接收错误: " + e.getMessage());
                }
            }
        }
    }

    /**
     * G.711 μ-law转线性PCM (简化算法)
     */
    private short ulawToLinear(byte ulaw) {
        ulaw = (byte) ~ulaw;
        int sign = ulaw & 0x80;
        int exponent = (ulaw >> 4) & 0x07;
        int mantissa = ulaw & 0x0F;
        
        int sample = ((mantissa << 3) + 0x84) << exponent;
        if (sign != 0) sample = -sample;
        
        return (short) sample;
    }
}

