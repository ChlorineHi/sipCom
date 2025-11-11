package com.sipex.client.media;

import javax.sound.sampled.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 通用音频初始化器 - 支持多种音频格式和特殊情况
 * 处理没有标准音频设备的系统
 */
public class UniversalAudioInitializer {
    
    /**
     * 尝试获取任何可用的音频行（不限制格式）
     */
    public static TargetDataLine getAnyAvailableMicrophone() {
        System.out.println("\n========== 通用音频初始化 ==========");
        
        // 1. 列出所有混音器
        Mixer.Info[] mixers = AudioSystem.getMixerInfo();
        System.out.println("发现 " + mixers.length + " 个音频混音器");
        
        for (Mixer.Info mixerInfo : mixers) {
            System.out.println("\n🔍 尝试混音器: " + mixerInfo.getName());
            
            try {
                Mixer mixer = AudioSystem.getMixer(mixerInfo);
                
                // 获取所有输入行信息
                Line.Info[] lines = mixer.getSourceLineInfo();
                System.out.println("   输入行数: " + lines.length);
                
                for (Line.Info lineInfo : lines) {
                    if (lineInfo instanceof DataLine.Info) {
                        DataLine.Info dataLineInfo = (DataLine.Info) lineInfo;
                        
                        // 获取支持的格式
                        AudioFormat[] formats = dataLineInfo.getFormats();
                        System.out.println("   支持 " + formats.length + " 种格式");
                        
                        if (formats.length > 0) {
                            // 尝试使用第一个支持的格式
                            for (AudioFormat format : formats) {
                                try {
                                    System.out.println("   ✓ 尝试格式: " + formatToString(format));
                                    TargetDataLine line = (TargetDataLine) AudioSystem.getLine(dataLineInfo);
                                    line.open(format);
                                    System.out.println("   ✅ 成功打开音频行: " + format);
                                    return line;
                                } catch (Exception e) {
                                    System.out.println("   ✗ 失败: " + e.getMessage());
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("   ⚠️  混音器访问失败: " + e.getMessage());
            }
        }
        
        System.out.println("\n========== 检查默认系统行 ==========");
        
        // 2. 尝试获取默认的麦克风而不指定格式
        try {
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, null);
            TargetDataLine line = (TargetDataLine) AudioSystem.getLine(info);
            
            // 获取默认格式
            AudioFormat defaultFormat = line.getFormat();
            System.out.println("✓ 获取默认音频行");
            System.out.println("  默认格式: " + formatToString(defaultFormat));
            
            line.open();
            System.out.println("✅ 成功打开默认音频行");
            return line;
        } catch (Exception e) {
            System.out.println("✗ 默认音频行失败: " + e.getMessage());
        }
        
        System.out.println("\n========== 所有方法均失败 ==========");
        return null;
    }
    
    /**
     * 获取可用的目标数据行（输入设备）
     */
    public static List<TargetDataLine> getAllAvailableInputLines() {
        List<TargetDataLine> lines = new ArrayList<>();
        
        Mixer.Info[] mixers = AudioSystem.getMixerInfo();
        for (Mixer.Info mixerInfo : mixers) {
            try {
                Mixer mixer = AudioSystem.getMixer(mixerInfo);
                Line.Info[] lineInfos = mixer.getSourceLineInfo();
                
                for (Line.Info lineInfo : lineInfos) {
                    try {
                        Line line = mixer.getLine(lineInfo);
                        if (line instanceof TargetDataLine) {
                            lines.add((TargetDataLine) line);
                            System.out.println("✓ 找到输入设备: " + mixerInfo.getName());
                        }
                    } catch (Exception e) {
                        // 跳过无法打开的行
                    }
                }
            } catch (Exception e) {
                // 跳过无法访问的混音器
            }
        }
        
        return lines;
    }
    
    /**
     * 诊断系统音频状态
     */
    public static void diagnoseAudioSystem() {
        System.out.println("\n╔════════════════════════════════════════╗");
        System.out.println("║       系统音频诊断 (System Audio Check)      ║");
        System.out.println("╚════════════════════════════════════════╝\n");
        
        // 1. 检查混音器
        System.out.println("📊 混音器信息:");
        Mixer.Info[] mixers = AudioSystem.getMixerInfo();
        if (mixers.length == 0) {
            System.out.println("❌ 系统未检测到任何音频混音器！");
            return;
        }
        System.out.println("✓ 检测到 " + mixers.length + " 个混音器\n");
        
        int inputDeviceCount = 0;
        int outputDeviceCount = 0;
        
        for (int i = 0; i < mixers.length; i++) {
            Mixer.Info info = mixers[i];
            System.out.println((i + 1) + ". " + info.getName());
            
            try {
                Mixer mixer = AudioSystem.getMixer(info);
                
                // 检查输入设备
                Line.Info[] sourceLines = mixer.getSourceLineInfo();
                if (sourceLines.length > 0) {
                    System.out.println("   📥 输入: " + sourceLines.length + " 个设备");
                    inputDeviceCount += sourceLines.length;
                    
                    for (Line.Info lineInfo : sourceLines) {
                        if (lineInfo instanceof DataLine.Info) {
                            DataLine.Info dataLineInfo = (DataLine.Info) lineInfo;
                            AudioFormat[] formats = dataLineInfo.getFormats();
                            System.out.println("      • " + lineInfo + " (" + formats.length + " 种格式)");
                        }
                    }
                }
                
                // 检查输出设备
                Line.Info[] targetLines = mixer.getTargetLineInfo();
                if (targetLines.length > 0) {
                    System.out.println("   📤 输出: " + targetLines.length + " 个设备");
                    outputDeviceCount += targetLines.length;
                }
            } catch (Exception e) {
                System.out.println("   ⚠️  无法访问: " + e.getMessage());
            }
        }
        
        System.out.println("\n📈 汇总:");
        System.out.println("   总输入设备: " + inputDeviceCount);
        System.out.println("   总输出设备: " + outputDeviceCount);
        
        if (inputDeviceCount == 0) {
            System.out.println("\n❌ 系统没有输入设备（麦克风）！");
            System.out.println("   解决方案:");
            System.out.println("   1. 检查麦克风是否已连接");
            System.out.println("   2. 在设备管理器中检查音频驱动");
            System.out.println("   3. 尝试更新或重装音频驱动");
            System.out.println("   4. 检查 Windows 设置中的隐私设置");
        } else {
            System.out.println("\n✅ 系统有音频输入设备");
        }
        
        System.out.println("\n╚════════════════════════════════════════╝\n");
    }
    
    /**
     * 格式化 AudioFormat 为可读字符串
     */
    private static String formatToString(AudioFormat f) {
        if (f == null) return "null";
        
        return String.format(
            "%.0fHz, %d-bit, %d-ch, %s-endian, %s",
            f.getSampleRate(),
            f.getSampleSizeInBits(),
            f.getChannels(),
            f.isBigEndian() ? "big" : "little",
            f.getEncoding()
        );
    }
    
    /**
     * 测试入口
     */
    public static void main(String[] args) {
        // 运行诊断
        diagnoseAudioSystem();
        
        // 尝试获取任何可用的麦克风
        System.out.println("尝试获取任何可用的麦克风...\n");
        TargetDataLine line = getAnyAvailableMicrophone();
        
        if (line != null) {
            System.out.println("\n✅ 成功获取音频行");
            line.close();
        } else {
            System.out.println("\n❌ 无法获取任何音频行");
        }
    }
}

