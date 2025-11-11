package com.sipex.client.media;

import javax.sound.sampled.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;

/**
 * RTP音频发送器 - 使用JavaSound捕获麦克风并通过RTP发送
 * 修复: 支持多种音频格式降级，增加异常处理和资源清理
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
    private AudioFormat actualFormat; // 实际使用的音频格式
    private int actualSampleRate = 8000; // 实际采样率

    // RTP参数
    private static final int PAYLOAD_TYPE = 0; // PCMU (G.711 μ-law)

    public RtpAudioSender(int localPort, String remoteHost, int remotePort) throws Exception {
        this.socket = new DatagramSocket(localPort);
        this.remoteAddress = InetAddress.getByName(remoteHost);
        this.remotePort = remotePort;
        this.ssrc = (int) (Math.random() * Integer.MAX_VALUE);
        
        // 初始化麦克风 - 支持多种格式降级
        microphone = initializeMicrophone();
        
        if (microphone == null) {
            throw new RuntimeException("无法初始化麦克风：系统不支持任何音频格式");
        }
    }
    
    /**
     * 初始化麦克风 - 尝试多个格式组合
     */
    private TargetDataLine initializeMicrophone() throws LineUnavailableException {
        // 候选格式列表 (优先级从高到低)
        AudioFormat[] formats = {
            // 格式1: 8kHz, 16-bit, mono, little-endian (首选)
            new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 8000f, 16, 1, 2, 8000f, false),
            // 格式2: 8kHz, 16-bit, mono, big-endian
            new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 8000f, 16, 1, 2, 8000f, true),
            
            // 格式3: 8kHz, 8-bit, mono
            new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 8000f, 8, 1, 1, 8000f, false),
            new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 8000f, 8, 1, 1, 8000f, true),
            
            // 格式4: 16kHz, 16-bit, mono (备选)
            new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 16000f, 16, 1, 2, 16000f, false),
            new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 16000f, 16, 1, 2, 16000f, true),
            
            // 格式5: 16kHz, 8-bit, mono (备选)
            new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 16000f, 8, 1, 1, 16000f, false),
            new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 16000f, 8, 1, 1, 16000f, true),
            
            // 格式6: 44.1kHz, 16-bit, mono (备选)
            new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100f, 16, 1, 2, 44100f, false),
            new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100f, 16, 1, 2, 44100f, true),
            
            // 格式7: 立体声作为最后备选
            new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 16000f, 16, 2, 4, 16000f, false),
        };
        
        for (AudioFormat format : formats) {
            try {
                DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
                
                if (AudioSystem.isLineSupported(info)) {
                    TargetDataLine line = (TargetDataLine) AudioSystem.getLine(info);
                    line.open(format);
                    this.actualFormat = format;
                    this.actualSampleRate = (int) format.getSampleRate();
                    System.out.println("✅ 麦克风初始化成功: " + formatToString(format));
                    return line;
                }
            } catch (Exception e) {
                System.out.println("⚠️  格式不支持: " + formatToString(format));
                continue;
            }
        }
        
        return null;
    }
    
    /**
     * 将AudioFormat转换为可读字符串
     */
    private String formatToString(AudioFormat f) {
        return f.getSampleRate() + "Hz, " + f.getSampleSizeInBits() + "-bit, " + 
               f.getChannels() + "-channel, " + (f.isBigEndian() ? "big" : "little") + "-endian";
    }

    public void start() {
        if (microphone == null) {
            System.err.println("❌ 麦克风未初始化");
            return;
        }
        
        running = true;
        try {
            microphone.start();
            new Thread(this, "RTP-Audio-Sender").start();
            System.out.println("✅ RTP音频发送器已启动: " + remoteAddress + ":" + remotePort);
        } catch (Exception e) {
            running = false;
            System.err.println("❌ 启动RTP音频发送器失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void stop() {
        running = false;
        
        try {
            if (microphone != null && microphone.isOpen()) {
                microphone.stop();
                microphone.close();
            }
        } catch (Exception e) {
            System.err.println("⚠️  关闭麦克风时出错: " + e.getMessage());
        }
        
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (Exception e) {
            System.err.println("⚠️  关闭socket时出错: " + e.getMessage());
        }
        
        System.out.println("❌ RTP音频发送器已停止");
    }

    @Override
    public void run() {
        int frameSize = (int)(actualSampleRate / 50); // 20ms的采样数
        byte[] audioBuffer = new byte[frameSize * actualFormat.getFrameSize()];
        
        while (running) {
            try {
                // 从麦克风读取音频数据
                int bytesRead = microphone.read(audioBuffer, 0, audioBuffer.length);
                
                if (bytesRead > 0) {
                    // 转换为G.711 μ-law (适应不同的采样率和位深)
                    byte[] payload = convertToUlaw(audioBuffer, bytesRead);
                    
                    // 构建RTP包
                    byte[] rtpPacket = buildRtpPacket(payload);
                    
                    // 发送RTP包
                    DatagramPacket packet = new DatagramPacket(
                        rtpPacket, rtpPacket.length, remoteAddress, remotePort
                    );
                    socket.send(packet);
                    
                    // 更新RTP头信息
                    sequenceNumber++;
                    timestamp += frameSize;
                    
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
     * 将音频数据转换为G.711 μ-law格式
     */
    private byte[] convertToUlaw(byte[] audioBuffer, int length) {
        int sampleSize = actualFormat.getSampleSizeInBits() / 8;
        int numSamples = length / sampleSize;
        byte[] payload = new byte[numSamples];
        
        for (int i = 0; i < numSamples; i++) {
            short sample = extractSample(audioBuffer, i, sampleSize);
            payload[i] = linearToUlaw(sample);
        }
        
        return payload;
    }
    
    /**
     * 从字节数组中提取样本
     */
    private short extractSample(byte[] buffer, int index, int sampleSize) {
        if (sampleSize == 1) {
            // 8-bit: 直接转换为16-bit
            return (short) (buffer[index] << 8);
        } else {
            // 16-bit: 考虑字节序
            if (actualFormat.isBigEndian()) {
                return (short) ((buffer[index * 2] << 8) | (buffer[index * 2 + 1] & 0xFF));
            } else {
                return (short) ((buffer[index * 2 + 1] << 8) | (buffer[index * 2] & 0xFF));
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
     * 线性PCM转G.711 μ-law (标准算法)
     */
    private byte linearToUlaw(short sample) {
        // 标准G.711 μ-law编码
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
