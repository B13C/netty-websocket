package com.britton;

import com.britton.util.PropertiesUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

@Slf4j
public class WebSocketServer {

    private static final int WEBSOCKET_PORT = Integer.parseInt(PropertiesUtil.getValue("/mpi.properties", "port"));

    public static void main(String[] args) {
        WebSocketServer ws = new WebSocketServer();
        try {
            ws.run();
        } catch (Exception e) {
            log.error("WEBSOCKET SERVER 启动错误", e);
        }
    }

    private void run() throws InterruptedException {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new ChannelInitializer<Channel>() {
                        @Override
                        protected void initChannel(Channel channel) throws Exception {
                            ChannelPipeline pipeline = channel.pipeline();
                            pipeline.addLast("http-codec", new HttpServerCodec());
                            pipeline.addLast("aggregator", new HttpObjectAggregator(65536));
                            pipeline.addLast("http-chunked", new ChunkedWriteHandler());
                            pipeline.addLast("websocket", new WebSocketServerProtocolHandler("/ws"));
                            pipeline.addLast("idle-state", new IdleStateHandler(10, 10, 10, TimeUnit.SECONDS));
                            pipeline.addLast("handler", new WebSocketServerHandler());
                        }
                    }).option(ChannelOption.SO_BACKLOG, 256);

            Channel channel = b.bind(WEBSOCKET_PORT).sync().channel();
            log.info("Web socket server started at port :" + WEBSOCKET_PORT + ".");
            channel.closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
