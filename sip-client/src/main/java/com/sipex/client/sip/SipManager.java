package com.sipex.client.sip;

import com.sipex.client.config.ClientConfig;
import gov.nist.javax.sip.header.UserAgent;

import javax.sip.*;
import javax.sip.address.Address;
import javax.sip.address.AddressFactory;
import javax.sip.address.SipURI;
import javax.sip.header.*;
import javax.sip.message.MessageFactory;
import javax.sip.message.Request;
import javax.sip.message.Response;
import java.util.*;

public class SipManager implements SipListener {

    private SipStack sipStack;
    private SipProvider sipProvider;
    public AddressFactory addressFactory;
    private HeaderFactory headerFactory;
    private MessageFactory messageFactory;
    public ListeningPoint listeningPoint;

    private String username;
    private String password;
    private Address contactAddress;
    private CallIdHeader callIdHeader;

    private Map<String, Dialog> activeDialogs = new HashMap<>();
    private SipCallListener callListener;
    private SipMessageListener messageListener;
    private Dialog currentCallDialog; // 当前通话的Dialog

    private long cseq = 1;

    public SipManager(SipCallListener callListener, SipMessageListener messageListener) {
        this.callListener = callListener;
        this.messageListener = messageListener;
    }

    public void initialize(String username, String password) throws Exception {
        this.username = username;
        this.password = password;

        // 创建SipFactory
        SipFactory sipFactory = SipFactory.getInstance();
        sipFactory.setPathName("gov.nist");

        // 创建SipStack
        Properties properties = new Properties();
        properties.setProperty("javax.sip.STACK_NAME", "SipClient-" + username);
        properties.setProperty("gov.nist.javax.sip.TRACE_LEVEL", "32");
        properties.setProperty("gov.nist.javax.sip.DEBUG_LOG", "siplog_" + username + ".txt");
        properties.setProperty("gov.nist.javax.sip.SERVER_LOG", "sipserver_" + username + ".txt");

        sipStack = sipFactory.createSipStack(properties);

        // 创建工厂
        headerFactory = sipFactory.createHeaderFactory();
        addressFactory = sipFactory.createAddressFactory();
        messageFactory = sipFactory.createMessageFactory();

        // 创建监听点（使用更大的随机范围避免端口冲突）
        int randomPort = ClientConfig.LOCAL_SIP_PORT + new Random().nextInt(5000);
        System.out.println("=== 客户端SIP监听端口: " + randomPort + " ===");
        listeningPoint = sipStack.createListeningPoint(ClientConfig.LOCAL_IP, randomPort, "udp");
        sipProvider = sipStack.createSipProvider(listeningPoint);
        sipProvider.addSipListener(this);

        // 创建联系地址
        SipURI contactURI = addressFactory.createSipURI(username, listeningPoint.getIPAddress());
        contactURI.setPort(listeningPoint.getPort());
        contactAddress = addressFactory.createAddress(contactURI);
    }

    public void register() throws Exception {
        // 创建Request URI
        SipURI requestURI = addressFactory.createSipURI(username, ClientConfig.SIP_DOMAIN);

        // 创建From Header
        SipURI fromURI = addressFactory.createSipURI(username, ClientConfig.SIP_DOMAIN);
        Address fromAddress = addressFactory.createAddress(fromURI);
        fromAddress.setDisplayName(username);
        FromHeader fromHeader = headerFactory.createFromHeader(fromAddress, String.valueOf(System.currentTimeMillis()));

        // 创建To Header
        SipURI toURI = addressFactory.createSipURI(username, ClientConfig.SIP_DOMAIN);
        Address toAddress = addressFactory.createAddress(toURI);
        ToHeader toHeader = headerFactory.createToHeader(toAddress, null);

        // 创建Via Header
        ViaHeader viaHeader = headerFactory.createViaHeader(
                listeningPoint.getIPAddress(),
                listeningPoint.getPort(),
                "udp",
                null);

        // 创建Call-ID
        callIdHeader = sipProvider.getNewCallId();

        // 创建CSeq
        CSeqHeader cseqHeader = headerFactory.createCSeqHeader(cseq++, Request.REGISTER);

        // 创建Max-Forwards
        MaxForwardsHeader maxForwards = headerFactory.createMaxForwardsHeader(70);

        // 创建Contact Header
        ContactHeader contactHeader = headerFactory.createContactHeader(contactAddress);

        // 创建REGISTER请求
        Request request = messageFactory.createRequest(
                requestURI,
                Request.REGISTER,
                callIdHeader,
                cseqHeader,
                fromHeader,
                toHeader,
                Collections.singletonList(viaHeader),
                maxForwards);

        request.addHeader(contactHeader);

        // 设置Expires
        ExpiresHeader expiresHeader = headerFactory.createExpiresHeader(3600);
        request.addHeader(expiresHeader);

        // 发送请求
        sipProvider.sendRequest(request);
        System.out.println("发送REGISTER请求到 " + requestURI);
    }

    public void sendMessage(String toUser, String messageContent) throws Exception {
        // 创建Request URI
        SipURI requestURI = addressFactory.createSipURI(toUser, ClientConfig.SIP_DOMAIN);

        // 创建From Header
        SipURI fromURI = addressFactory.createSipURI(username, ClientConfig.SIP_DOMAIN);
        Address fromAddress = addressFactory.createAddress(fromURI);
        FromHeader fromHeader = headerFactory.createFromHeader(fromAddress, String.valueOf(System.currentTimeMillis()));

        // 创建To Header
        SipURI toURI = addressFactory.createSipURI(toUser, ClientConfig.SIP_DOMAIN);
        Address toAddress = addressFactory.createAddress(toURI);
        ToHeader toHeader = headerFactory.createToHeader(toAddress, null);

        // 创建Via Header
        ViaHeader viaHeader = headerFactory.createViaHeader(
                listeningPoint.getIPAddress(),
                listeningPoint.getPort(),
                "udp",
                null);

        // 创建新的Call-ID
        CallIdHeader messageCallId = sipProvider.getNewCallId();

        // 创建CSeq
        CSeqHeader cseqHeader = headerFactory.createCSeqHeader(1L, Request.MESSAGE);

        // 创建Max-Forwards
        MaxForwardsHeader maxForwards = headerFactory.createMaxForwardsHeader(70);

        // 创建MESSAGE请求
        Request request = messageFactory.createRequest(
                requestURI,
                Request.MESSAGE,
                messageCallId,
                cseqHeader,
                fromHeader,
                toHeader,
                Collections.singletonList(viaHeader),
                maxForwards);

        // 添加Content-Type
        ContentTypeHeader contentTypeHeader = headerFactory.createContentTypeHeader("text", "plain");
        request.setContent(messageContent, contentTypeHeader);

        // 发送请求
        sipProvider.sendRequest(request);
        System.out.println("发送MESSAGE到 " + toUser + ": " + messageContent);
    }

    public void makeCall(String toUser, String sdp) throws Exception {
        // 创建Request URI - 指向Kamailio代理
        SipURI requestURI = addressFactory.createSipURI(toUser, ClientConfig.SIP_DOMAIN);
        requestURI.setPort(5060); // 确保使用Kamailio端口

        // 创建From Header
        SipURI fromURI = addressFactory.createSipURI(username, ClientConfig.SIP_DOMAIN);
        Address fromAddress = addressFactory.createAddress(fromURI);
        FromHeader fromHeader = headerFactory.createFromHeader(fromAddress, String.valueOf(System.currentTimeMillis()));

        // 创建To Header
        SipURI toURI = addressFactory.createSipURI(toUser, ClientConfig.SIP_DOMAIN);
        Address toAddress = addressFactory.createAddress(toURI);
        ToHeader toHeader = headerFactory.createToHeader(toAddress, null);

        // 创建Via Header
        ViaHeader viaHeader = headerFactory.createViaHeader(
                listeningPoint.getIPAddress(),
                listeningPoint.getPort(),
                "udp",
                null);

        // 创建Call-ID
        CallIdHeader inviteCallId = sipProvider.getNewCallId();

        // 创建CSeq
        CSeqHeader cseqHeader = headerFactory.createCSeqHeader(1L, Request.INVITE);

        // 创建Max-Forwards
        MaxForwardsHeader maxForwards = headerFactory.createMaxForwardsHeader(70);

        // 创建Contact Header
        ContactHeader contactHeader = headerFactory.createContactHeader(contactAddress);

        // 创建INVITE请求
        Request request = messageFactory.createRequest(
                requestURI,
                Request.INVITE,
                inviteCallId,
                cseqHeader,
                fromHeader,
                toHeader,
                Collections.singletonList(viaHeader),
                maxForwards);

        request.addHeader(contactHeader);

        // 添加SDP
        ContentTypeHeader contentTypeHeader = headerFactory.createContentTypeHeader("application", "sdp");
        request.setContent(sdp, contentTypeHeader);

        // 发送请求
        ClientTransaction inviteTransaction = sipProvider.getNewClientTransaction(request);
        inviteTransaction.sendRequest();

        System.out.println("发送INVITE到 " + toUser);
    }

    @Override
    public void processRequest(RequestEvent requestEvent) {
        Request request = requestEvent.getRequest();
        ServerTransaction serverTransaction = requestEvent.getServerTransaction();

        System.out.println("收到请求: " + request.getMethod());

        try {
            if (request.getMethod().equals(Request.INVITE)) {
                // 处理来电
                if (serverTransaction == null) {
                    serverTransaction = sipProvider.getNewServerTransaction(request);
                }

                // 发送180 Ringing
                Response ringingResponse = messageFactory.createResponse(Response.RINGING, request);
                serverTransaction.sendResponse(ringingResponse);

                // 提取SDP
                String sdp = new String((byte[]) request.getContent());
                FromHeader fromHeader = (FromHeader) request.getHeader(FromHeader.NAME);
                String caller = ((SipURI) fromHeader.getAddress().getURI()).getUser();

                // 通知上层应用
                if (callListener != null) {
                    callListener.onIncomingCall(caller, sdp, serverTransaction, request);
                }
            } else if (request.getMethod().equals(Request.MESSAGE)) {
                // 处理接收消息
                String content = new String((byte[]) request.getContent());
                FromHeader fromHeader = (FromHeader) request.getHeader(FromHeader.NAME);
                String sender = ((SipURI) fromHeader.getAddress().getURI()).getUser();

                System.out.println("收到消息来自 " + sender + ": " + content);

                // 发送200 OK
                Response okResponse = messageFactory.createResponse(Response.OK, request);
                if (serverTransaction == null) {
                    serverTransaction = sipProvider.getNewServerTransaction(request);
                }
                serverTransaction.sendResponse(okResponse);

                // 通知上层应用
                if (messageListener != null) {
                    messageListener.onMessageReceived(sender, content);
                }
            } else if (request.getMethod().equals(Request.BYE)) {
                // 处理挂断
                Response okResponse = messageFactory.createResponse(Response.OK, request);
                if (serverTransaction == null) {
                    serverTransaction = sipProvider.getNewServerTransaction(request);
                }
                serverTransaction.sendResponse(okResponse);

                if (callListener != null) {
                    callListener.onCallEnded();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void processResponse(ResponseEvent responseEvent) {
        Response response = responseEvent.getResponse();
        System.out.println("收到响应: " + response.getStatusCode());

        try {
            if (response.getStatusCode() == Response.UNAUTHORIZED || response.getStatusCode() == Response.PROXY_AUTHENTICATION_REQUIRED) {
                // 处理认证
                handleAuthentication(responseEvent);
            } else if (response.getStatusCode() == Response.OK) {
                CSeqHeader cseq = (CSeqHeader) response.getHeader(CSeqHeader.NAME);
                if (cseq.getMethod().equals(Request.INVITE)) {
                    // 呼叫成功，发送ACK
                    Dialog dialog = responseEvent.getDialog();
                    currentCallDialog = dialog; // 保存Dialog用于挂断
                    
                    Request ackRequest = dialog.createAck(cseq.getSeqNumber());
                    dialog.sendAck(ackRequest);

                    // 提取SDP
                    String sdp = new String((byte[]) response.getContent());
                    if (callListener != null) {
                        callListener.onCallEstablished(sdp);
                    }
                } else if (cseq.getMethod().equals(Request.REGISTER)) {
                    System.out.println("注册成功");
                }
            } else if (response.getStatusCode() == Response.RINGING) {
                if (callListener != null) {
                    callListener.onRinging();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleAuthentication(ResponseEvent responseEvent) throws Exception {
        Response challenge = responseEvent.getResponse();
        Request oldRequest = responseEvent.getClientTransaction().getRequest();

        // 这里简化处理，实际应该实现完整的Digest认证
        System.out.println("需要认证，但此演示版本未实现完整认证");
    }

    @Override
    public void processTimeout(TimeoutEvent timeoutEvent) {
        System.out.println("请求超时");
    }

    @Override
    public void processIOException(IOExceptionEvent exceptionEvent) {
        System.out.println("IO异常: " + exceptionEvent.toString());
    }

    @Override
    public void processTransactionTerminated(TransactionTerminatedEvent transactionTerminatedEvent) {
        System.out.println("事务终止");
    }

    @Override
    public void processDialogTerminated(DialogTerminatedEvent dialogTerminatedEvent) {
        System.out.println("对话终止");
    }

    /**
     * 设置当前通话的Dialog（用于被叫方）
     */
    public void setCurrentCallDialog(Dialog dialog) {
        this.currentCallDialog = dialog;
    }

    /**
     * 挂断通话 - 发送BYE请求
     */
    public void terminateCall() throws Exception {
        if (currentCallDialog == null) {
            throw new Exception("没有活动的通话");
        }

        // 创建BYE请求
        Request byeRequest = currentCallDialog.createRequest(Request.BYE);
        ClientTransaction byeTransaction = sipProvider.getNewClientTransaction(byeRequest);
        currentCallDialog.sendRequest(byeTransaction);
        
        System.out.println("已发送BYE请求");
        currentCallDialog = null;
    }

    public void shutdown() {
        if (sipStack != null) {
            sipStack.stop();
        }
    }

    public String getUsername() {
        return username;
    }

    public MessageFactory getMessageFactory() {
        return messageFactory;
    }

    public HeaderFactory getHeaderFactory() {
        return headerFactory;
    }
}

