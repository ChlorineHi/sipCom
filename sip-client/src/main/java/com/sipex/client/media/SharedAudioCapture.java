package com.sipex.client.media;

import javax.sound.sampled.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 共享音频采集器
 * 只使用一个麦克风采集线程，将数据分发给多个监听器
 */
public class SharedAudioCapture implements Runnable {
    
    private static SharedAudioCapture instance;
    private final List<AudioDataListener> listeners;
    private TargetDataLine microphone;
    private volatile boolean running = false;
    
    // 音频参数
    private static final int SAMPLE_RATE = 8000;
    private static final int FRAME_SIZE = 160; // 20ms @ 8kHz
    private static final int BUFFER_SIZE = FRAME_SIZE * 2; // 16-bit samples
    
    public interface AudioDataListener {
        void onAudioData(byte[] audioData);
    }
    
    private SharedAudioCapture() {
        this.listeners = new CopyOnWriteArrayList<>();
        initializeMicrophone();
    }
    
    public static synchronized SharedAudioCapture getInstance() {
        if (instance == null) {
            instance = new SharedAudioCapture();
        }
        return instance;
    }
    
    /**
     * 初始化麦克风
     */
    private void initializeMicrophone() {
        try {
            AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            
            if (!AudioSystem.isLineSupported(info)) {
                throw new RuntimeException("麦克风不支持");
            }
            
            microphone = (TargetDataLine) AudioSystem.getLine(info);
            microphone.open(format, BUFFER_SIZE * 4);
        } catch (Exception e) {
            System.err.println("初始化麦克风失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 添加音频数据监听器
     */
    public void addListener(AudioDataListener listener) {
        listeners.add(listener);
        
        // 如果这是第一个监听器，启动采集
        if (listeners.size() == 1 && !running) {
            start();
        }
    }
    
    /**
     * 移除音频数据监听器
     */
    public void removeListener(AudioDataListener listener) {
        listeners.remove(listener);
        
        // 如果没有监听器了，停止采集
        if (listeners.isEmpty() && running) {
            stop();
        }
    }
    
    /**
     * 启动音频采集
     */
    private void start() {
        if (running || microphone == null) {
            return;
        }
        
        running = true;
        microphone.start();
        new Thread(this, "Shared-Audio-Capture").start();
        System.out.println("✅ 共享音频采集器已启动");
    }
    
    /**
     * 停止音频采集
     */
    private void stop() {
        running = false;
        if (microphone != null && microphone.isOpen()) {
            microphone.drain();
            microphone.stop();
        }
        System.out.println("❌ 共享音频采集器已停止");
    }
    
    @Override
    public void run() {
        byte[] buffer = new byte[BUFFER_SIZE];
        
        while (running) {
            try {
                // 从麦克风读取数据
                int bytesRead = microphone.read(buffer, 0, buffer.length);
                
                if (bytesRead > 0) {
                    // 分发给所有监听器
                    byte[] audioData = new byte[bytesRead];
                    System.arraycopy(buffer, 0, audioData, 0, bytesRead);
                    
                    for (AudioDataListener listener : listeners) {
                        try {
                            listener.onAudioData(audioData);
                        } catch (Exception e) {
                            System.err.println("音频数据分发错误: " + e.getMessage());
                        }
                    }
                }
                
            } catch (Exception e) {
                if (running) {
                    System.err.println("音频采集错误: " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * 强制停止（用于应用退出时）
     */
    public void forceStop() {
        listeners.clear();
        stop();
        if (microphone != null && microphone.isOpen()) {
            microphone.close();
        }
    }
}
