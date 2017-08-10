package bftsmart.communication.client.netty;

import bftsmart.communication.client.CommunicationSystemServerSide;
import bftsmart.reconfiguration.util.TOMConfiguration;
import bftsmart.tom.core.messages.TOMMessage;
import bftsmart.tom.util.Logger;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;

import java.util.HashMap;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import javax.xml.bind.DatatypeConverter;


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