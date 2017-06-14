package bftsmart.communication.client.netty;

import bftsmart.communication.client.CommunicationSystemServerSide;
import bftsmart.tom.core.messages.TOMMessage;
import bftsmart.tom.core.messages.TOMMessageType;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.FullHttpMessage;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;


import java.util.ArrayList;
import java.util.Iterator;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;


public class WebSocketHandler extends ChannelInboundHandlerAdapter {

    CommunicationSystemServerSide communicationSystemServer;

    public WebSocketHandler(CommunicationSystemServerSide communicationSystemServer) {
        super();
        this.communicationSystemServer = communicationSystemServer;
        ((NettyClientServerCommunicationSystemServerSide) communicationSystemServer).setWebSocketHandler(this);
    }


    public void send(ArrayList<WebClientServerSession> webClientReceivers, TOMMessage sm) {

        for (WebClientServerSession wcss: webClientReceivers) {

        }

    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        if (msg instanceof WebSocketFrame) {
            System.out.println("This is a WebSocket frame");
            System.out.println("Client Channel : " + ctx.channel());
            if (msg instanceof BinaryWebSocketFrame) {
                System.out.println("BinaryWebSocketFrame Received : ");
                System.out.println(((BinaryWebSocketFrame) msg).content());
            } else if (msg instanceof TextWebSocketFrame) {
                System.out.println("TextWebSocketFrame Received : ");

                String jsonMsg = ((TextWebSocketFrame) msg).text();

                // ctx.channel().write(new TextWebSocketFrame("Message recieved : " + jsonMsg));

                System.out.println(jsonMsg);


                JSONParser parser = new JSONParser();

                try {

                    Object obj = parser.parse(jsonMsg);

                    JSONObject jsonObject = (JSONObject) obj;

                    int sender = ((Long) jsonObject.get("sender")).intValue();
                    int session = ((Long) jsonObject.get("session")).intValue();
                    int sequence = ((Long) jsonObject.get("sequence")).intValue();
                    int operationId = ((Long) jsonObject.get("operationId")).intValue();
                    int view = ((Long) jsonObject.get("viewId")).intValue();
                    TOMMessageType type = TOMMessageType.fromInt(((Long) jsonObject.get("type")).intValue());
                    JSONObject contentObject = (JSONObject) jsonObject.get("content");
                    byte[] content = contentObject.toJSONString().getBytes("utf-8");


                    TOMMessage sm = new TOMMessage(sender, session, sequence, operationId, content, view, type);

                    if (((NettyClientServerCommunicationSystemServerSide) communicationSystemServer)
                            .getRequestReceiver() != null) {
                        ((NettyClientServerCommunicationSystemServerSide) communicationSystemServer)
                                .getRequestReceiver().requestReceived(sm);
                    }


                } catch (Exception e) {
                    e.printStackTrace();
                }


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