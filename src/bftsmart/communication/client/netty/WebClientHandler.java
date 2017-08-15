package bftsmart.communication.client.netty;

import bftsmart.reconfiguration.util.TOMConfiguration;
import bftsmart.tom.core.messages.TOMMessage;
import bftsmart.tom.util.Logger;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.xml.bind.DatatypeConverter;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import java.util.HashMap;
import java.util.List;

public abstract class WebClientHandler extends ChannelInboundHandlerAdapter {

    protected NettyClientServerCommunicationSystemServerSide communicationServer;
    protected TOMConfiguration config;

    public WebClientHandler(NettyClientServerCommunicationSystemServerSide communicationServer) {
        super();
        this.communicationServer = communicationServer;
        communicationServer.setWebClientHandler(this);
        this.config = communicationServer.getController().getStaticConf();
    }

    /**
     * Sends a TOMMessage to a List of Web clients
     *
     * @param webClientReceivers List of web clients
     * @param sm                 message
     */
    public void send(List<WebClientServerSession> webClientReceivers, TOMMessage sm) {

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
                    String dataString = tomMessageJSON.getDataString();
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
            this.sendTo(clientSession, jsonMsg);
            Logger.println(" Client <--- Replica | TextWebSocketFrame Sent : " + jsonMsg);
        }
    }

    /**
     * Reads a message string an passes it to TOM Layer, if MACs / signatures are valid
     *
     * @param ctx
     * @param msgText
     */
    public void readMessage(ChannelHandlerContext ctx, String msgText) {

        boolean useMacs = config.getUseMACs() == 1;
        TOMMessageJSON tomMessageJSON = new TOMMessageJSON(msgText, useMacs);

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

    }

    /**
     * Sends the json message to a single web client
     *
     * @param clientSession
     * @param jsonMsg
     */
    public void sendTo(WebClientServerSession clientSession, String jsonMsg) {
        // Is overwritten in sub classes!
    }
}
