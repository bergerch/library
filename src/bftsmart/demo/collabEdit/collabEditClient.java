package bftsmart.demo.collabEdit;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import java.io.InputStreamReader;

import bftsmart.tom.ServiceProxy;
import bftsmart.tom.util.Logger;
import java.util.logging.Level;

/**
 * Example client that writes on a document
 *
 */
public class collabEditClient {

    public static void main(String[] args) throws IOException, InterruptedException {

        ServiceProxy editorProxy = new ServiceProxy(4321);

        byte[] response;

        System.out.println("Started");

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
}
