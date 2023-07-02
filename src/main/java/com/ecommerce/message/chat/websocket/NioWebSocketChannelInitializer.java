package com.ecommerce.message.chat.websocket;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.stream.ChunkedWriteHandler;

public class NioWebSocketChannelInitializer extends ChannelInitializer<SocketChannel> {
    @Override
    protected void initChannel(SocketChannel socketChannel) throws Exception {
        // 设置log监听器
        socketChannel.pipeline().addLast("logging",new LoggingHandler("DEBUG"));
        // 设置解码器
        socketChannel.pipeline().addLast("http-codec",new HttpServerCodec());
        // 聚合器，使用websocket会用到
        socketChannel.pipeline().addLast("aggregator",new HttpObjectAggregator(65535));
        // 用于大数据的分区传输
        socketChannel.pipeline().addLast("http-chunked",new ChunkedWriteHandler());
        // 自定义的业务handler
        socketChannel.pipeline().addLast("handler",new NioWebSocketHandler());
    }
}
