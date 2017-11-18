package bftsmart.demo.collabEdit;

import bftsmart.tom.MessageContext;
import bftsmart.tom.ReplicaContext;
import bftsmart.tom.ServiceReplica;
import bftsmart.tom.core.messages.TOMMessage;
import bftsmart.tom.core.messages.TOMMessageType;
import bftsmart.tom.server.Replier;
import bftsmart.tom.server.defaultservices.DefaultRecoverable;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedList;

import bftsmart.tom.util.Storage;
import com.sun.management.OperatingSystemMXBean;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.List;

public class CollabEditServer extends DefaultRecoverable implements Replier {

    private ReplicaContext rc;
    ServiceReplica replica = null;

    private String document = "Hello World! This is a test for collaborative editing";
    private String server_shadow = document;

    int[] subscribers = new int[50];
    int subscriptionCount = 0;

    DiffMatchPatch dmp = new DiffMatchPatch();

    JSONParser parser = new JSONParser();

    //** Debug and Performance fields */
    boolean verbose;
    private int interval = 100;
    private int iterations = 0;
    private Storage totalLatency = null;
    private Storage executeLatency = null;
    private float maxTp = -1;
    private long startTime = System.currentTimeMillis();
    private long currentTime = System.currentTimeMillis();
    private long appStartTime = System.currentTimeMillis();
    // private ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
    // private OperatingSystemMXBean opBean = ManagementFactory.getOperatingSystemMXBean();
    int patchesCount;
    double client_latency = -1;
    HashMap<Integer, Integer> simultanWritingClients = new HashMap<>();
    List<String> lines = new LinkedList<String>();
    private long saveStatsTime = System.currentTimeMillis();
    long measureInterval = 1000; // 1 second
    long saveInterval = 30000; // 30 s


    public CollabEditServer(int id, boolean verbose) {
        for (int i = 0; i < subscribers.length; i++) {
            subscribers[i] = -1;
        }
        replica = new ServiceReplica(id, this, this, null, this);
        this.verbose = verbose;
        totalLatency = new Storage(interval);
        executeLatency = new Storage(interval);

        String line = "AppTime,SysTime,Throughput,MAXThroughput,#ClientsWriting,#Patches,ClientLatency,DocLength,CPULoad";
        lines.add(line);

    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Expected <processId>");
            System.exit(-1);
        }
        boolean verbose = true;
        if (args.length > 1) {
            try {
                verbose = Boolean.parseBoolean(args[1]);
            } catch (Exception kA) {
                // Do nothing
            }
        }
        new CollabEditServer(Integer.parseInt(args[0]), verbose);
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

        print(" Reading..." + document);
        return documentToByte(msgCtx.getSender());
    }

    private byte[] executeSingle(byte[] command, MessageContext msgCtx) {

        iterations++;

        /** BEGIN Performance measurement */

        // if (msgCtx != null && msgCtx.getFirstInBatch() != null) {
        // msgCtx.getFirstInBatch().executedTime = System.nanoTime();
        // totalLatency.store(msgCtx.getFirstInBatch().executedTime - msgCtx.getFirstInBatch().receptionTime);
        // }
        float tp = -1;

        Integer i = msgCtx.getSender();
        if (!simultanWritingClients.containsKey(i)) {
            simultanWritingClients.put(i, i);
        }

        currentTime = System.currentTimeMillis();
        if (currentTime > startTime + measureInterval) {

            String line = "";
            System.out.println();
            System.out.println("--- Measurements at " + (((float) ((long) currentTime - appStartTime)) / 1000) + " s ");
            line += currentTime - appStartTime;
            line += ",";
            System.out.println("System Time = " + currentTime);
            line += currentTime + ",";
            tp = (float) (patchesCount * 1000 / (float) (currentTime - startTime));
            line += tp + ",";
            int simWritingClients = simultanWritingClients.size();
            if (tp > maxTp) maxTp = tp;
            line += maxTp + "," + simWritingClients + ",";
            System.out.println("# Clients writing = " + simWritingClients);
            System.out.println(patchesCount + " Patches computed");
            line += patchesCount + ",";
            System.out.println("Throughput = " + tp + " operations/sec (Maximum observed: " + maxTp + " patchesCount/sec)");
            System.out.println("Client latency: " + this.client_latency);
            line += this.client_latency + ",";
            line += this.document.length() + ",";

            // System.out.println("Total latency = " + totalLatency.getAverage(false) / 1000 + " (+/- "+ (long)totalLatency.getDP(false) / 1000 +") us ");
            //float exeT = (float) executeLatency.getAverage(false);
            //System.out.println("Execute Time = "+ exeT);

            try {
                float cpuLoad = (float) getProcessCpuLoad();
                System.out.println("CPU LOAD = " + cpuLoad);
                line += cpuLoad;

            } catch (Exception e) {

            }
            lines.add(line);
            totalLatency.reset();
            executeLatency.reset();
            startTime = System.currentTimeMillis();
            patchesCount = 0;
            simultanWritingClients.clear();

            if (currentTime > saveStatsTime + saveInterval) {
                Path file = Paths.get("/home/bergerch/performance.csv");
                try {
                    Files.write(file, lines, Charset.forName("UTF-8"));
                    System.out.println("File Written!");
                } catch (Exception e) {
                    System.out.println("Could not wrtite file");
                }
                saveStatsTime = currentTime;
            }
        }
        /** END Performance measurement */

        /** BEGIN Application logic  */
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

        int requester = msgCtx.getSender();

        // API: subscribe / unsubscribe / write / read (ordered)
        switch (operation) {
            case "subscribe":
                addSubscriber(msgCtx.getSender(), event);
                print(" Added subscriber " + msgCtx.getSender() + " for event " + event);
                break;
            case "unsubscribe":
                removeSubscriber(msgCtx.getSender(), event);
                print(" Added subscriber " + msgCtx.getSender() + " for event " + event);
                break;
            case "write":
                print(" Writing...");
                // Differential Synchronisation algorithm starts here
                JSONArray data = (JSONArray) jsonObject.get("data");
                // Parse Diff
                LinkedList<DiffMatchPatch.Diff> diffs = CollaborativeUtils.parseJSONArray(data);
                // Create Patch
                LinkedList<DiffMatchPatch.Patch> patch;
                patchesCount++;
                try {
                    patch = dmp.patch_make(this.document, diffs);
                } catch (Exception e) {
                    // Changes could not be applied, notify client about this
                    print("  Exception was thrown in patch_make(), document was not changed");
                    //long appEndtTime = System.currentTimeMillis();
                    //executeLatency.store(appEndtTime-appStartTime);
                    return documentToByte(requester);
                }
                print("----Applying Patch----");
                print(patch.toString());
                // Apply Patch
                this.document = (String) dmp.patch_apply(patch, this.document)[0];
                print("----Patch applied!---");
                // Create Diffs, document_shadow <- updated_document
                edits = dmp.diff_main(this.server_shadow, this.document);
                dmp.diff_cleanupSemantic(edits);
                this.document.replaceAll("\"", "");
                this.server_shadow = this.document;
                print("Document changed to: " + document);
                // Distribute changes to all subscribed clients
                break;
            case "read":
                print(" Reading ordered...");
                // Ordered (!) read-request
                break;
            case "latency-measurement":
                // Client transmitted its latency measurement results
                try {
                    client_latency = (double) jsonObject.get("data");
                } catch (Exception e) {
                    Long l = (Long) jsonObject.get("data"); // Parse error
                    client_latency = l.doubleValue();
                }
                break;
            default:
                print(" INVALID Operation! See Server API");
                break;
        }
        /** END Application logic  */
        //long appEndtTime = System.currentTimeMillis();
        //executeLatency.store(appEndtTime-appStartTime);
        return operation.equals("write") ? CollaborativeUtils.diffsToBytes(edits, requester) : documentToByte(requester);
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
            print("getState called");
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

    private byte[] documentToByte(int requester) {
        String command = "\"operation\":\"read\"";
        String data = "\"data\":\"" + this.document + "\"";
        String issuer = "\"requester\":" + requester;
        String res = "{" + command + "," + data + "," + issuer + "}";
        try {
            return res.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            print("ERROR ENCODING NOT SUPPORTED!!!!!");
            return res.getBytes();
        }
    }


    @Override
    public void manageReply(TOMMessage request, MessageContext msgCtx) {

        String event = msgCtx.getEvent();
        if (request.getReqType() == TOMMessageType.ORDERED_REQUEST)
            request.reply.setEvent("onDocChange");

        if (request.getReqType() == TOMMessageType.UNORDERED_REQUEST) {
            print("Self-Send back");
            int[] replyManagement = new int[1];
            replyManagement[0] = request.getSender();
            rc.getServerCommunicationSystem().send(replyManagement, request.reply);
            return;
        } else {
            print("Group-Send back");
            rc.getServerCommunicationSystem().send(subscribers, request.reply);
        }
        print();
        print("__________________________________________");
        print();

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
        print("Now subscribers are " + printSubscriber());
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
        print("Now subscribers are " + printSubscriber());
    }


    void print() {
        if (verbose)
            System.out.println();
    }

    void print(String s) {
        if (verbose)
            System.out.println(s);
    }

    public static double getProcessCpuLoad() throws Exception {
        OperatingSystemMXBean bean = (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        return bean.getProcessCpuLoad() * 100;
    }

}
