package com.sipex.client.sip;

import com.sipex.client.config.ClientConfig;

import javax.sip.*;
import javax.sip.address.Address;
import javax.sip.address.AddressFactory;
import javax.sip.address.SipURI;
import javax.sip.header.*;
import javax.sip.message.MessageFactory;
import javax.sip.message.Request;
import javax.sip.message.Response;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 群聊会议SIP管理器
 * 管理多方SIP呼叫
 */
public class ConferenceSipManager implements SipListener {
    
    private SipStack sipStack;
    private SipProvider sipProvider;
    private AddressFactory addressFactory;
    private HeaderFactory headerFactory;
    private MessageFactory messageFactory;
    private ListeningPoint listeningPoint;
    
    private String username;
    private String password;
    private Address contactAddress;
    
    // 管理多个Dialog（每个参与者一个）
    private final Map<String, Dialog> participantDialogs;
    
    // 回调接口
    private ConferenceCallListener callListener;
    
    private long cseq = 1;
    
    /**
     * 群聊呼叫监听器
     */
    public interface ConferenceCallListener {
        void onParticipantInvited(String username);
        void onParticipantConnected(String username, String sdp);
        void onParticipantDisconnected(String username);
        void onIncomingConferenceCall(String caller, String sdp, ServerTransaction transaction, Request request);
    }
    
    public ConferenceSipManager(ConferenceCallListener callListener) {
        this.callListener = callListener;
        this.participantDialogs = new ConcurrentHashMap<>();
    }
    
    /**
     * 初始化
     */
    public void initialize(String username, String password) throws Exception {
        // 如果已初始化，先清理
        if (sipProvider != null || sipStack != null) {
            shutdown();
        }
        
        this.username = username;
        this.password = password;
        
        // 创建SipFactory
        SipFactory sipFactory = SipFactory.getInstance();
        sipFactory.setPathName("gov.nist");
        
        // 创建SipStack
        Properties properties = new Properties();
        properties.setProperty("javax.sip.STACK_NAME", "ConferenceClient-" + username + "-" + System.currentTimeMillis());
        properties.setProperty("gov.nist.javax.sip.TRACE_LEVEL", "0");
        
        sipStack = sipFactory.createSipStack(properties);
        
        // 创建工厂
        headerFactory = sipFactory.createHeaderFactory();
        addressFactory = sipFactory.createAddressFactory();
        messageFactory = sipFactory.createMessageFactory();
        
        // 创建监听点（使用不同端口避免冲突）
        int port = ClientConfig.LOCAL_SIP_PORT + 100; // 5170
        listeningPoint = sipStack.createListeningPoint(ClientConfig.LOCAL_IP, port, "udp");
        
        // 创建SipProvider
        sipProvider = sipStack.createSipProvider(listeningPoint);
        sipProvider.addSipListener(this);
        
        // 创建Contact地址
        SipURI contactURI = addressFactory.createSipURI(username, 
            listeningPoint.getIPAddress() + ":" + listeningPoint.getPort());
        contactAddress = addressFactory.createAddress(contactURI);
        
        System.out.println("群聊SIP管理器初始化完成: " + username + "@" + 
            listeningPoint.getIPAddress() + ":" + port);
    }
    
    /**
     * 向参与者发起呼叫
     */
    public void inviteParticipant(String targetUsername, String sdp) throws Exception {
        // 创建请求URI
        SipURI requestURI = addressFactory.createSipURI(
            targetUsername, 
            ClientConfig.KAMAILIO_HOST + ":" + ClientConfig.KAMAILIO_PORT
        );
        
        // 创建From头
        SipURI fromURI = addressFactory.createSipURI(username, ClientConfig.SIP_DOMAIN);
        Address fromAddress = addressFactory.createAddress(fromURI);
        fromAddress.setDisplayName(username);
        FromHeader fromHeader = headerFactory.createFromHeader(fromAddress, UUID.randomUUID().toString());
        
        // 创建To头
        SipURI toURI = addressFactory.createSipURI(targetUsername, ClientConfig.SIP_DOMAIN);
        Address toAddress = addressFactory.createAddress(toURI);
        ToHeader toHeader = headerFactory.createToHeader(toAddress, null);
        
        // 创建Via头
        ViaHeader viaHeader = headerFactory.createViaHeader(
            listeningPoint.getIPAddress(),
            listeningPoint.getPort(),
            "udp",
            UUID.randomUUID().toString().substring(0, 8)
        );
        
        // 创建CallId
        CallIdHeader callIdHeader = sipProvider.getNewCallId();
        
        // 创建CSeq
        CSeqHeader cSeqHeader = headerFactory.createCSeqHeader(cseq++, Request.INVITE);
        
        // 创建MaxForwards
        MaxForwardsHeader maxForwards = headerFactory.createMaxForwardsHeader(70);
        
        // 创建INVITE请求
        Request inviteRequest = messageFactory.createRequest(
            requestURI,
            Request.INVITE,
            callIdHeader,
            cSeqHeader,
            fromHeader,
            toHeader,
            Arrays.asList(viaHeader),
            maxForwards
        );
        
        // 添加Contact头
        ContactHeader contactHeader = headerFactory.createContactHeader(contactAddress);
        inviteRequest.addHeader(contactHeader);
        
        // 添加SDP
        ContentTypeHeader contentTypeHeader = headerFactory.createContentTypeHeader("application", "sdp");
        inviteRequest.setContent(sdp, contentTypeHeader);
        
        // 发送请求
        ClientTransaction inviteTransaction = sipProvider.getNewClientTransaction(inviteRequest);
        inviteTransaction.sendRequest();
        
        System.out.println("向 " + targetUsername + " 发起群聊呼叫");
        
        // 通知监听器
        if (callListener != null) {
            callListener.onParticipantInvited(targetUsername);
        }
    }
    
    /**
     * 挂断某个参与者
     */
    public void hangupParticipant(String username) {
        try {
            Dialog dialog = participantDialogs.get(username);
            if (dialog != null) {
                Request byeRequest = dialog.createRequest(Request.BYE);
                ClientTransaction byeTransaction = sipProvider.getNewClientTransaction(byeRequest);
                dialog.sendRequest(byeTransaction);
                
                participantDialogs.remove(username);
                System.out.println("已挂断参与者: " + username);
            }
        } catch (Exception e) {
            System.err.println("挂断参与者失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 挂断所有参与者
     */
    public void hangupAll() {
        for (String username : new ArrayList<>(participantDialogs.keySet())) {
            hangupParticipant(username);
        }
    }
    
    @Override
    public void processRequest(RequestEvent requestEvent) {
        Request request = requestEvent.getRequest();
        ServerTransaction serverTransaction = requestEvent.getServerTransaction();
        
        try {
            if (serverTransaction == null) {
                serverTransaction = sipProvider.getNewServerTransaction(request);
            }
            
            if (request.getMethod().equals(Request.INVITE)) {
                handleIncomingInvite(request, serverTransaction);
            } else if (request.getMethod().equals(Request.BYE)) {
                handleBye(request, serverTransaction);
            } else if (request.getMethod().equals(Request.ACK)) {
                // ACK不需要响应
            }
        } catch (Exception e) {
            System.err.println("处理请求失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void handleIncomingInvite(Request request, ServerTransaction transaction) throws Exception {
        // 获取呼叫者
        FromHeader fromHeader = (FromHeader) request.getHeader(FromHeader.NAME);
        String caller = fromHeader.getAddress().getURI().toString();
        if (caller.contains("sip:")) {
            caller = caller.substring(4);
            if (caller.contains("@")) {
                caller = caller.substring(0, caller.indexOf("@"));
            }
        }
        
        // 获取SDP
        String sdp = new String((byte[]) request.getContent());
        
        System.out.println("收到来自 " + caller + " 的群聊呼叫");
        
        // 通知监听器
        if (callListener != null) {
            callListener.onIncomingConferenceCall(caller, sdp, transaction, request);
        }
    }
    
    private void handleBye(Request request, ServerTransaction transaction) throws Exception {
        // 发送200 OK响应
        Response okResponse = messageFactory.createResponse(Response.OK, request);
        transaction.sendResponse(okResponse);
        
        // 获取对方用户名
        FromHeader fromHeader = (FromHeader) request.getHeader(FromHeader.NAME);
        String caller = fromHeader.getAddress().getURI().toString();
        if (caller.contains("sip:")) {
            caller = caller.substring(4);
            if (caller.contains("@")) {
                caller = caller.substring(0, caller.indexOf("@"));
            }
        }
        
        // 移除Dialog
        participantDialogs.remove(caller);
        
        System.out.println("参与者 " + caller + " 已挂断");
        
        // 通知监听器
        if (callListener != null) {
            callListener.onParticipantDisconnected(caller);
        }
    }
    
    @Override
    public void processResponse(ResponseEvent responseEvent) {
        Response response = responseEvent.getResponse();
        ClientTransaction clientTransaction = responseEvent.getClientTransaction();
        
        if (clientTransaction == null) {
            return;
        }
        
        try {
            int statusCode = response.getStatusCode();
            
            if (statusCode == Response.TRYING || statusCode == Response.RINGING) {
                System.out.println("呼叫中... 状态码: " + statusCode);
            } else if (statusCode == Response.OK) {
                handleOkResponse(response, clientTransaction);
            } else if (statusCode >= 400) {
                System.err.println("呼叫失败: " + statusCode + " " + response.getReasonPhrase());
            }
        } catch (Exception e) {
            System.err.println("处理响应失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void handleOkResponse(Response response, ClientTransaction clientTransaction) throws Exception {
        CSeqHeader cseq = (CSeqHeader) response.getHeader(CSeqHeader.NAME);
        
        if (cseq.getMethod().equals(Request.INVITE)) {
            // 发送ACK
            Dialog dialog = clientTransaction.getDialog();
            Request ackRequest = dialog.createAck(cseq.getSeqNumber());
            dialog.sendAck(ackRequest);
            
            // 获取对方用户名
            ToHeader toHeader = (ToHeader) response.getHeader(ToHeader.NAME);
            String username = toHeader.getAddress().getURI().toString();
            if (username.contains("sip:")) {
                username = username.substring(4);
                if (username.contains("@")) {
                    username = username.substring(0, username.indexOf("@"));
                }
            }
            
            // 保存Dialog
            participantDialogs.put(username, dialog);
            
            // 获取SDP
            String sdp = new String((byte[]) response.getContent());
            
            System.out.println("参与者 " + username + " 已连接");
            
            // 通知监听器
            if (callListener != null) {
                callListener.onParticipantConnected(username, sdp);
            }
        }
    }
    
    @Override
    public void processTimeout(TimeoutEvent timeoutEvent) {
        System.err.println("请求超时");
    }
    
    @Override
    public void processIOException(IOExceptionEvent exceptionEvent) {
        System.err.println("IO异常: " + exceptionEvent.getHost());
    }
    
    @Override
    public void processTransactionTerminated(TransactionTerminatedEvent transactionTerminatedEvent) {
        // 事务终止
    }
    
    @Override
    public void processDialogTerminated(DialogTerminatedEvent dialogTerminatedEvent) {
        // Dialog终止
    }
    
    /**
     * 关闭SIP管理器
     */
    public void shutdown() {
        try {
            hangupAll();
            if (sipProvider != null) {
                sipProvider.removeSipListener(this);
                sipStack.deleteSipProvider(sipProvider);
            }
            if (listeningPoint != null) {
                sipStack.deleteListeningPoint(listeningPoint);
            }
            sipStack.stop();
            System.out.println("群聊SIP管理器已关闭");
        } catch (Exception e) {
            System.err.println("关闭SIP管理器失败: " + e.getMessage());
        }
    }
    
    public String getUsername() {
        return username;
    }
    
    public int getParticipantCount() {
        return participantDialogs.size();
    }
    
    public HeaderFactory getHeaderFactory() {
        return headerFactory;
    }
    
    public MessageFactory getMessageFactory() {
        return messageFactory;
    }
    
    public AddressFactory getAddressFactory() {
        return addressFactory;
    }
}

