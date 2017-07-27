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

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.xml.bind.DatatypeConverter;


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

        if(sm.view_change_response) {
            Object obj = TOMUtil.getObject(sm.getContent());
            System.out.println("View change response ");
            //if(obj instanceof View) {
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
                    jsonValue.put("port", port+5);

                    JSONObject addEntry = new JSONObject();
                    addEntry.put("inetaddress", jsonValue);

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
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        JSONObject data = new JSONObject();
        data.put("sender", new Integer(sender));
        data.put("session", new Integer(session));
        data.put("sequence", new Integer(sequence));
        data.put("operationId", new Integer(operationId));
        data.put("viewId", new Integer(view));
        data.put("type", new Integer(type));
        data.put("content", content);


        // TODO Generate a HMAC
        String hmac = "TEST TROLOLLOLO";

        JSONObject msg = new JSONObject();
        msg.put("data", data);
        msg.put("hmac", hmac);

        String jsonMsg = msg.toJSONString();


        System.out.println(" Client <--- Replica | TextWebSocketFrame Sent : " + jsonMsg);

        for (WebClientServerSession wcss: webClientReceivers) {

            wcss.getCtx().writeAndFlush(new TextWebSocketFrame(jsonMsg));
        }

    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        if (msg instanceof WebSocketFrame) {
           //  System.out.println("This is a WebSocket frame");

            if (msg instanceof BinaryWebSocketFrame) {
               //  System.out.println("BinaryWebSocketFrame Received : ");
               //  System.out.println(((BinaryWebSocketFrame) msg).content());
            } else if (msg instanceof TextWebSocketFrame) {

                String jsonMsg = ((TextWebSocketFrame) msg).text();

                System.out.println(" Client ---> Replica | TextWebSocketFrame Received : " + jsonMsg);


                JSONParser parser = new JSONParser();

                try {

                    Object obj = parser.parse(jsonMsg);

                    JSONObject jsonObject = (JSONObject) obj;

                    JSONObject data = (JSONObject) jsonObject.get("data");
                    System.out.println(data + " " + data.toJSONString());
                    String hmac = (String) jsonObject.get("hmac");

                    System.out.println("HMAC Received " + hmac);


                    int sender = ((Long) data.get("sender")).intValue();

                    WebClientServerSession wcss = new WebClientServerSession(ctx,
                            ((NettyClientServerCommunicationSystemServerSide) communicationSystemServer).getController().
                                    getStaticConf().getProcessId());

                    HashMap<Integer, WebClientServerSession> webClients =
                            ((NettyClientServerCommunicationSystemServerSide) communicationSystemServer).getWebClientConnections();

                    if (!webClients.containsValue(wcss)) {
                        webClients.put(new Integer(sender), wcss);
                    }

                    int session = ((Long) data.get("session")).intValue();
                    int sequence = ((Long) data.get("sequence")).intValue();
                    int operationId = ((Long) data.get("operationId")).intValue();
                    int view = ((Long) data.get("viewId")).intValue();
                    TOMMessageType type = TOMMessageType.fromInt(((Long) data.get("type")).intValue());
                    byte[] content = new byte[1];

                    try {

                        JSONObject contentObject = (JSONObject) data.get("content");
                        content = contentObject.toJSONString().getBytes("utf-8");

                    } catch(ClassCastException e) {

                        ByteBuffer b = ByteBuffer.allocate(4);
                        int contentInt = Integer.parseInt(data.get("content").toString());
                        b.putInt(contentInt);
                        content = b.array();

                    } finally {


                        //TODO Compute HMAC and Compare
                        SecretKeyFactory fac = SecretKeyFactory.getInstance("PBEWithMD5AndDES");
                        String str = sender + ":" +  ((NettyClientServerCommunicationSystemServerSide) communicationSystemServer).getController().getStaticConf().getProcessId();
                        PBEKeySpec spec = new PBEKeySpec(str.toCharArray());
                        SecretKey authKey = fac.generateSecret(spec);

                        Mac macReceive = Mac.getInstance(((NettyClientServerCommunicationSystemServerSide) communicationSystemServer).getController().getStaticConf().getHmacAlgorithm());

                        System.out.println("AUTH KEY " + new String(authKey.getEncoded()));
                        macReceive.init(authKey);

                        String dataString = jsonMsg.replace("{\"data\":", "");

                        System.out.println("DATA STRING" + dataString);

                        String[] words = dataString.split(",\"hmac");
                        dataString = words[0];

                        System.out.println("DATA STRING2" + dataString);

                        byte[] hmacComputedBytes = macReceive.doFinal(dataString.getBytes());

                        String hmacHex = DatatypeConverter.printHexBinary(hmacComputedBytes);
                        hmacHex = hmacHex.toLowerCase();
                        System.out.println("HMAC COMPUTED " + hmacHex);

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