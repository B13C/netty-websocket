package com.britton;

import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.britton.exceptions.HandshakeException;
import com.britton.service.MessageInfo;
import com.britton.service.impl.SendServiceImpl;
import com.britton.util.EnumCode;
import com.britton.util.JEDISUtil;
import com.britton.util.Request;
import com.britton.util.Response;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class WebSocketServerHandler extends SimpleChannelInboundHandler<Object> {

    private static AtomicInteger counter = new AtomicInteger(0);

    private static final String WEB_SOCKET_PATH = "/ws";

    private WebSocketServerHandshaker handShaker;

    private ChannelHandlerContext ctx;

    private String sessionId;

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        log.info("userEventTriggered [{}]", ctx.channel().id());
        super.userEventTriggered(ctx, evt);
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            MessageInfo info = SendServiceImpl.userWatchMap.get(ctx.channel().id().toString());
            if (event.state().equals(IdleState.READER_IDLE)) {
                if (info != null) {
                    if (checkSenderType(info)) {
                        ctx.close();
                    }
                } else {
                    ctx.close();
                }
            } else if (event.state().equals(IdleState.WRITER_IDLE)) {
                if (info != null) {
                    if (checkSenderType(info)) {
                        ctx.channel().writeAndFlush(new TextWebSocketFrame("ping"));
                    }
                } else {
                    ctx.channel().writeAndFlush(new TextWebSocketFrame("ping"));
                }
            } else if (event.state().equals(IdleState.ALL_IDLE)) {
                log.info("------ALL-IDLE------");
            }
        }
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) {
        counter.getAndIncrement();
        log.info("channelRegistered [{}]---------------连接注册:{}", ctx.channel().id(), counter.get());
        SendServiceImpl.register(ctx.channel().id().toString(), new MessageInfo("", "", "", null), "1");
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        super.channelUnregistered(ctx);
        log.info("channelUnregistered [{}]---------------连接注销", ctx.channel().id());
        logout(ctx);
        SendServiceImpl.logout(ctx.channel().id().toString());
        for (Entry<String, MessageInfo> entry : SendServiceImpl.userWatchMap.entrySet()) {
            log.info("---------------{}", entry.getKey());
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("channelActive [{}]", ctx.channel().id());
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        counter.getAndDecrement();
        log.info("channelInactive [{}]---------------连接断开:{}", ctx.channel().id(), counter.get());
        logout(ctx);
        SendServiceImpl.logout(ctx.channel().id().toString());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.info(cause.getMessage());
        ctx.close();
        log.info("exceptionCaught [{}]---------------异常", ctx.channel().id());
        logout(ctx);
        SendServiceImpl.logout(ctx.channel().id().toString());
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        log.info("channelReadComplete [{}]", ctx.channel().id());
        ctx.flush();
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, Object msg) {
        log.info("channelRead0 [{}]", ctx.channel().id());
        if ((msg instanceof FullHttpRequest)) {
            System.out.println("channelRead0 --- FullHttpRequest");
            handleHttpRequest(ctx, (FullHttpRequest) msg);
        } else if ((msg instanceof WebSocketFrame)) {
            handleWebSocketFrame(ctx, (WebSocketFrame) msg);
        }
    }

    /**
     * 处理HTTP请求
     *
     * @param ctx
     * @param request
     */
    private void handleHttpRequest(ChannelHandlerContext ctx, FullHttpRequest request) {
        log.info("handleHttpRequest [{}]", ctx.channel().id());
        if ((!request.decoderResult().isSuccess()) || (!"websocket".equals(request.headers().get("Upgrade")))) {
            sendHttpResponse(ctx, request, new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST));
            return;
        }
        if (request.method() != HttpMethod.GET) {
            sendHttpResponse(ctx, request, new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.FORBIDDEN));
            return;
        }
        WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(getWebSocketLocation(request), null, false);
        this.handShaker = wsFactory.newHandshaker(request);
        if (this.handShaker == null) {
            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
        } else {
            this.handShaker.handshake(ctx.channel(), request);
            this.ctx = ctx;
        }
    }

    /**
     * 上线
     *
     * @param ctx
     * @param request
     * @return
     */
    private Response online(ChannelHandlerContext ctx, Request request) {
        log.info("online [{}]", ctx.channel().id());
        Response response = new Response();
        response.setEventId(request.getEventId());
        response.setStatus(false);
        String channelId = ctx.channel().id().toString();
        if (StringUtils.isEmpty(request.getRequestId())) {
            response.setMessage("唯一标识不能为空");
        } else if (StringUtils.isEmpty(request.getSenderType())) {
            response.setMessage("发送者类型不能为空");
        } else if (StringUtils.isEmpty(request.getUserId())) {
            response.setMessage("用户id不能为空");
        } else if (SendServiceImpl.userWatchMap.containsKey(ctx.channel().id().toString())) {
            if (!"".equals(SendServiceImpl.userWatchMap.get(ctx.channel().id().toString()).getRequestId())) {
                response.setMessage("标识已存在");
                log.info("{}---------------标识已存在", channelId);
            } else if (!SendServiceImpl.register(ctx.channel().id().toString(), new MessageInfo(request.getUserId(), request.getSenderType(), request.getRequestId(), new SendServiceImpl(ctx, "")), "2")) {
                response.setMessage("注册失败");
                log.info("{}---------------注册失败", channelId);
            } else {
                response.setStatus(true);
                response.setMessage("注册成功");
                MessageInfo info = SendServiceImpl.userWatchMap.get(ctx.channel().id().toString());
                if (StringUtils.isNotBlank(info.getUserId())) {
                    if (checkSenderType(info)) {
                        JEDISUtil.publishMsg("login", request.getUserId(), 0);
                        log.info("{}---------------注册成功-----{}", channelId, "login-" + request.getUserId());
                    }
                }
            }
        }
        return response;
    }

    /**
     * 下线
     *
     * @param ctx
     * @param request
     * @return
     */
    private Response offline(ChannelHandlerContext ctx, Request request) {
        log.info("offline [{}]", ctx.channel().id());
        Response response = new Response();
        response.setEventId(request.getEventId());
        response.setStatus(false);
        if (StringUtils.isEmpty(request.getRequestId())) {
            response.setStatus(false);
            response.setMessage("唯一标识不能为空");
        } else {
            SendServiceImpl.logout(ctx.channel().id().toString());
            response.setStatus(true);
            response.setMessage("下线成功");
        }
        return response;
    }

    /**
     * 发送心跳
     *
     * @param ctx
     * @param request
     * @throws Exception
     */
    private void heartSend(ChannelHandlerContext ctx, Request request) throws Exception {
        log.info("heartSend [{}]", ctx.channel().id());
        Response response = new Response();
        response.setEventId(request.getEventId());
        if (StringUtils.isEmpty(request.getRequestId())) {
            response.setStatus(false);
            response.setMessage("唯一标识不能为空");
            sendWebSocket(ctx, response.toJson());
        } else {
            for (Entry<String, MessageInfo> entry : SendServiceImpl.userWatchMap.entrySet()) {
                Request serviceRequest = new Request();
                serviceRequest.setEventId(EnumCode.HEART_SEND.getCode());
                serviceRequest.setRequestId(request.getRequestId());
                serviceRequest.setMessage("{\"msg\":\"success\"}");
                try {
                    entry.getValue().getSendService().send(serviceRequest);
                } catch (Exception e) {
                    log.warn("回调发送消息给客户端异常", e);
                }
            }
        }
    }

    /**
     * 发送消息
     *
     * @param ctx
     * @param request
     * @return
     */
    private Response sendMessage(ChannelHandlerContext ctx, Request request) {
        log.info("sendMessage [{}]", ctx.channel().id());
        Response response = new Response();
        response.setEventId(request.getEventId());
        response.setStatus(false);
        if (StringUtils.isEmpty(request.getRequestId())) {
            response.setMessage("唯一标识不能为空");
        } else if (StringUtils.isEmpty(request.getSenderType())) {
            response.setMessage("发送者类型不能为空");
        } else if (StringUtils.isEmpty(request.getMessage())) {
            response.setMessage("数据对象不能为空");
        } else if (StringUtils.isEmpty(request.getUserId())) {
            response.setMessage("用户id不能为空");
        } else {
            response.setMessage("消息发送失败");
            if (!StringUtils.isEmpty(request.getReceiverId())) {
                for (Entry<String, MessageInfo> entry : SendServiceImpl.userWatchMap.entrySet()) {
                    if (!"".equals(entry.getValue().getUserId())) {
                        if (request.getReceiverId().equals(entry.getValue().getUserId() + "-" + entry.getValue().getSenderType())) {
                            Request req = this.setRequestInfo(request);
                            try {
                                entry.getValue().getSendService().send(req);
                                log.info("单发:{}>>>>>>>{}", entry.getKey(), request.toJson());
                            } catch (Exception e) {
                                log.warn("回调发送消息给客户端异常", e);
                            }
                            response.setStatus(true);
                            response.setMessage("发送消息成功");
                        }
                    }
                }
            }
            if (!StringUtils.isEmpty(request.getReceiverType())) {
                for (Entry<String, MessageInfo> entry : SendServiceImpl.userWatchMap.entrySet()) {
                    if (request.getReceiverType().equals(entry.getValue().getSenderType()) &&
                            ((!request.getUserId().equals(entry.getValue().getUserId())) || (!request.getSenderType().equals(entry.getValue().getSenderType())))
                    ) {
                        Request serviceRequest = this.setRequestInfo(request);
                        try {
                            entry.getValue().getSendService().send(serviceRequest);
                            log.info("群发:{}>>>>>>>{}", entry.getKey(), serviceRequest.toString());
                        } catch (Exception e) {
                            log.warn("回调发送消息给客户端异常", e);
                        }
                    }
                }
                response.setStatus(true);
                response.setMessage("发送消息成功");
            }
        }
        return response;
    }

    /**
     * 处理WebSocket
     *
     * @param ctx
     * @param frame
     */
    private void handleWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame) {
        log.info("handleWebSocketFrame [{}]", ctx.channel().id());
        if ((frame instanceof CloseWebSocketFrame)) {
            this.handShaker.close(ctx.channel(), (CloseWebSocketFrame) frame.retain());
            return;
        }
        if ((frame instanceof io.netty.handler.codec.http.websocketx.PingWebSocketFrame)) {
            ctx.channel().write(new PongWebSocketFrame(frame.content().retain()));
            return;
        }
        if (!(frame instanceof TextWebSocketFrame)) {
            throw new UnsupportedOperationException("只接收TextWebSocketFrame消息");
        }
        if ("PONG".equals(((TextWebSocketFrame) frame).text())) {
            MessageInfo info = SendServiceImpl.userWatchMap.get(ctx.channel().id().toString());
            log.info("--------------------PONG({})", info.getUserId());
            return;
        }
        try {
            log.info("{}======================{}", ctx.channel().id(), frame);
            Request request = Request.create(((TextWebSocketFrame) frame).text());
            Response response = new Response();
            if (EnumCode.ON_LINE.getCode() == request.getEventId()) {
                response = online(ctx, request);
                sendWebSocket(ctx, response.toJson());
                this.sessionId = ctx.channel().id().toString();
            } else if (EnumCode.DOWN_LINE.getCode() == request.getEventId()) {
                response = offline(ctx, request);
                sendWebSocket(ctx, response.toJson());
                ctx.channel().close();
                this.sessionId = ctx.channel().id().toString();
            } else if (EnumCode.SEND_MESSAGE.getCode() == request.getEventId()) {
                response = sendMessage(ctx, request);
                sendWebSocket(ctx, response.toJson());
                this.sessionId = ctx.channel().id().toString();
            } else if (EnumCode.HEART_SEND.getCode() == request.getEventId()) {
                heartSend(ctx, request);
            } else {
                response.setStatus(false);
                response.setMessage("未知请求");
                sendWebSocket(ctx, response.toJson());
            }
        } catch (JSONException jsonException) {
            log.warn("Json解析异常", jsonException);
            Response respObj = new Response();
            respObj.setEventId(0);
            respObj.setStatus(false);
            respObj.setMessage("Json解析异常");
            this.ctx.channel().writeAndFlush(new TextWebSocketFrame(JSONObject.toJSONString(respObj)));
        } catch (Exception exception) {
            log.error("Socket处理异常", exception);
        }
    }

    /**
     * 发送Http相应消息
     *
     * @param ctx
     * @param request
     * @param response
     */
    private static void sendHttpResponse(ChannelHandlerContext ctx, FullHttpRequest request, FullHttpResponse response) {
        log.info("sendHttpResponse [{}]", ctx.channel().id());
        if (response.status().code() != EnumCode.SUCCESS.getCode()) {
            ByteBuf buf = Unpooled.copiedBuffer(response.status().toString(), io.netty.util.CharsetUtil.UTF_8);
            response.content().writeBytes(buf);
            buf.release();
            HttpUtil.setContentLength(response, response.content().readableBytes());
        }
        ChannelFuture f = ctx.channel().writeAndFlush(response);
        if ((!HttpUtil.isKeepAlive(request)) || (response.status().code() != EnumCode.SUCCESS.getCode())) {
            f.addListener(ChannelFutureListener.CLOSE);
        }
    }

    /**
     * 发送WebSocket消息
     *
     * @param ctx
     * @param msg
     * @throws HandshakeException
     */
    public void sendWebSocket(ChannelHandlerContext ctx, String msg) throws HandshakeException {
        log.info("sendWebSocket [{}]", ctx.channel().id());
        if ((ctx == null) || (ctx.isRemoved())) {
            throw new HandshakeException("尚未握手成功，无法向客户端发送WebSocket消息");
        }
        ctx.channel().write(new TextWebSocketFrame(msg));
        ctx.flush();
    }

    /**
     * 获取文本WebSocket的地址
     *
     * @param req
     * @return
     */
    private static String getWebSocketLocation(FullHttpRequest req) {
        return "ws://" + req.headers().get(HttpHeaderNames.HOST) + WEB_SOCKET_PATH;
    }

    /**
     * 推出登录（注销）
     *
     * @param ctx
     */
    private void logout(ChannelHandlerContext ctx) {
        log.info("logout [{}]", ctx.channel().id());
        MessageInfo info = SendServiceImpl.userWatchMap.get(ctx.channel().id().toString());
        if (info != null && StringUtils.isNotBlank(info.getUserId()) && checkSenderType(info)) {
            JEDISUtil.publishMsg("logout", info.getUserId(), 0);
            log.info("{}---------------注销成功-----{}", ctx.channel().id().toString(), "logout-" + info.getUserId());
        }
    }

    /**
     * 设置Request信息
     *
     * @param request
     * @return
     */
    private Request setRequestInfo(Request request) {
        Request serviceRequest = new Request();
        serviceRequest.setRequestId(request.getRequestId());
        serviceRequest.setSenderType(request.getSenderType());
        serviceRequest.setReceiverId(request.getReceiverId());
        serviceRequest.setReceiverType(request.getReceiverType());
        serviceRequest.setEventId(EnumCode.RECEIVE_MESSAGE.getCode());
        serviceRequest.setMessage(request.getMessage());
        return serviceRequest;
    }

    /**
     * 检测发送者的类型
     *
     * @param info
     * @return
     */
    private boolean checkSenderType(MessageInfo info) {
        return ("2".equals(info.getSenderType())) || ("3".equals(info.getSenderType()));
    }
}
