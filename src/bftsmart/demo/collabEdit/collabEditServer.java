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
    private String server_shadow = "Hello World!";

    int[] subscribers = new int[50];
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

        System.out.println(" Reading..." + document);
        return documentToByte();
    }

    private byte[] executeSingle(byte[] command, MessageContext msgCtx) {

        JSONObject jsonObject = new JSONObject();
        String operation = "";
        String event = "";
        LinkedList<DiffMatchPatch.Diff> edits = new LinkedList<>();

        try {
            // Parse command
            Object obj = parser.parse(new String(command));
            jsonObject = (JSONObject) obj;
            operation = (String) jsonObject.get("operation");
            event = msgCtx.getEvent();

        } catch (ParseException e) {
            e.printStackTrace();
        }

        // API: subscribe / unsubscribe / write / read (ordered)
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
                System.out.println(" Writing...");
                // Differential Synchronisation algorithm starts here
                LinkedList<DiffMatchPatch.Diff> diffs = new LinkedList<>();
                JSONArray data = (JSONArray) jsonObject.get("data");
                // Parse Diff
                for (Object a : data) {
                    long method = (long) ((JSONArray) a).get(0);
                    String text = ((String) ((JSONArray) a).get(1));
                    DiffMatchPatch.Operation op = DiffMatchPatch.Operation.fromInt((int) method);
                    DiffMatchPatch.Diff diff = new DiffMatchPatch.Diff(op, text);
                    diffs.add(diff);
                }

                LinkedList<DiffMatchPatch.Patch> patch;
                // Create Patch
                try {
                    patch = dmp.patch_make(this.document, diffs);
                } catch (Exception e) {
                    // Changes could not be applied, notify client about this
                    System.out.println("  Exception was thrown in patch_make(), document was not changed");
                    return documentToByte();
                }

                System.out.println("----Applying Patch----");
                System.out.println(patch);

                // Apply Patch
                this.document = (String) dmp.patch_apply(patch, this.document)[0];
                System.out.println("----Patch applied!---");

                // Create Diffs, document_shadow <- updated_document
                edits = dmp.diff_main(this.server_shadow, this.document);
                dmp.diff_cleanupSemantic(edits);
                this.document.replaceAll("\"", "");
                this.server_shadow = this.document;
                System.out.println("Document changed to: " + document);

                // Distribute changes to all subscribed clients
                break;
            case "read":
                System.out.println(" Reading ordered...");
                // Ordered (!) read-request
                break;
            default:
                System.out.println(" INVALID Operation! See Server API");
                break;
        }

        return operation.equals("write") ? diffsToBytes(edits) : documentToByte();

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

    private byte[] diffsToBytes(LinkedList<DiffMatchPatch.Diff> edits) {
        String dataString = "[";
        int k = 0;
        for (DiffMatchPatch.Diff edit : edits) {
            String a = "[";
            a+=DiffMatchPatch.Operation.toInt(edit.operation)+ ",";
            a+="\""+ edit.text + "\"";
            a+= "]";
            dataString+=a; //FIXME String Builder
            if (k < edits.size()-1) {
                dataString+=",";
            }
            k++;
        }
        dataString+="]";
        String command = "\"operation\":\"write\"";
        String data =  "\"data\":"+ dataString;
        String res = "{"+command+","+data+"}";
        return res.getBytes();
    }

    private byte[] documentToByte() {
        String command = "\"operation\":\"read\"";
        String data =  "\"data\":\""+ this.document+"\"";
        String res = "{"+command+","+data+"}";
        try {
            return res.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            System.out.println("ERROR ENCODING NOT SUPPORTED!!!!!");
            return res.getBytes();
        }
    }


    @Override
    public void manageReply(TOMMessage request, MessageContext msgCtx) {

        String event = msgCtx.getEvent();
        if (request.getReqType() == TOMMessageType.ORDERED_REQUEST)
            request.reply.setEvent("onDocChange");

        if (request.getReqType() == TOMMessageType.UNORDERED_REQUEST) {
            System.out.println("Self-Send back");
            int[] replyManagement = new int[1];
            replyManagement[0] = request.getSender();
            rc.getServerCommunicationSystem().send(replyManagement, request.reply);
            return;
        } else {
            System.out.println("Group-Send back");
            rc.getServerCommunicationSystem().send(subscribers, request.reply);
        }

        System.out.println();
        System.out.println("__________________________________________");
        System.out.println();

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
