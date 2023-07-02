package com.ecommerce.message.chat.websocket;

import com.ecommerce.message.chat.global.ChannelSupervise;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;

import static io.netty.handler.codec.http.HttpUtil.isKeepAlive;

/**
 * 自定义处理器
 */
@Slf4j
public class NioWebSocketHandler extends SimpleChannelInboundHandler<Object> {

    private WebSocketServerHandshaker handshaker;

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Object msg) throws Exception {
        log.debug("收到消息："+msg);
        if (msg instanceof FullHttpRequest){
            // 以http请求形式接入，但是走的是websocket
            handleHttpRequest(channelHandlerContext,(FullHttpRequest)msg);

        } else if (msg instanceof WebSocketFrame){
            // 处理websocket客户端的消息
            handleWebSocketFrame(channelHandlerContext,(WebSocketFrame)msg);
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception{
        // 添加连接
        log.debug("客户端加入连接: "+ctx.channel());
        ChannelSupervise.addChannel(ctx.channel());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception{
        log.debug("客户端断开连接: "+ctx.channel());
        ChannelSupervise.removeChannel(ctx.channel());
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception{
       ctx.flush();
    }

    private void handleWebSocketFrame(ChannelHandlerContext ctx,WebSocketFrame frame){
        // 判断是否关闭链路的指令
        if (frame instanceof CloseWebSocketFrame){
            handshaker.close(ctx.channel(),(CloseWebSocketFrame) frame.retain());
            return;
        }
        // 判断是否ping消息
        if (frame instanceof PingWebSocketFrame){
            ctx.channel().write(new PongWebSocketFrame(frame.content().retain()));
            return;
        }
        // 仅支持文本消息，不支持二进制消息
        if (!(frame instanceof TextWebSocketFrame)){
            log.error("本例仅支持文本消息，不支持二进制消息");
            throw new UnsupportedOperationException(String.format(
                    "%s frame types not supported", frame.getClass().getName()));
        }
        // 返回应答消息
        String request = ((TextWebSocketFrame) frame).text();
        log.debug("服务端收到: " + request);
        TextWebSocketFrame tws = new TextWebSocketFrame(new Date().toString() + ctx.channel().id() + ": " + request);

        // 群发
        ChannelSupervise.send2All(tws);
    }

    private void handleHttpRequest(ChannelHandlerContext ctx,FullHttpRequest request){
        // 要求Upgrade为websocket，过滤掉get/post
        if (!request.decoderResult().isSuccess() || (!"websocket".equals(request.headers().get("Upgrade")))){
            sendHttpResponse(ctx,request,new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST));
            return;
        }
        WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory("ws://localhost:8182/websocket",null,false);
        handshaker = wsFactory.newHandshaker(request);
        if (handshaker == null){
            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
        }else {
            handshaker.handshake(ctx.channel(),request);
        }
    }

    /**
     * 拒绝不合法的请求，并返回错误信息
     * @param ctx
     * @param request
     * @param res
     */
    private static void sendHttpResponse(ChannelHandlerContext ctx,FullHttpRequest request,DefaultFullHttpResponse res){
        if (res.status().code() != 200){
            ByteBuf byteBuf = Unpooled.copiedBuffer(res.status().toString(), CharsetUtil.UTF_8);
            request.content().writeBytes(byteBuf);
            byteBuf.release();
        }
        ChannelFuture future = ctx.channel().writeAndFlush(request);
        if (!isKeepAlive(request) || res.status().code() != 200){
            future.addListener(ChannelFutureListener.CLOSE);
        }
    }
}
