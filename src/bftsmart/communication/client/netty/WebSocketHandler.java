package bftsmart.communication.client.netty;

import bftsmart.communication.client.CommunicationSystemServerSide;
import bftsmart.reconfiguration.util.TOMConfiguration;
import bftsmart.tom.core.messages.TOMMessage;
import bftsmart.tom.util.Logger;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import java.util.ArrayList;
import java.util.HashMap;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import javax.xml.bind.DatatypeConverter;


public class WebSocketHandler extends ChannelInboundHandlerAdapter {

    private NettyClientServerCommunicationSystemServerSide communicationServer;
    private TOMConfiguration config;

    public WebSocketHandler(CommunicationSystemServerSide communicationSystemServer) {
        super();
        this.communicationServer = (NettyClientServerCommunicationSystemServerSide) communicationSystemServer;
        communicationServer.setWebSocketHandler(this);
        this.config = communicationServer.getController().getStaticConf();
    }

    public void send(ArrayList<WebClientServerSession> webClientReceivers, TOMMessage sm) {

        TOMMessageJSON tomMessageJSON = new TOMMessageJSON(sm);

        // For each client receiver, attach mac or signature (if used), then send message
        for (WebClientServerSession clientSession : webClientReceivers) {

            // Generate and attach MAC to message
            boolean useMacs = config.getUseMACs() == 1;
            String hmac = "";
            if (useMacs) {

                try {

                    // Performance optimization: Save Mac object in connection and reuse
                    Mac macSend = clientSession.getMacSend();

                    if (macSend == null) {
                        // Initialize Mac stuff
                        SecretKeyFactory fac = SecretKeyFactory.getInstance("PBEWithMD5AndDES");
                        String str = clientSession.getClientId() + ":" + config.getProcessId();
                        PBEKeySpec spec = new PBEKeySpec(str.toCharArray());
                        SecretKey authKey = fac.generateSecret(spec);
                        macSend = Mac.getInstance(config.getHmacAlgorithm());
                        macSend.init(authKey);
                        clientSession.setMacSend(macSend);
                    }

                    // Generate the HMAC for the data to be transmitted
                    String dataString = tomMessageJSON.getData().toJSONString();
                    byte[] hmacComputedBytes = macSend.doFinal(dataString.getBytes());
                    String hmacHexString = DatatypeConverter.printHexBinary(hmacComputedBytes);
                    hmac = hmacHexString.toLowerCase();

                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                } catch (InvalidKeySpecException e) {
                    e.printStackTrace();
                } catch (InvalidKeyException e) {
                    e.printStackTrace();
                }
            }

            //  Create the JSON TOM Message and send it to the client. if Mac are used, attach it
            String jsonMsg = useMacs ? tomMessageJSON.attachHMAC(hmac).getJSON() : tomMessageJSON.getJSON();
            clientSession.getCtx().writeAndFlush(new TextWebSocketFrame(jsonMsg));
            Logger.println(" Client <--- Replica | TextWebSocketFrame Sent : " + jsonMsg);
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

                Logger.println(" Client ---> Replica | TextWebSocketFrame Received : " + ((TextWebSocketFrame) msg).text());

                boolean useMacs = config.getUseMACs() == 1;
                TOMMessageJSON tomMessageJSON = new TOMMessageJSON(((TextWebSocketFrame) msg).text(), useMacs);

                try {

                    // Add websocket connection to map of all web clients
                    int sender = tomMessageJSON.getTOMMsg().getSender();
                    WebClientServerSession wcss = new WebClientServerSession(ctx, communicationServer.getController()
                            .getStaticConf().getProcessId(), sender);
                    HashMap<Integer, WebClientServerSession> webClients = communicationServer.getWebClientConnections();

                    if (!webClients.containsValue(wcss)) {
                        webClients.put(new Integer(sender), wcss);
                    }

                    // Compute HMAC and Compare
                    if (useMacs) {

                        // Performance Optimization (Save Mac object in Map) and reuse
                        HashMap<Integer, WebClientServerSession> connections =
                                communicationServer.getWebClientConnections();
                        WebClientServerSession webConnection = connections.get(new Integer(sender));
                        Mac macReceive = webConnection.getMacReceived();

                        if (macReceive == null) {
                            // Init Mac Stuff if not already done
                            SecretKeyFactory fac = SecretKeyFactory.getInstance("PBEWithMD5AndDES");
                            String str = sender + ":" + config.getProcessId();
                            PBEKeySpec spec = new PBEKeySpec(str.toCharArray());
                            SecretKey authKey = fac.generateSecret(spec);
                            macReceive = Mac.getInstance(config.getHmacAlgorithm());
                            macReceive.init(authKey);
                            webConnection.setMacReceived(macReceive);
                        }

                        String hmacReceived = tomMessageJSON.getHMAC();

                        // Compute the hmac of this received data
                        String dataString = tomMessageJSON.getDataString();
                        byte[] hmacComputedBytes = macReceive.doFinal(dataString.getBytes());
                        String hmacComputed = DatatypeConverter.printHexBinary(hmacComputedBytes);
                        hmacComputed = hmacComputed.toLowerCase();

                        // Compare hmac computed with hmac received
                        if (!hmacReceived.equals(hmacComputed)) {

                            System.out.println(" =/= HMACS NOT EQUAL");
                            System.out.println("HMAC RECEIVED " + hmacReceived);
                            System.out.println("HMAC COMPUTED " + hmacComputed);

                            // !!! Break out from method, do not deliver message to TOM Layer
                            return;
                        }

                    }

                    // Create the TOM Message and deliver it to TOM LAYER
                    if (communicationServer.getRequestReceiver() != null) {
                        communicationServer.getRequestReceiver().requestReceived(tomMessageJSON.getTOMMsg());
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