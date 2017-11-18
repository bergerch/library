package bftsmart.demo.collabEdit;

import java.awt.*;
import java.io.IOException;

import bftsmart.communication.client.ReplyListener;
import bftsmart.demo.microbenchmarks.ThroughputLatencyClient;
import bftsmart.tom.AsynchServiceProxy;
import bftsmart.tom.RequestContext;
import bftsmart.tom.core.messages.TOMMessage;
import bftsmart.tom.core.messages.TOMMessageType;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.swing.*;
import javax.swing.text.Document;
import javax.swing.text.html.HTMLEditorKit;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Example client that writes on a document
 */
class CollabEditClient implements ReplyListener {

    private AsynchServiceProxy editorProxy;
    private String document = "";
    private String client_shadow;
    // cursorPosition;
    // range;
    private boolean subscribed;
    private String local_change;
    private DiffMatchPatch dmp;
    private JEditorPane jEditorPane;

    JSONParser parser = new JSONParser();

    /**
     * Performance measurement fields, only used for evaluation purpose
     */
    int numberOfOps = 1000000; // How many operations each client executs e.g. the number of requests
    int interval = 2000; // Milliseconds a client waits before sending the next request
    // boolean readOnly = false; // If client should send read-only requests instead of ordered requests
    long lastTime = System.currentTimeMillis();
    boolean measureLatency = false;
    int requestSent = 0;
    int requestReceived = 0;
    Map<Integer, Long> requestsSentTime = new HashMap();
    Map<Integer, Long> requestsReceivedTime = new HashMap();
    double averageLatencyAll = -1;
    int sampleRate = 10;
    int sampleCount = 0;
    int num = 0;
    boolean ui = false;


    public static void main(String[] args) throws IOException, InterruptedException {
        if (args.length < 1) {
            System.out.println("Expected <processId>");
            System.exit(-1);
        }
        boolean verbose = true;
        boolean ui = true;
        boolean measureLatency = false;
        if (args.length > 1) {
            try {
                verbose = Boolean.parseBoolean(args[1]);
                ui = Boolean.parseBoolean(args[2]);
                measureLatency = Boolean.parseBoolean(args[3]);
            } catch (Exception kA) {
                // Do nothing
            }
        }
        System.out.println("Started");
        new CollabEditClient(Integer.parseInt(args[0]), verbose, ui, measureLatency);
    }


    public void run() {

    }

    public CollabEditClient(int id, boolean verbose, boolean ui, boolean measureLatency) {

        editorProxy = new AsynchServiceProxy(id);
        this.ui = ui;
        this.measureLatency = measureLatency;

        if (ui) {
            // create jeditorpane
            jEditorPane = new JEditorPane();

            // make it read-only
            jEditorPane.setEditable(false);

            // create a scrollpane; modify its attributes as desired
            JScrollPane scrollPane = new JScrollPane(jEditorPane);

            // add an html editor kit
            HTMLEditorKit kit = new HTMLEditorKit();
            jEditorPane.setEditorKit(kit);

            // create a document, set it on the jeditorpane, then add the html
            Document doc = kit.createDefaultDocument();
            jEditorPane.setDocument(doc);
            jEditorPane.setText(this.document);

            // now add it all to a frame
            JFrame j = new JFrame("Group Editor");
            j.getContentPane().add(scrollPane, BorderLayout.CENTER);

            // make it easy to close the application
            j.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            // display the frame
            j.setSize(new Dimension(600, 600));

            // pack it, if you prefer
            //j.pack();

            // center the jframe, then make it visible
            j.setLocationRelativeTo(null);
            j.setVisible(true);
        }

        this.dmp = new DiffMatchPatch();

        String subscribeCommand = "{\"operation\": \"subscribe\"}";
        System.out.println("Sending " + subscribeCommand);
        this.editorProxy.invokeAsynchronousSubscription(subscribeCommand.getBytes(), this,
                TOMMessageType.ORDERED_REQUEST, "onDocChange");

        try {
            Thread.sleep(5000);
            autoWritePerofrmanceMeasurement();
        } catch (Exception e) {

        }
    }

    public void autoWritePerofrmanceMeasurement() throws InterruptedException {

        while (num < numberOfOps) {

            num++;

            String shakespeare = "FROM fairest creatures we desire increase," +
                    "That thereby beautys rose might never die, " +
                    "But as the riper should by time decease," +
                    "His tender heir might bear his memory: " +
                    "But thou, contracted to thine own bright eyes," +
                    "Feedst thy lightest flame with self-substantial fuel, " +
                    "Making a famine where abundance lies, " +
                    "Thyself thy foe, to thy sweet self too cruel. " +
                    "Thou that art now the worlds fresh ornament " +
                    "And only herald to the gaudy spring, " +
                    "Within thine own bud buriest thy content" +
                    "And, tender churl, makest waste in niggarding. " +
                    "Pity the world, or else this glutton be, " +
                    "To eat the worlds due, by the grave and thee.";

            this.requestReceived = 0;
            int sequence = -1;
            if (this.requestSent < this.numberOfOps) {
                this.requestSent++;
                String currentDoc = this.document;
                int randomPosition = (int) (Math.random() * currentDoc.length());
                String operationType = Math.random() > 0.47 ? "INSERT" : "DELETE";
                String pre, post;
                switch (operationType) {
                    case "INSERT":
                        pre = currentDoc.substring(0, randomPosition);
                        String insertion = "" + shakespeare.charAt(num % shakespeare.length());
                        post = currentDoc.substring(randomPosition);
                        currentDoc = pre + insertion + post;
                        break;
                    case "DELETE":
                        pre = currentDoc.substring(0, randomPosition);
                        post = currentDoc.substring(randomPosition + 1);
                        currentDoc = pre + post;
                        break;
                }
                this.document = currentDoc;
                LinkedList<DiffMatchPatch.Diff> d = this.dmp.diff_main(this.client_shadow, this.document);
                this.dmp.diff_cleanupSemantic(d);
                this.client_shadow = this.document;
                // Send write command to replica set
                sequence = this.editorProxy.invokeAsynchRequest(CollaborativeUtils.diffsToBytes(d, editorProxy.getProcessId()),
                        this, TOMMessageType.ORDERED_REQUEST);

            }

            if (this.requestSent % 100 == 0) {

                System.out.println(num);
               /*
            long newTime = System.currentTimeMillis();
            long timeDiff = newTime - this.lastTime;
            long time1Req = timeDiff / 100;
            double numReq = ((double) 1000) / time1Req;
            // this.opsPerSecond = Math.floor(numReq);
            this.lastTime = System.currentTimeMillis();
           // this.progress = this.requestSent / this.numberOfOps * 100;
           */
            }

            if (this.measureLatency) {
                this.requestsSentTime.put(sequence, System.currentTimeMillis());
            }

            Thread.sleep(interval);
        }
    }

    private void testServerAPI() {
        byte[] response;

        // Test Read
        String readCommand = "{\"operation\": \"read\"}";
        System.out.println("Sending " + readCommand);
        response = editorProxy.invokeUnordered(readCommand.getBytes());
        System.out.println(new String(response));

        // Test Subscribe
        String subscribeCommand = "{\"operation\": \"subscribe\"}";
        System.out.println("Sending " + subscribeCommand);
        response = editorProxy.invokeOrdered(subscribeCommand.getBytes());
        System.out.println(new String(response));

        // Test Write
        String writeCommand = "{\"operation\": \"write\", \"data\": [[0,\"Hello W\"],[1,\"a\"],[0,\"orld!\"]] }";
        System.out.println("Sending " + writeCommand);
        response = editorProxy.invokeOrdered(writeCommand.getBytes());
        System.out.println(new String(response));
    }

    @Override
    public void replyReceived(RequestContext context, TOMMessage reply) {

        System.out.println("Received TOMMessage");

        JSONObject jsonObject = new JSONObject();
        String operation = "";
        String event = "";
        LinkedList<DiffMatchPatch.Diff> edits = new LinkedList<>();

        try {
            // Parse command
            Object obj = parser.parse(new String(reply.getContent()));
            jsonObject = (JSONObject) obj;
            operation = (String) jsonObject.get("operation");

        } catch (ParseException e) {
            e.printStackTrace();
        }

        switch (operation) {
            case "read":
                this.document = (String) jsonObject.get("data");
                this.client_shadow = this.document;
                if (ui)
                    this.jEditorPane.setText(document);
                break;
            case "write":
                /// IfDef Performance measurement
                int requester = ((Long) jsonObject.get("requester")).intValue();
                if (requester == editorProxy.getProcessId()) {
                    this.requestReceived++;
                    this.sampleCount++;
                    this.requestsReceivedTime.put(reply.getSequence(), System.currentTimeMillis());
                    if (this.measureLatency && this.sampleCount % this.sampleRate == 0) {
                        this.averageLatencyAll = this.computeStatistic(this.sampleCount - this.sampleRate);
                        // {operation:"latency-measurement", data: this.averageLatencyAll}
                        String op = "{\"operation\":\"latency-measurement\",\"data\":" + this.averageLatencyAll + "}";
                        this.editorProxy.invokeAsynchRequest(op.getBytes(), this, TOMMessageType.ORDERED_REQUEST);
                    }
                }

                // Differential Synchronisation algorithm starts here
                JSONArray data = (JSONArray) jsonObject.get("data");
                // Parse Diff
                LinkedList<DiffMatchPatch.Diff> diffs = CollaborativeUtils.parseJSONArray(data);
                LinkedList<DiffMatchPatch.Patch> patch;
                // Create Patch
                try {
                    patch = dmp.patch_make(this.document, diffs);
                } catch (Exception e) {
                    // Changes could not be applied, notify client about this
                    //long appEndtTime = System.currentTimeMillis();
                    //executeLatency.store(appEndtTime-appStartTime);
                    break;
                }
                this.document = (String) dmp.patch_apply(patch, this.document)[0];
                this.client_shadow = this.document;
                if (ui)
                    this.jEditorPane.setText(document);
                break;
        }
    }


    public double computeStatistic(int index) {
        HashMap<Integer, Double> latencies = new HashMap<>();
        // Compute all latencies
        int k = 0;
        for (int i = index; i < index + this.sampleRate; i++) {
            if (this.requestsReceivedTime.get(i) != null) {
                // latency[request_i] = T_req_Received[i] - T_req_Sent[i]
                long latency = this.requestsReceivedTime.get(i) - this.requestsSentTime.get(i);
                double latencySeconds = ((double) latency) / 1000;
                latencies.put(k, latencySeconds);
                k++;
            }
        }
        // Compute average Latency of all latencies
        double s = 0;
        for (int i = 0; i < latencies.size(); i++) {
            s += latencies.get(i);
        }
        this.averageLatencyAll = s / latencies.size();
        this.averageLatencyAll = Math.round(this.averageLatencyAll * 100) / 100;
        return this.averageLatencyAll;
    }

}

