package bftsmart.communication.client.netty;

import bftsmart.communication.client.CommunicationSystemServerSide;
import bftsmart.reconfiguration.views.View;
import bftsmart.tom.core.messages.TOMMessage;
import bftsmart.tom.core.messages.TOMMessageType;
import bftsmart.tom.util.TOMUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.FullHttpMessage;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;


import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

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

        int sender = sm.getSender();
        int session = sm.getSession();
        int sequence = sm.getSequence();
        int operationId = sm.getOperationId();
        int view = sm.getViewID();
        int type = sm.getReqType().toInt();
        String content = "";

        System.out.println("VIEW ID " + sm.getViewID());


        System.out.println("View change response: " + sm.view_change_response);
        if(sm.view_change_response) {
            Object obj = TOMUtil.getObject(sm.getContent());
            System.out.println("View change response2");
            //if(obj instanceof View) {
                System.out.println("instanceof view");
                JSONObject viewJSON = new JSONObject();
                viewJSON.put("id", new Integer(((View) obj).getId()));
                viewJSON.put("f", new Integer(((View) obj).getF()));

                int[] processes = ((View) obj).getProcesses();
                JSONArray processesJson = new JSONArray();
                for ( int k = 0; k < processes.length; k++) {
                    processesJson.add(new Integer(processes[k]));
                }
                viewJSON.put("processes", processesJson);

                Map<Integer, InetSocketAddress> map = ((View) obj).getAddresses();


               JSONArray jsonArray = new JSONArray();
                for (Map.Entry e: map.entrySet()) {

                    Integer i = (Integer) e.getKey();
                    InetSocketAddress addr = (InetSocketAddress) e.getValue();
                    String address = addr.getHostName();
                    Integer port = new Integer(addr.getPort());

                    JSONObject jsonValue = new JSONObject();
                    jsonValue.put("address", address);
                    jsonValue.put("port", port);

                    JSONObject addEntry = new JSONObject();
                    addEntry.put(i, jsonValue);

                    jsonArray.add(addEntry);
                    // }
                }

            viewJSON.put("addresses", jsonArray);

            String jsonString = viewJSON.toJSONString();
            System.out.println(jsonString);
            content = content + jsonString;

        } else {
            // FIXME do not assume int
            byte[] reply = sm.getContent();
            try {
                int newValue = new DataInputStream(new ByteArrayInputStream(reply)).readInt();
                content = content + newValue;
                //System.outprintln(newValue);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        JSONObject msg = new JSONObject();
        msg.put("sender", new Integer(sender));
        msg.put("session", new Integer(session));
        msg.put("sequence", new Integer(sequence));
        msg.put("operationId", new Integer(operationId));
        msg.put("viewId", new Integer(view));
        msg.put("type", new Integer(type));
        msg.put("content", content);

        String jsonMsg = msg.toJSONString();

        for (WebClientServerSession wcss: webClientReceivers) {
            // System.out.println(" ---> Sending JSON to client " + jsonMsg);
            wcss.getCtx().writeAndFlush(new TextWebSocketFrame(jsonMsg));
        }

    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        if (msg instanceof WebSocketFrame) {
           //  System.out.println("This is a WebSocket frame");
            // System.out.println("Client Channel : " + ctx.channel());
            if (msg instanceof BinaryWebSocketFrame) {
               //  System.out.println("BinaryWebSocketFrame Received : ");
               //  System.out.println(((BinaryWebSocketFrame) msg).content());
            } else if (msg instanceof TextWebSocketFrame) {

                String jsonMsg = ((TextWebSocketFrame) msg).text();

                // System.out.println(" <--- TextWebSocketFrame Received : " + jsonMsg);


                JSONParser parser = new JSONParser();

                try {

                    Object obj = parser.parse(jsonMsg);

                    JSONObject jsonObject = (JSONObject) obj;

                    int sender = ((Long) jsonObject.get("sender")).intValue();

                    WebClientServerSession wcss = new WebClientServerSession(ctx,
                            ((NettyClientServerCommunicationSystemServerSide) communicationSystemServer).getController().
                                    getStaticConf().getProcessId());

                    HashMap<Integer, WebClientServerSession> webClients =
                            ((NettyClientServerCommunicationSystemServerSide) communicationSystemServer).getWebClientConnections();

                    if (!webClients.containsValue(wcss)) {
                        webClients.put(new Integer(sender), wcss);
                    }

                    int session = ((Long) jsonObject.get("session")).intValue();
                    int sequence = ((Long) jsonObject.get("sequence")).intValue();
                    int operationId = ((Long) jsonObject.get("operationId")).intValue();
                    int view = ((Long) jsonObject.get("viewId")).intValue();
                    TOMMessageType type = TOMMessageType.fromInt(((Long) jsonObject.get("type")).intValue());
                    byte[] content = new byte[1];

                    try {

                        JSONObject contentObject = (JSONObject) jsonObject.get("content");
                        content = contentObject.toJSONString().getBytes("utf-8");

                    } catch(ClassCastException e) {

                        ByteBuffer b = ByteBuffer.allocate(4);
                        int contentInt = Integer.parseInt(jsonObject.get("content").toString());
                        b.putInt(contentInt);
                        content = b.array();

                    } finally {

                        TOMMessage sm = new TOMMessage(sender, session, sequence, operationId, content, view, type);
                        sm.serializedMessage = TOMMessage.messageToBytes(sm);

                        if (((NettyClientServerCommunicationSystemServerSide) communicationSystemServer)
                                .getRequestReceiver() != null) {
                            ((NettyClientServerCommunicationSystemServerSide) communicationSystemServer)
                                    .getRequestReceiver().requestReceived(sm);
                        }

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