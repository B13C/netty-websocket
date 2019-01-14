package com.britton.service.impl;

import com.britton.service.MessageInfo;
import com.britton.service.SendService;
import com.britton.util.EnumCode;
import com.britton.util.Request;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Data
public class SendServiceImpl implements SendService {

    public static final Map<String, MessageInfo> userWatchMap = new ConcurrentHashMap<>();

    private ChannelHandlerContext ctx;

    private String name;

    public SendServiceImpl(ChannelHandlerContext ctx, String name) {
        this.ctx = ctx;
        this.name = name;
    }

    /**
     * 注册心channel
     *
     * @param channelId
     * @param messageInfo
     * @param type
     * @return
     */
    public static boolean register(String channelId, MessageInfo messageInfo, String type) {
        if (StringUtils.isEmpty(channelId)) {
            return false;
        }
        if (userWatchMap.containsKey(channelId)) {
            for (Entry<String, MessageInfo> entry : userWatchMap.entrySet()) {
                if (messageInfo.getRequestId().equals((entry.getValue()).getRequestId())) {
                    return false;
                }
            }
        }
        userWatchMap.put(channelId, messageInfo);
        if ("2".equals(type)) {
            int i = 0;
            for (Entry<String, MessageInfo> entry : userWatchMap.entrySet()) {
                if (!"".equals((entry.getValue()).getRequestId())) {
                    i++;
                }
            }
            log.info("[{}]上线，在线人数:{}", channelId, i);
        }
        return true;
    }

    /**
     * 注销（退出）
     *
     * @param channelId
     * @return
     */
    public static boolean logout(String channelId) {
        if ((StringUtils.isEmpty(channelId)) || (!userWatchMap.containsKey(channelId))) {
            return false;
        }
        userWatchMap.remove(channelId);
        log.info("[{}]下线，在线人数:{}", channelId, userWatchMap.size());
        return true;
    }

    /**
     * 发送信息
     *
     * @param request
     * @throws Exception
     */
    public void send(Request request) throws Exception {
        if ((this.ctx == null) || (this.ctx.isRemoved())) {
            throw new Exception("尚未握手成功，无法向客户端发送WebSocket消息");
        }
        this.ctx.channel().writeAndFlush(new TextWebSocketFrame(request.toJson()));
    }

    /**
     * 关闭连接
     *
     * @throws Exception
     */
    public void close() throws Exception {
        if ((this.ctx == null) || (this.ctx.isRemoved())) {
            throw new Exception("尚未握手成功，无法向客户端发送WebSocket消息");
        }
        this.ctx.close();
    }

    /**
     * 下线通知
     *
     * @param channelId
     */
    public static void notifyDownline(String channelId) {
        for (Entry<String, MessageInfo> entry : userWatchMap.entrySet()) {
            Request serviceRequest = new Request();
            serviceRequest.setEventId(EnumCode.DOWN_LINE.getCode());
            serviceRequest.setRequestId((entry.getValue()).getRequestId());
            try {
                (entry.getValue()).getSendService().send(serviceRequest);
            } catch (Exception e) {
                log.warn("回调发送消息给客户端异常", e);
            }
        }
    }
}
