package bftsmart.aware.tests;

import sun.reflect.generics.tree.Tree;

import java.util.LinkedList;
import java.util.List;

/**
 * Implements a three-leveled k-nary Dissemination tree used to aggregate votes to form a quorum certificate
 */
public class DisseminationTree {

    int n;
    int fanout;

    int quorum_size;
    TreeNode root;

    long[][] latencyMap;

    public DisseminationTree(int fanout, int[] nodes, String[] labels, long[][] m) {
        this.n = nodes.length;
        this.fanout = fanout;
        this.root = new TreeNode(nodes[0], labels[nodes[0]]);
        this.latencyMap = m;

        // Create the mid-level hierachy
        for (int i = 1; i <= fanout; i++) {
            TreeNode child = new TreeNode(nodes[i], labels[nodes[i]]);
            this.root.addChild(child);
            // Create the leaf level
            for (int j = i*fanout+1; j < (i+1)*fanout+1; j++) {
                child.addChild(new TreeNode(nodes[j], labels[nodes[j]]));
            }
        }

        this.quorum_size = (int) Math.ceil( ((double) 2*n+1) / (double) 3.0 );
        //System.out.println("New Dissemination Tree! root: " + root.getValue() + " children: " + root.getChildNodes() + " fanout: " + fanout + " quorumSize: " + quorum_size);
    }

    public void printTree() {
        System.out.println(root.toString());
        for (TreeNode node : root.childNodes) {
            System.out.println(" |--"+node.toString()  + " Disseminated: " + node.latencyDisseminated + " Aggregated: " + node.latencyAggregated + " Delivered: " + node.latencyDelivered);
            for (TreeNode leaf : node.childNodes) {
                System.out.println("     |--"+leaf.toString()  + " Disseminated: " + leaf.latencyDisseminated + " Aggregated: " + leaf.latencyAggregated + " Delivered: " + leaf.latencyDelivered);
            }
        }
    }

    public long latencyQC() {
        root.latencyDisseminated = 0;
        for (TreeNode internal: root.childNodes) {
            internal.latencyDisseminated = root.latencyDisseminated + latencyMap[root.getValue()][internal.getValue()];
            for (TreeNode leaf: internal.childNodes) {
                leaf.latencyDisseminated = internal.latencyDisseminated + latencyMap[internal.getValue()][leaf.getValue()];
                leaf.latencyAggregated =  leaf.latencyDisseminated; // Return instanly the vote
                leaf.latencyDelivered =  leaf.latencyAggregated + latencyMap[leaf.getValue()][internal.getValue()];
            }
        }
        // Dissemination finished!
        // Aggregation starts!
        for (TreeNode internal: root.childNodes) {
            internal.latencyAggregated = internal.latencyDisseminated;
            for (TreeNode leaf: internal.childNodes) {
               internal.latencyAggregated = Math.max(internal.latencyAggregated, leaf.latencyDelivered);
               internal.votes += leaf.votes;
            }
            internal.latencyDelivered = internal.latencyAggregated + latencyMap[internal.getValue()][root.getValue()];
        }
       root.getChildNodes().sort((child1, child2) -> (Long.compare(child1.latencyDelivered , child2.latencyDelivered)));

        for (TreeNode internal: root.getChildNodes()) {
            //System.out.println("Votes " + root.votes);
            if (root.votes >= quorum_size) {
                return root.latencyAggregated;
            }
            root.latencyAggregated = Math.max(root.latencyAggregated, internal.latencyDelivered);
            root.votes += internal.votes;
        }
        return root.latencyAggregated;
    }

    public long latencyQC2() {
        for (TreeNode internal: root.childNodes) {    // Dissemination starts!
            internal.latencyDisseminated = latencyMap[root.getValue()][internal.getValue()];
            for (TreeNode leaf: internal.childNodes) {
                leaf.latencyDisseminated = internal.latencyDisseminated + latencyMap[internal.getValue()][leaf.getValue()];
            }
        }
        // Aggregation starts!
        for (TreeNode internal: root.childNodes) {
            for (TreeNode leaf: internal.childNodes) {
                internal.latencyAggregated = Math.max(internal.latencyAggregated, leaf.latencyDisseminated + latencyMap[internal.getValue()][leaf.getValue()]);
                internal.votes += leaf.votes;
            }
            internal.latencyDelivered = internal.latencyAggregated + latencyMap[internal.getValue()][root.getValue()];
        }
        root.getChildNodes().sort((child1, child2) -> (Long.compare(child1.latencyDelivered , child2.latencyDelivered)));

        for (TreeNode internal: root.getChildNodes()) {
            if (root.votes >= quorum_size) {
                return root.latencyAggregated;
            }
            root.latencyAggregated = Math.max(root.latencyAggregated, internal.latencyDelivered);
            root.votes += internal.votes;
        }
        return root.latencyAggregated;
    }




 class TreeNode {

        private int id;
        private String label;
        private List<TreeNode> childNodes;

        public long latencyDisseminated = 0;
        public long latencyAggregated = 0;

        public long latencyDelivered = 0;

        public int votes = 1;

        public TreeNode(int value, String label) {
            this.id = value;
            this.label = label;
            this.childNodes = new LinkedList<>();
        }

        public void addChild(TreeNode childNode) {
            this.childNodes.add(childNode);
        }

        public int getValue() {
            return id;
        }

        public String getlabel() {
            return label;
        }

        public List<TreeNode> getChildNodes() {
            return childNodes;
        }

     @Override
     public String toString() {
         return " " + this.id + " (" + this.getlabel()+ ") ,";
     }
 }


}
