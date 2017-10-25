package bftsmart.demo.collabEdit;

import bftsmart.tom.MessageContext;
import bftsmart.tom.ReplicaContext;
import bftsmart.tom.ServiceReplica;
import bftsmart.tom.core.messages.TOMMessage;
import bftsmart.tom.core.messages.TOMMessageType;
import bftsmart.tom.server.Replier;
import bftsmart.tom.server.defaultservices.DefaultRecoverable;

import java.io.*;
import java.util.LinkedList;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class collabEditServer extends DefaultRecoverable implements Replier {

    private ReplicaContext rc;
    ServiceReplica replica = null;

    private String document = "Hello World!";

    int[] subscribers = new int[100];
    int subscriptionCount = 0;

    DiffMatchPatch dmp = new DiffMatchPatch();

    JSONParser parser = new JSONParser();

    public collabEditServer(int id) {
        replica = new ServiceReplica(id, this, this, null, this);

        for (int i = 0; i < subscribers.length; i++) {
            subscribers[i] = -1;
        }
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Expected <processId>");
            System.exit(-1);
        }
        new bftsmart.demo.collabEdit.collabEditServer(Integer.parseInt(args[0]));
    }

    @Override
    public byte[][] appExecuteBatch(byte[][] commands, MessageContext[] msgCtxs) {

        byte[][] replies = new byte[commands.length][];
        for (int i = 0; i < commands.length; i++) {
            if (msgCtxs != null && msgCtxs[i] != null) {
                replies[i] = executeSingle(commands[i], msgCtxs[i]);
            } else replies[i] = executeSingle(commands[i], null);
        }

        return replies;
    }

    @Override
    public byte[] appExecuteUnordered(byte[] command, MessageContext msgCtx) {

        System.out.println(" Reading document: " + document);
        return documentToByte();
    }

    private byte[] executeSingle(byte[] command, MessageContext msgCtx) {

        // API: Subscribe / Unsubscribe / Write / Read (ordered)
        try {
            Object obj = parser.parse(new String(command));
            JSONObject jsonObject = (JSONObject) obj;
            String operation = (String) jsonObject.get("operation");
            String event = msgCtx.getEvent();
            switch (operation) {
                case "subscribe":
                    addSubscriber(msgCtx.getSender(), event);
                    System.out.println(" Added subscriber " + msgCtx.getSender() + " for event " + event);
                    break;
                case "unsubscribe":
                    removeSubscriber(msgCtx.getSender(), event);
                    System.out.println(" Added subscriber " + msgCtx.getSender() + " for event " + event);
                    break;
                case "write":
                    System.out.println(" Write ");
                    JSONArray data = (JSONArray) jsonObject.get("data");
                    System.out.println(" Apply Patch to document " + data);

                    LinkedList<DiffMatchPatch.Diff> diffs = new LinkedList<DiffMatchPatch.Diff>();
                    for (Object a : data) {
                       long method =  (long) ((JSONArray) a).get(0);
                       String text = ((String) ((JSONArray) a).get(1));
                       System.out.println("Diff method: "+ method+ " text " + text);
                       DiffMatchPatch.Operation op = DiffMatchPatch.Operation.fromInt((int) method);
                       DiffMatchPatch.Diff diff = new DiffMatchPatch.Diff(op, text);
                       diffs.add(diff);
                    }
                    LinkedList<DiffMatchPatch.Patch> patch = dmp.patch_make(this.document, diffs);
                    this.document = (String) dmp.patch_apply(patch, this.document)[0];
                    System.out.println("Doc changed to: " + document);
                    return documentToByte();
                case "read":
                    // Ordered (!) read-request
                    System.out.println("Reading document " + document);
                    return documentToByte();
            }
        } catch (ParseException e) {
            e.printStackTrace();
        } finally {
            return documentToByte();
        }

    }

    @SuppressWarnings("unchecked")
    @Override
    public void installSnapshot(byte[] state) {

        // FIXME

        try {
            System.out.println("setState called");
            ByteArrayInputStream bis = new ByteArrayInputStream(state);
            ObjectInput in = new ObjectInputStream(bis);
            document = in.readUTF();
            in.close();
            bis.close();
        } catch (Exception e) {
            System.err.println("[ERROR] Error deserializing state: "
                    + e.getMessage());
        }
    }

    @Override
    public byte[] getSnapshot() {

        // FIXME


        try {
            System.out.println("getState called");
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutput out = new ObjectOutputStream(bos);
            out.writeUTF(document);
            out.flush();
            bos.flush();
            out.close();
            bos.close();
            return bos.toByteArray();
        } catch (IOException ioe) {
            System.err.println("[ERROR] Error serializing state: "
                    + ioe.getMessage());
            return "ERROR".getBytes();
        }
    }

    private byte[] documentToByte() {

        String document = "\"" + this.document + "\"";
        try {
            return document.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            System.out.println("ERROR ENCODING NOT SUPPORTED!!!!!");
            return document.getBytes();
        }
    }


    @Override
    public void manageReply(TOMMessage request, MessageContext msgCtx) {

        if (request.getReqType() == TOMMessageType.ORDERED_REQUEST)
            request.reply.setEvent("onDocChange");

        rc.getServerCommunicationSystem().send(subscribers, request.reply);

    }

    @Override
    public void setReplicaContext(ReplicaContext rc) {
        super.setReplicaContext(rc);
        this.rc = rc;
    }

    public String printSubscriber() {

        String s = "[";

        for (int i = 0; i < subscriptionCount; i++) {

            s = s + ", " + subscribers[i];
        }

        s += "]";

        return s;
    }


    private void removeSubscriber(int sender, String event) {


        boolean removed = false;
        for (int i = 0; i < subscriptionCount; i++) {
            if (subscribers[i] == sender) {

                subscribers[i] = i == subscriptionCount - 1 ? -1 : subscribers[i + 1];
                removed = true;
            }
            if (removed) {
                subscribers[i] = i == subscriptionCount - 1 ? -1 : subscribers[i + 1];
            }
        }
        subscriptionCount = removed ? subscriptionCount-- : subscriptionCount;

        System.out.println("Now subscribers are " + printSubscriber());

    }

    private void addSubscriber(int sender, String event) {


        boolean contains = false;
        for (int i = 0; i < subscriptionCount; i++) {
            if (subscribers[i] == sender) {
                contains = true;
            }
        }
        if (!contains) {
            subscribers[subscriptionCount] = sender;
            subscriptionCount++;
        }


        System.out.println("Now subscribers are " + printSubscriber());
    }

}
