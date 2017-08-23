package bftsmart.communication.client.web;

import bftsmart.reconfiguration.views.View;
import bftsmart.tom.core.messages.TOMMessage;
import bftsmart.tom.core.messages.TOMMessageType;
import bftsmart.tom.util.TOMUtil;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.net.InetSocketAddress;
import java.util.Map;


/**
 * This class handles the parsing and conversion of TOMMessage to JSON Objects / JSON String and vice versa
 * Wrapper class for TOMMessage
 */
public class TOMMessageJSON {

    /**
     * JSON representation of TOMMessage
     */
    private JSONObject data;
    private String hmac;

    private TOMMessage tomMsg;

    /**
     * Do not use "data.toJSONString()"
     */
    private String dataString;

    /**
     * Parses the JSON String representation of TOMMessage and creates a TOMMessage object
     *
     * @param jsonMsg JSON String representation of TOMMessage
     * @param useMacs if the JSON String representation of TOMMessage contains a hmac
     */
    public TOMMessageJSON(String jsonMsg, boolean useMacs) {

        JSONParser parser = new JSONParser();

        try {

            // Parse message
            Object obj = parser.parse(jsonMsg);
            JSONObject jsonObject = (JSONObject) obj;
            data = (JSONObject) jsonObject.get("data");

            if (useMacs) {
                hmac = (String) jsonObject.get("hmac");
            }

            // Initialize TOM message fields
            int sender = ((Long) data.get("sender")).intValue();
            int session = ((Long) data.get("session")).intValue();
            int sequence = ((Long) data.get("sequence")).intValue();
            int operationId = ((Long) data.get("operationId")).intValue();
            int view = ((Long) data.get("viewId")).intValue();
            TOMMessageType type = TOMMessageType.fromInt(((Long) data.get("type")).intValue());
            String event = (String) data.get("event");

            // Parse content
            byte[] content = data.get("content").toString().getBytes();

            // Cuts off hmac and compute the data sequence of the received message
            dataString = jsonMsg.replace("{\"data\":", "");
            String[] words = dataString.split(",\"hmac");
            dataString = words[0];

            // Create the TOM Message
            tomMsg = new TOMMessage(sender, session, sequence, operationId, content, view, type, event);
            tomMsg.serializedMessage = TOMMessage.messageToBytes(tomMsg);

        } catch (ParseException e) {
            e.printStackTrace();
        }
    }


    /**
     * Initializes the JSON String representation from a TOMMessage object
     *
     * @param sm the TOMMessage
     */
    public TOMMessageJSON(TOMMessage sm) {

        this.tomMsg = sm;

        // Initialize TOM message fields
        int sender = sm.getSender();
        int session = sm.getSession();
        int sequence = sm.getSequence();
        int operationId = sm.getOperationId();
        int view = sm.getViewID();
        int type = sm.getReqType().toInt();
        String content = "";
        String event = sm.getEvent();

        // IF it's a >>>Reconfiguration message<<< (clients view number was not up to date) create message with new VIEW
        if (sm.view_change_response) {
            System.out.println(">>>VIEW CHANGE RESPONSE<<<");

            Object obj = TOMUtil.getObject(sm.getContent());
            JSONObject viewJSON = new JSONObject();

            // Set new view id and f for client to reconfigure to
            viewJSON.put("id", new Integer(((View) obj).getId()));
            viewJSON.put("f", new Integer(((View) obj).getF()));

            // Set the processed array with proc ids
            int[] processes = ((View) obj).getProcesses();
            JSONArray processesJson = new JSONArray();
            for (int k = 0; k < processes.length; k++) {
                processesJson.add(new Integer(processes[k]));
            }
            viewJSON.put("processes", processesJson);

            // Create the addresses array containing the replica set
            Map<Integer, InetSocketAddress> map = ((View) obj).getAddresses();
            JSONArray jsonArray = new JSONArray();
            for (Map.Entry e : map.entrySet()) {

                Integer i = (Integer) e.getKey();
                InetSocketAddress addr = (InetSocketAddress) e.getValue();
                String address = addr.getHostName();
                Integer port = new Integer(addr.getPort());

                JSONObject jsonValue = new JSONObject();
                jsonValue.put("address", address);
                jsonValue.put("port", port + 5);

                JSONObject addEntry = new JSONObject();
                addEntry.put("inetaddress", jsonValue);

                jsonArray.add(addEntry);

            }

            viewJSON.put("addresses", jsonArray);

            String jsonString = viewJSON.toJSONString();
            content = content + jsonString;

        } else {

            byte[] reply = sm.getContent();
            content = new String(reply);
            //content = DatatypeConverter.printHexBinary(reply);
        }


        if (event != null) {
            dataString = "{\"sequence\":"+sequence+",\"viewId\":"+view+",\"sender\":"+sender+",\"session\":"+session+",\"operationId\":"+operationId+",\"type\":"+type+",\"content\":"+"\""+content+"\""+",\"event\":"+"\""+event+"\""+"}";

        } else {
            dataString = "{\"sequence\":"+sequence+",\"viewId\":"+view+",\"sender\":"+sender+",\"session\":"+session+",\"operationId\":"+operationId+",\"type\":"+type+",\"content\":"+"\""+content+"\""+"}";

        }

        //{"sequence":1,"viewId":0,"sender":3,"session":0,"operationId":-1,"type":0,"content":"a<div><br></div>"}

        // Create JSON TOM Message
        data = new JSONObject();
        data.put("sender", new Integer(sender));
        data.put("session", new Integer(session));
        data.put("sequence", new Integer(sequence));
        data.put("operationId", new Integer(operationId));
        data.put("viewId", new Integer(view));
        data.put("type", new Integer(type));
        data.put("content", content);


    }

    /**
     * Gets the data of a JSON TOM Message as JSON Object
     *
     * @return JSON Object data
     */
    public JSONObject getData() {
        return this.data;
    }

    /**
     * Attach a hmac to the JSON TOM Message
     *
     * @param hmac
     * @return
     */
    public TOMMessageJSON attachHMAC(String hmac) {
        // Create the JSON TOM Message
        this.hmac = hmac;
        return this;
    }

    /**
     * Gets the a JSON TOM Message as JSON String
     *
     * @return JSON String message
     */
    public String getJSON() {
        // Create the JSON TOM Message
        JSONObject msg = new JSONObject();
        msg.put("data", data);

        if (this.hmac != null) {
            msg.put("hmac", hmac);
        }

        String msgString = "{\"data\":"+dataString+",\"hmac\":"+"\""+hmac+"\""+"}";

        return msgString;
    }

    /**
     * gets the TOMMessage object representation
     *
     * @return TOMMessage sm
     */
    public TOMMessage getTOMMsg() {
        return this.tomMsg;
    }

    /**
     * Gets the DataString of the JSON TOMMessage
     * !!! IMPORTANT !!! Always use this method, and never "data.toJSONString()" as it is invariant and does not alter the
     * permutation of the variables within the JSON String! "data.toJSONString()" is not a robust method and will lead
     * to incorrect results when used for creating hmacs
     *
     * @return
     */
    public String getDataString() {
        return this.dataString;
    }

    /**
     * If the JSON String representation of the message has a hmac attached to it, it will be returned, else empty String
     *
     * @return
     */
    public String getHMAC() {
        return this.hmac != null ? this.hmac : "";
    }

    @Override
    public String toString() {
        return this.getJSON();
    }


}
