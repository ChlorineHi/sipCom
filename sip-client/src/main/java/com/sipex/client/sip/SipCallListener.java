package com.sipex.client.sip;

import javax.sip.ServerTransaction;
import javax.sip.message.Request;

public interface SipCallListener {
    void onIncomingCall(String caller, String sdp, ServerTransaction transaction, Request request);
    void onCallEstablished(String remoteSdp);
    void onRinging();
    void onCallEnded();
}

