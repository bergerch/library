package bftsmart.demo.collabEdit;

import org.json.simple.JSONArray;

import java.util.LinkedList;

public class CollaborativeUtils {

    public static byte[] diffsToBytes(LinkedList<DiffMatchPatch.Diff> edits, int requester) {
        String dataString = "[";
        String issuer = "\"requester\":" + requester;
        int k = 0;
        for (DiffMatchPatch.Diff edit : edits) {
            String a = "[";
            a += DiffMatchPatch.Operation.toInt(edit.operation) + ",";
            a += "\"" + edit.text + "\"";
            a += "]";
            dataString += a; //FIXME String Builder
            if (k < edits.size() - 1) {
                dataString += ",";
            }
            k++;
        }
        dataString += "]";
        String command = "\"operation\":\"write\"";
        String data = "\"data\":" + dataString;
        String res = "{" + command + "," + data + "," + issuer + "}";
        return res.getBytes();
    }


    public static LinkedList<DiffMatchPatch.Diff> parseJSONArray(JSONArray data) {
        LinkedList<DiffMatchPatch.Diff> diffs = new LinkedList<>();
        for (Object a : data) {
            long method = (long) ((JSONArray) a).get(0);
            String text = ((String) ((JSONArray) a).get(1));
            DiffMatchPatch.Operation op = DiffMatchPatch.Operation.fromInt((int) method);
            DiffMatchPatch.Diff diff = new DiffMatchPatch.Diff(op, text);
            diffs.add(diff);
        }
        return diffs;
    }

}
