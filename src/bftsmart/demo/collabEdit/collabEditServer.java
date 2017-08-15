package bftsmart.demo.collabEdit;

import bftsmart.tom.MessageContext;
import bftsmart.tom.ReplicaContext;
import bftsmart.tom.ServiceReplica;
import bftsmart.tom.server.defaultservices.DefaultRecoverable;

import java.io.*;


public class collabEditServer extends DefaultRecoverable {

    private ReplicaContext rc;
    ServiceReplica replica = null;

    private String document = "Hello World!";

    // int[] subscriber = new int[100];
    // int subscriptionCount = 0;

    public collabEditServer(int id) {
        replica = new ServiceReplica(id, this, this);
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
            } else executeSingle(commands[i], null);
        }

        return replies;
    }

    @Override
    public byte[] appExecuteUnordered(byte[] command, MessageContext msgCtx) {

        System.out.println(" Reading document: " + document);
        return documentToByte();
    }

    private byte[] executeSingle(byte[] command, MessageContext msgCtx) {

        System.out.println(" Apply changes to document " + document);

        try {
            String changedDoc = new String(command, "UTF-8");
            this.document = changedDoc;
            System.out.println("Doc changed to: " + document);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } finally {
            return documentToByte();
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void installSnapshot(byte[] state) {
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
        try {
            return document.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            System.out.println("ERROR ENCODING NOT SUPPORTED!!!!!");
            return document.getBytes();
        }
    }

    /*
    @Override
    public void manageReply(TOMMessage request, MessageContext msgCtx) {
        boolean contains = false;
        for (int i = 0; i < subscriptionCount; i++) {
            if (subscriber[i] == request.getSender()) {
                contains = true;
            }
        }
        if (!contains) {
            subscriber[subscriptionCount] = request.getSender();
            subscriptionCount++;
        }
        rc.getServerCommunicationSystem().send(subscriber, request.reply);
    }

    @Override
    public void setReplicaContext(ReplicaContext rc) {
        this.rc = rc;
    }
    */
}
