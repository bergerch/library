package bftsmart.communication.client.netty;

import bftsmart.reconfiguration.util.TOMConfiguration;
import bftsmart.tom.core.messages.TOMMessage;
import bftsmart.tom.util.Logger;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.xml.bind.DatatypeConverter;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
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
            this.sendTo(clientSession, jsonMsg);
            Logger.println(" Client <--- Replica | TextWebSocketFrame Sent : " + jsonMsg);
        }
    }

    public void sendTo(WebClientServerSession clientSession, String jsonMsg) {

    }
}
