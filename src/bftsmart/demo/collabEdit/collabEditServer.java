package bftsmart.demo.collabEdit;

import bftsmart.tom.MessageContext;
import bftsmart.tom.ReplicaContext;
import bftsmart.tom.ServiceReplica;
import bftsmart.tom.core.messages.TOMMessage;
import bftsmart.tom.server.Replier;
import bftsmart.tom.server.defaultservices.DefaultRecoverable;

import java.io.*;


class OperationType {
    public static final int READ = 0; // Read-only operation
    public static final int APPEND = 1; // Appends character set at position x
    public static final int DELETE = 2; // Deletes character set at position x
}


/**
 * Created by bergerch on 14.08.17.
 */
public class collabEditServer extends DefaultRecoverable implements Replier {

    private ReplicaContext rc;

    private String document = "";
    private int revision = 0;

    ServiceReplica replica = null;

    int[] subscriber = new int[100];
    int subscriptionCount = 0;

    public collabEditServer(int id) {
        replica = new ServiceReplica(id, this, this, null, this);
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

        System.out.println(" Reading document at revision " + revision + " with value " + document);
        try {
            return document.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            System.out.println("ERROR ENCODING NOT SUPPORTED!!!!!");
            return "".getBytes();
        }
    }

    private byte[] executeSingle(byte[] command, MessageContext msgCtx) {

        ByteArrayInputStream bis = new ByteArrayInputStream(command);
        String dSet = "";
        try (ObjectInput in = new ObjectInputStream(bis)) {
            int clientRevision = in.readInt();
            int position = in.readInt();
            int method = in.readInt();
            dSet = in.readUTF();
            document = opTransform(method, dSet, position, clientRevision);
            revision++;
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(" Apply changeSet to document at revision " + revision + " with value " + document + "with changeSet " + dSet);

        return document.getBytes();
    }

    private String opTransform(int method, String dSet, int position, int clientRevision) {

        switch (method) {
            case OperationType.READ:
                return document;

            case OperationType.APPEND:
                if (clientRevision == revision) {
                    String first = document.substring(0, position-1);
                    String second = document.substring(position, document.length());
                    first+= dSet;
                    document = first + second;
                } else {
                    // TODO Operation Transformation

                }
            case OperationType.DELETE:
                if (clientRevision == revision) {
                    String first = document.substring(0, position-1);
                    String second = document.substring(position, document.length());
                    second.replace(dSet, "");
                    document = first + second;
                } else {
                    // TODO Operation Transformation

                }
        }

        return document;
    }


    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Expected <processId>");
            System.exit(-1);
        }
        new bftsmart.demo.collabEdit.collabEditServer(Integer.parseInt(args[0]));
    }


    @SuppressWarnings("unchecked")
    @Override
    public void installSnapshot(byte[] state) {
        try {
            System.out.println("setState called");
            ByteArrayInputStream bis = new ByteArrayInputStream(state);
            ObjectInput in = new ObjectInputStream(bis);
            revision = in.readInt();
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
            out.writeInt(revision);
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
}
