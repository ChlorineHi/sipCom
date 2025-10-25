package com.sipex.client.media;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * RTP音频转发器
 * 不采集音频，只负责将接收到的音频数据通过RTP发送
 */
public class RtpAudioForwarder implements Runnable, SharedAudioCapture.AudioDataListener {

    private final DatagramSocket socket;
    private final InetAddress remoteAddress;
    private final int remotePort;
    private volatile boolean running = false;
    private int sequenceNumber = 0;
    private long timestamp = 0;
    private final int ssrc; // 随机生成的源标识符
    
    private final BlockingQueue<byte[]> audioQueue;
    
    // RTP参数
    private static final int PAYLOAD_TYPE = 0; // PCMU (G.711 μ-law)

    public RtpAudioForwarder(int localPort, String remoteHost, int remotePort) throws Exception {
        this.socket = new DatagramSocket(localPort);
        this.remoteAddress = InetAddress.getByName(remoteHost);
        this.remotePort = remotePort;
        this.ssrc = (int) (Math.random() * Integer.MAX_VALUE);
        this.audioQueue = new LinkedBlockingQueue<>(100);
    }

    public void start() {
        running = true;
        
        // 注册到共享音频采集器
        SharedAudioCapture.getInstance().addListener(this);
        
        // 启动发送线程
        new Thread(this, "RTP-Audio-Forwarder").start();
        System.out.println("✅ RTP音频转发器已启动: " + remoteAddress + ":" + remotePort);
    }

    public void stop() {
        running = false;
        
        // 从共享音频采集器注销
        SharedAudioCapture.getInstance().removeListener(this);
        
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
        System.out.println("❌ RTP音频转发器已停止");
    }
    
    @Override
    public void onAudioData(byte[] audioData) {
        // 将音频数据加入队列
        if (!audioQueue.offer(audioData)) {
            // 队列满了，丢弃最旧的数据
            audioQueue.poll();
            audioQueue.offer(audioData);
        }
    }

    @Override
    public void run() {
        while (running) {
            try {
                // 从队列获取音频数据
                byte[] pcmData = audioQueue.take();
                
                // PCM转G.711 μ-law
                byte[] ulawData = new byte[pcmData.length / 2];
                for (int i = 0; i < ulawData.length; i++) {
                    // 从16位PCM提取样本
                    short sample = (short) ((pcmData[i * 2] & 0xFF) | ((pcmData[i * 2 + 1] & 0xFF) << 8));
                    ulawData[i] = linearToUlaw(sample);
                }

                // 创建RTP包
                sendRtpPacket(ulawData);
                
            } catch (InterruptedException e) {
                break;
            } catch (Exception e) {
                if (running) {
                    System.err.println("RTP音频转发错误: " + e.getMessage());
                }
            }
        }
    }

    /**
     * 发送RTP包
     */
    private void sendRtpPacket(byte[] payload) throws IOException {
        // RTP头（12字节）
        ByteBuffer rtpHeader = ByteBuffer.allocate(12 + payload.length);
        
        // 版本(2) + 填充(1) + 扩展(1) + CSRC计数(4)
        rtpHeader.put((byte) 0x80);
        
        // 标记(1) + 载荷类型(7)
        rtpHeader.put((byte) PAYLOAD_TYPE);
        
        // 序列号
        rtpHeader.putShort((short) (sequenceNumber++ & 0xFFFF));
        
        // 时间戳
        rtpHeader.putInt((int) timestamp);
        timestamp += payload.length; // 每个样本递增
        
        // SSRC
        rtpHeader.putInt(ssrc);
        
        // 载荷
        rtpHeader.put(payload);
        
        // 发送数据包
        byte[] packetData = rtpHeader.array();
        DatagramPacket packet = new DatagramPacket(
            packetData, packetData.length, 
            remoteAddress, remotePort
        );
        
        socket.send(packet);
    }

    /**
     * 线性PCM转G.711 μ-law (简化算法)
     */
    private byte linearToUlaw(short sample) {
        // 简化的μ-law编码
        int sign = (sample < 0) ? 0x80 : 0x00;
        if (sign != 0) sample = (short) -sample;
        
        if (sample > 32635) sample = 32635;
        
        int exponent = 7;
        int expMask = 0x4000;
        while ((sample & expMask) == 0 && exponent > 0) {
            exponent--;
            expMask >>= 1;
        }
        
        int mantissa = (sample >> (exponent + 3)) & 0x0F;
        int ulaw = sign | (exponent << 4) | mantissa;
        
        return (byte) ~ulaw;
    }
}
