package com.sipex.client.config;

public class ClientConfig {
    // 服务器配置
    public static final String SERVER_HOST = "localhost";
    public static final int SERVER_PORT = 8080;
    public static final String SERVER_URL = "http://" + SERVER_HOST + ":" + SERVER_PORT;
    public static final String WS_URL = "ws://" + SERVER_HOST + ":" + SERVER_PORT + "/ws";

    // Kamailio配置
    public static final String KAMAILIO_HOST = "10.129.161.35";
    public static final int KAMAILIO_PORT = 5060;
    public static final String SIP_DOMAIN = KAMAILIO_HOST;

    // 本地SIP配置
    public static String LOCAL_IP = getLocalIP(); // 动态获取本地IP
    public static final int LOCAL_SIP_PORT = 5070; // 客户端监听端口
    
    private static String getLocalIP() {
        try {
            return java.net.InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "127.0.0.1"; // 备用地址
        }
    }

    // RTP端口范围
    public static final int RTP_PORT_START = 10000;
    public static final int RTP_PORT_END = 20000;
}

