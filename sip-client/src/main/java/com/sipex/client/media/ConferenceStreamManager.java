package com.sipex.client.media;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 会议流管理器 - 防止音视频流启动时卡死
 * 使用超时机制和异步初始化确保界面响应
 */
public class ConferenceStreamManager {
    
    private final MediaManager mediaManager;
    private final ExecutorService executor;
    private final long INITIALIZATION_TIMEOUT_MS = 10000; // 10秒超时
    private AtomicBoolean isInitializing = new AtomicBoolean(false);
    
    // 流状态追踪
    private volatile boolean audioStreamActive = false;
    private volatile boolean videoStreamActive = false;
    private volatile Exception lastAudioError = null;
    private volatile Exception lastVideoError = null;
    
    public ConferenceStreamManager(MediaManager mediaManager) {
        this.mediaManager = mediaManager;
        this.executor = Executors.newFixedThreadPool(2, r -> {
            Thread t = new Thread(r, "ConferenceStreamInit");
            t.setDaemon(true);
            return t;
        });
    }
    
    /**
     * 异步启动音频流 - 防止UI阻塞
     */
    public void startAudioStreamAsync(String remoteSdp, Callback callback) {
        if (audioStreamActive) {
            if (callback != null) {
                callback.onAudioStreamStarted(false, "音频流已运行");
            }
            return;
        }
        
        executor.execute(() -> {
            try {
                startAudioStreamWithTimeout(remoteSdp);
                audioStreamActive = true;
                lastAudioError = null;
                
                if (callback != null) {
                    callback.onAudioStreamStarted(true, "音频流已启动");
                }
            } catch (TimeoutException e) {
                lastAudioError = e;
                System.err.println("❌ 音频流启动超时 (10秒)");
                if (callback != null) {
                    callback.onAudioStreamStarted(false, "音频流启动超时");
                }
            } catch (Exception e) {
                lastAudioError = e;
                System.err.println("❌ 音频流启动失败: " + e.getMessage());
                if (callback != null) {
                    callback.onAudioStreamStarted(false, e.getMessage());
                }
            }
        });
    }
    
    /**
     * 异步启动视频流 - 防止UI阻塞
     */
    public void startVideoStreamAsync(String remoteSdp, Callback callback) {
        if (videoStreamActive) {
            if (callback != null) {
                callback.onVideoStreamStarted(false, "视频流已运行");
            }
            return;
        }
        
        executor.execute(() -> {
            try {
                startVideoStreamWithTimeout(remoteSdp);
                videoStreamActive = true;
                lastVideoError = null;
                
                if (callback != null) {
                    callback.onVideoStreamStarted(true, "视频流已启动");
                }
            } catch (TimeoutException e) {
                lastVideoError = e;
                System.err.println("❌ 视频流启动超时 (10秒)");
                if (callback != null) {
                    callback.onVideoStreamStarted(false, "视频流启动超时");
                }
            } catch (Exception e) {
                lastVideoError = e;
                System.err.println("❌ 视频流启动失败: " + e.getMessage());
                if (callback != null) {
                    callback.onVideoStreamStarted(false, e.getMessage());
                }
            }
        });
    }
    
    /**
     * 带超时的音频流启动
     */
    private void startAudioStreamWithTimeout(String remoteSdp) throws TimeoutException, Exception {
        FutureTask<Void> task = new FutureTask<>(() -> {
            mediaManager.startAudioStream(remoteSdp);
            return null;
        });
        
        executor.execute(task);
        
        try {
            task.get(INITIALIZATION_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            task.cancel(true);
            throw e;
        } catch (ExecutionException e) {
            throw (Exception) e.getCause();
        }
    }
    
    /**
     * 带超时的视频流启动
     */
    private void startVideoStreamWithTimeout(String remoteSdp) throws TimeoutException, Exception {
        FutureTask<Void> task = new FutureTask<>(() -> {
            mediaManager.startVideoStream(remoteSdp);
            return null;
        });
        
        executor.execute(task);
        
        try {
            task.get(INITIALIZATION_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            task.cancel(true);
            throw e;
        } catch (ExecutionException e) {
            throw (Exception) e.getCause();
        }
    }
    
    /**
     * 同步启动所有流 - 带超时保护
     */
    public void startAllStreamsAsync(String remoteSdp, StreamInitCallback callback) {
        executor.execute(() -> {
            StreamStatus status = new StreamStatus();
            
            // 启动音频流
            try {
                startAudioStreamWithTimeout(remoteSdp);
                audioStreamActive = true;
                status.audioSuccess = true;
            } catch (Exception e) {
                status.audioError = e.getMessage();
                System.err.println("⚠️  音频流启动失败: " + e.getMessage());
            }
            
            // 启动视频流
            try {
                startVideoStreamWithTimeout(remoteSdp);
                videoStreamActive = true;
                status.videoSuccess = true;
            } catch (Exception e) {
                status.videoError = e.getMessage();
                System.err.println("⚠️  视频流启动失败: " + e.getMessage());
            }
            
            if (callback != null) {
                callback.onStreamInitComplete(status);
            }
        });
    }
    
    /**
     * 停止所有流
     */
    public void stopAllStreams() {
        try {
            mediaManager.stopStreams();
            audioStreamActive = false;
            videoStreamActive = false;
        } catch (Exception e) {
            System.err.println("⚠️  停止流失败: " + e.getMessage());
        }
    }
    
    /**
     * 关闭管理器
     */
    public void shutdown() {
        stopAllStreams();
        executor.shutdownNow();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                System.err.println("⚠️  执行线程池未能及时关闭");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    // ========== Getters ==========
    
    public boolean isAudioStreamActive() {
        return audioStreamActive;
    }
    
    public boolean isVideoStreamActive() {
        return videoStreamActive;
    }
    
    public Exception getLastAudioError() {
        return lastAudioError;
    }
    
    public Exception getLastVideoError() {
        return lastVideoError;
    }
    
    // ========== Callback接口 ==========
    
    public interface Callback {
        void onAudioStreamStarted(boolean success, String message);
        void onVideoStreamStarted(boolean success, String message);
    }
    
    public interface StreamInitCallback {
        void onStreamInitComplete(StreamStatus status);
    }
    
    /**
     * 流初始化状态
     */
    public static class StreamStatus {
        public boolean audioSuccess = false;
        public boolean videoSuccess = false;
        public String audioError = null;
        public String videoError = null;
        
        @Override
        public String toString() {
            return "StreamStatus{" +
                    "audio=" + (audioSuccess ? "✅" : "❌ " + audioError) +
                    ", video=" + (videoSuccess ? "✅" : "❌ " + videoError) +
                    '}';
        }
    }
}

