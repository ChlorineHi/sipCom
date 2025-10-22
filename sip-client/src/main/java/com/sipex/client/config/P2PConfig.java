package com.sipex.client.config;

import java.util.concurrent.ConcurrentHashMap;

/**
 * P2P配置 - 用于客户端之间直接通信
 */
public class P2PConfig {
    
    // 存储用户名到SIP地址的映射
    private static final ConcurrentHashMap<String, String> userRegistry = new ConcurrentHashMap<>();
    
    public static void registerUser(String username, String sipAddress, int port) {
        String fullAddress = sipAddress + ":" + port;
        userRegistry.put(username.toLowerCase(), fullAddress);
        System.out.println("P2P注册: " + username + " -> " + fullAddress);
    }
    
    public static String getUserAddress(String username) {
        return userRegistry.get(username.toLowerCase());
    }
    
    public static void printRegistry() {
        System.out.println("=== P2P用户注册表 ===");
        userRegistry.forEach((user, addr) -> {
            System.out.println("  " + user + " -> " + addr);
        });
    }
}

