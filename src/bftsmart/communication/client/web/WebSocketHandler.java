package bftsmart.communication.client.web;

import bftsmart.communication.client.CommunicationSystemServerSide;
import bftsmart.communication.client.netty.NettyClientServerCommunicationSystemServerSide;
import bftsmart.tom.util.Logger;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;

public class WebSocketHandler extends WebClientHandler {

    public WebSocketHandler(CommunicationSystemServerSide communicationSystemServer) {
        super((NettyClientServerCommunicationSystemServerSide) communicationSystemServer);
    }

    @Override
    public void sendTo(WebClientServerSession clientSession, String jsonMsg) {
        clientSession.getCtx().writeAndFlush(new TextWebSocketFrame(jsonMsg));
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        if (msg instanceof WebSocketFrame) {
            //  System.out.println("This is a WebSocket frame");

            if (msg instanceof BinaryWebSocketFrame) {
                //  System.out.println("BinaryWebSocketFrame Received : ");
                //  System.out.println(((BinaryWebSocketFrame) msg).content());
            } else if (msg instanceof TextWebSocketFrame) {

                Logger.println(" Client ---> Replica | TextWebSocketFrame Received : " + ((TextWebSocketFrame) msg).text());

                String msgText = ((TextWebSocketFrame) msg).text();
                this.readMessage(ctx, msgText);

            } else if (msg instanceof PingWebSocketFrame) {
                System.out.println("PingWebSocketFrame Received : ");
                System.out.println(((PingWebSocketFrame) msg).content());
            } else if (msg instanceof PongWebSocketFrame) {
                System.out.println("PongWebSocketFrame Received : ");
                System.out.println(((PongWebSocketFrame) msg).content());
            } else if (msg instanceof CloseWebSocketFrame) {
                System.out.println("CloseWebSocketFrame Received : ");
                System.out.println("ReasonText :" + ((CloseWebSocketFrame) msg).reasonText());
                System.out.println("StatusCode : " + ((CloseWebSocketFrame) msg).statusCode());
            } else {
                System.out.println("Unsupported WebSocketFrame");
            }

        }
    }
}