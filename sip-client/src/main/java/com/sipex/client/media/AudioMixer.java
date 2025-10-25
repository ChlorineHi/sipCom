package com.sipex.client.media;

import javax.sound.sampled.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 音频混音器
 * 将多路音频流混合后播放
 */
public class AudioMixer implements Runnable {
    
    private final List<BlockingQueue<byte[]>> audioQueues;
    private SourceDataLine speaker;
    private volatile boolean running = false;
    
    private static final int SAMPLE_RATE = 8000;
    private static final int FRAME_SIZE = 160; // 20ms @ 8kHz
    private static final int BUFFER_SIZE = FRAME_SIZE * 2; // 16-bit samples
    
    public AudioMixer() {
        this.audioQueues = new ArrayList<>();
        initializeSpeaker();
    }
    
    /**
     * 初始化扬声器
     */
    private void initializeSpeaker() {
        try {
            AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            
            if (!AudioSystem.isLineSupported(info)) {
                throw new RuntimeException("扬声器不支持");
            }
            
            speaker = (SourceDataLine) AudioSystem.getLine(info);
            speaker.open(format, BUFFER_SIZE * 4);
        } catch (Exception e) {
            System.err.println("初始化扬声器失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 添加音频源
     */
    public synchronized int addAudioSource() {
        BlockingQueue<byte[]> queue = new LinkedBlockingQueue<>(100);
        audioQueues.add(queue);
        System.out.println("添加音频源，当前总数: " + audioQueues.size());
        return audioQueues.size() - 1;
    }
    
    /**
     * 移除音频源
     */
    public synchronized void removeAudioSource(int index) {
        if (index >= 0 && index < audioQueues.size()) {
            audioQueues.remove(index);
            System.out.println("移除音频源，剩余: " + audioQueues.size());
        }
    }
    
    /**
     * 添加音频数据到指定源
     */
    public void addAudioData(int sourceIndex, byte[] audioData) {
        if (sourceIndex >= 0 && sourceIndex < audioQueues.size()) {
            BlockingQueue<byte[]> queue = audioQueues.get(sourceIndex);
            if (!queue.offer(audioData)) {
                // 队列满，丢弃旧数据
                queue.poll();
                queue.offer(audioData);
            }
        }
    }
    
    /**
     * 启动混音器
     */
    public void start() {
        if (!running) {
            running = true;
            speaker.start();
            new Thread(this, "Audio-Mixer").start();
            System.out.println("音频混音器已启动");
        }
    }
    
    /**
     * 停止混音器
     */
    public void stop() {
        running = false;
        if (speaker != null) {
            speaker.stop();
            speaker.close();
        }
        System.out.println("音频混音器已停止");
    }
    
    @Override
    public void run() {
        byte[] mixedBuffer = new byte[BUFFER_SIZE];
        
        while (running) {
            try {
                // 清空混音缓冲区
                for (int i = 0; i < mixedBuffer.length; i++) {
                    mixedBuffer[i] = 0;
                }
                
                // 从所有音频源读取数据并混音
                boolean hasData = false;
                synchronized (this) {
                    for (BlockingQueue<byte[]> queue : audioQueues) {
                        byte[] audioData = queue.poll();
                        if (audioData != null && audioData.length == BUFFER_SIZE) {
                            mixAudio(mixedBuffer, audioData);
                            hasData = true;
                        }
                    }
                }
                
                // 播放混音后的音频
                if (hasData && speaker.isOpen()) {
                    speaker.write(mixedBuffer, 0, mixedBuffer.length);
                } else {
                    // 没有数据时，稍作等待
                    Thread.sleep(5);
                }
                
            } catch (Exception e) {
                if (running) {
                    System.err.println("混音错误: " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * 混音算法
     * 将两路音频混合（简单相加后归一化）
     */
    private void mixAudio(byte[] dest, byte[] source) {
        ByteBuffer destBuffer = ByteBuffer.wrap(dest).order(ByteOrder.LITTLE_ENDIAN);
        ByteBuffer sourceBuffer = ByteBuffer.wrap(source).order(ByteOrder.LITTLE_ENDIAN);
        
        int samples = dest.length / 2; // 16-bit = 2 bytes per sample
        
        for (int i = 0; i < samples; i++) {
            // 读取当前样本（16-bit short）
            short destSample = destBuffer.getShort(i * 2);
            short sourceSample = sourceBuffer.getShort(i * 2);
            
            // 混音：简单相加
            int mixed = destSample + sourceSample;
            
            // 防止溢出
            if (mixed > Short.MAX_VALUE) {
                mixed = Short.MAX_VALUE;
            } else if (mixed < Short.MIN_VALUE) {
                mixed = Short.MIN_VALUE;
            }
            
            // 写回混音结果
            destBuffer.putShort(i * 2, (short) mixed);
        }
    }
    
    /**
     * 获取活跃音频源数量
     */
    public synchronized int getSourceCount() {
        return audioQueues.size();
    }
}

