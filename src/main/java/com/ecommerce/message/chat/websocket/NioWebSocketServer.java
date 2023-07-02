package com.ecommerce.message.chat.websocket;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NioWebSocketServer {
    private void init() {
        log.info("正在启动WebSocket服务器");
        NioEventLoopGroup boss = new NioEventLoopGroup();
        NioEventLoopGroup worker = new NioEventLoopGroup();
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(boss, worker);
            bootstrap.channel(NioServerSocketChannel.class);
            bootstrap.childHandler(new NioWebSocketChannelInitializer());
            Channel channel = bootstrap.bind(8182).sync().channel();
            log.info("websocket服务器启动成功：" + channel);
            channel.closeFuture().sync();
        } catch (InterruptedException e) {
            log.error("运行出错:" + e);
        } finally {
            boss.shutdownGracefully();
            worker.shutdownGracefully();
            log.error("websocket服务器已关闭");
        }
    }

    public static void main(String[] args) {
        NioWebSocketServer nioWebSocketServer = new NioWebSocketServer();
        nioWebSocketServer.init();
    }
}
