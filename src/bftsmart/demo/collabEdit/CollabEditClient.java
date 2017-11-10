package bftsmart.demo.collabEdit;

import java.awt.*;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import java.io.InputStreamReader;

import bftsmart.communication.client.ReplyListener;
import bftsmart.tom.AsynchServiceProxy;
import bftsmart.tom.RequestContext;
import bftsmart.tom.ServiceProxy;
import bftsmart.tom.core.messages.TOMMessage;
import bftsmart.tom.core.messages.TOMMessageType;
import bftsmart.tom.util.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.swing.*;
import javax.swing.text.Document;
import javax.swing.text.html.HTMLEditorKit;
import java.util.LinkedList;
import java.util.logging.Level;

/**
 * Example client that writes on a document
 *
 */
public class CollabEditClient implements ReplyListener {

    private AsynchServiceProxy editorProxy;
    private String document = "";
    private String client_shadow;
    // cursorPosition;
    // range;
    private boolean subscribed;
    private String local_change;
    private  DiffMatchPatch dmp;
    private JEditorPane jEditorPane;

    JSONParser parser = new JSONParser();

    public static void main(String[] args) throws IOException, InterruptedException {
        if (args.length < 1) {
            System.out.println("Expected <processId>");
            System.exit(-1);
        }
        boolean verbose = true;
        boolean ui = true;
        if (args.length > 1) {
            try {
                verbose = Boolean.parseBoolean(args[1]);
                ui = Boolean.parseBoolean(args[2]);
            } catch (Exception kA) {
                // Do nothing
            }
        }
        System.out.println("Started");
        new CollabEditClient(Integer.parseInt(args[0]), verbose,  ui);
    }

    public CollabEditClient(int id, boolean verbose, boolean ui) {

        editorProxy = new AsynchServiceProxy(id);

        if (ui)
        {
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
            j.setSize(new Dimension(600,600));

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
                System.out.println("Read");
                this.document = (String) jsonObject.get("data");
                this.client_shadow = this.document;
                this.jEditorPane.setText(document);
                break;
            case "write":
                System.out.println("Write");
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
                    //long appEndtTime = System.currentTimeMillis();
                    //executeLatency.store(appEndtTime-appStartTime);
                    break;
                }
                this.document = (String) dmp.patch_apply(patch, this.document)[0];
                this.client_shadow = this.document;
                this.jEditorPane.setText(document);
                break;
        }
    }

}
