package com.sipex.client.sip;

public interface SipMessageListener {
    void onMessageReceived(String sender, String content);
}

