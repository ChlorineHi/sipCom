package com.sipex.client.util;

import javax.sound.sampled.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 音频格式诊断工具 - 帮助诊断和解决音频兼容性问题
 */
public class AudioFormatDiagnostics {
    
    /**
     * 列出所有可用的麦克风
     */
    public static void listAvailableMicrophones() {
        System.out.println("\n========== 可用麦克风列表 ==========");
        Mixer.Info[] mixers = AudioSystem.getMixerInfo();
        
        if (mixers.length == 0) {
            System.out.println("❌ 未找到任何音频混音器");
            return;
        }
        
        for (int i = 0; i < mixers.length; i++) {
            Mixer.Info mixerInfo = mixers[i];
            System.out.println((i + 1) + ". " + mixerInfo.getName());
            
            try {
                Mixer mixer = AudioSystem.getMixer(mixerInfo);
                
                // 列出输入（麦克风）设备
                Line.Info[] targetLines = mixer.getTargetLineInfo();
                if (targetLines.length > 0) {
                    System.out.println("   📝 输入设备: ");
                    for (Line.Info lineInfo : targetLines) {
                        if (lineInfo instanceof DataLine.Info) {
                            System.out.println("      - " + lineInfo);
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("   ⚠️  无法访问: " + e.getMessage());
            }
        }
    }
    
    /**
     * 测试特定的音频格式
     */
    public static void testAudioFormat(AudioFormat format) {
        System.out.println("\n========== 测试音频格式 ==========");
        System.out.println("格式: " + formatToString(format));
        
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
        boolean supported = AudioSystem.isLineSupported(info);
        System.out.println("支持状态: " + (supported ? "✅ 支持" : "❌ 不支持"));
        
        if (supported) {
            try {
                TargetDataLine line = (TargetDataLine) AudioSystem.getLine(info);
                line.open(format);
                System.out.println("✅ 成功打开音频行");
                line.close();
            } catch (Exception e) {
                System.out.println("❌ 打开失败: " + e.getMessage());
            }
        }
    }
    
    /**
     * 扫描所有可能的音频格式并报告支持情况
     */
    public static void scanAllFormats() {
        System.out.println("\n========== 扫描所有支持的音频格式 ==========\n");
        
        float[] sampleRates = {8000, 11025, 16000, 22050, 44100, 48000};
        int[] sampleSizes = {8, 16};
        int[] channels = {1, 2};
        boolean[] endians = {false, true};
        
        int supportedCount = 0;
        
        for (float sampleRate : sampleRates) {
            for (int sampleSize : sampleSizes) {
                for (int channelCount : channels) {
                    for (boolean isBigEndian : endians) {
                        AudioFormat format = new AudioFormat(
                            AudioFormat.Encoding.PCM_SIGNED,
                            sampleRate, sampleSize, channelCount,
                            (sampleSize / 8) * channelCount, sampleRate, isBigEndian
                        );
                        
                        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
                        if (AudioSystem.isLineSupported(info)) {
                            System.out.println("✅ " + formatToString(format));
                            supportedCount++;
                        }
                    }
                }
            }
        }
        
        System.out.println("\n========== 总结 ==========");
        System.out.println("支持的格式数: " + supportedCount);
        
        if (supportedCount == 0) {
            System.out.println("❌ 未找到任何支持的音频格式！");
            System.out.println("建议: 检查音频驱动程序或更新系统驱动");
        }
    }
    
    /**
     * 获取推荐的音频格式列表
     */
    public static List<AudioFormat> getRecommendedFormats() {
        List<AudioFormat> formats = new ArrayList<>();
        
        // 候选格式列表 (优先级从高到低)
        AudioFormat[] candidates = {
            // 8kHz formats (VoIP标准)
            new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 8000f, 16, 1, 2, 8000f, false),
            new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 8000f, 16, 1, 2, 8000f, true),
            new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 8000f, 8, 1, 1, 8000f, false),
            new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 8000f, 8, 1, 1, 8000f, true),
            
            // 16kHz formats (高质量)
            new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 16000f, 16, 1, 2, 16000f, false),
            new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 16000f, 16, 1, 2, 16000f, true),
            new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 16000f, 8, 1, 1, 16000f, false),
            new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 16000f, 8, 1, 1, 16000f, true),
            
            // 44.1kHz / 48kHz formats (备选)
            new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100f, 16, 1, 2, 44100f, false),
            new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 48000f, 16, 1, 2, 48000f, false),
            
            // Stereo formats (最后备选)
            new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 16000f, 16, 2, 4, 16000f, false),
        };
        
        for (AudioFormat format : candidates) {
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            if (AudioSystem.isLineSupported(info)) {
                formats.add(format);
            }
        }
        
        return formats;
    }
    
    /**
     * 将AudioFormat转换为可读字符串
     */
    private static String formatToString(AudioFormat f) {
        return String.format(
            "%.0fHz, %d-bit, %d-ch, %s-endian",
            f.getSampleRate(),
            f.getSampleSizeInBits(),
            f.getChannels(),
            f.isBigEndian() ? "big" : "little"
        );
    }
    
    /**
     * 主诊断入口
     */
    public static void runFullDiagnostics() {
        System.out.println("════════════════════════════════════════");
        System.out.println("   音频系统诊断工具 (Audio Diagnostics)");
        System.out.println("════════════════════════════════════════");
        
        // 1. 列出混音器
        listAvailableMicrophones();
        
        // 2. 扫描所有支持的格式
        scanAllFormats();
        
        // 3. 显示推荐格式
        System.out.println("\n========== 推荐的格式 ==========");
        List<AudioFormat> recommended = getRecommendedFormats();
        if (recommended.isEmpty()) {
            System.out.println("❌ 未找到任何推荐的音频格式");
        } else {
            for (int i = 0; i < Math.min(3, recommended.size()); i++) {
                AudioFormat f = recommended.get(i);
                System.out.println((i + 1) + ". " + formatToString(f));
            }
        }
        
        System.out.println("\n════════════════════════════════════════");
        System.out.println("诊断完成\n");
    }
    
    // 测试入口
    public static void main(String[] args) {
        runFullDiagnostics();
    }
}

