package com.sipex.client.media;

/**
 * 音频数据回调接口
 */
public interface AudioDataCallback {
    /**
     * 当接收到音频数据时调用
     * @param audioData PCM音频数据
     */
    void onAudioData(byte[] audioData);
}
