package bftsmart.aware.tests;

import bftsmart.aware.decisions.Simulator;
import bftsmart.aware.decisions.Simulator.SimulationRun;

import java.io.*;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

import static jdk.nashorn.internal.objects.NativeString.indexOf;

public class TestDisseminationTrees {
    public static void main(String[] args) {

        /**
         * Simulation parameter
         */
        int SIZE = 21; // number of replicas
        String strategy = "";  // "SA" or "Exhaustive"
        /**
         *
         */

        if (args.length > 0) {
            strategy = args[0];
        }
        // Simply copy-pasted from cloudping.io, todo add parsing from some file later
        String aws_regions = "Africa (Cape Town)\n" +
                "af-south-1\tAsia Pacific (Hong Kong)\n" +
                "ap-east-1\tAsia Pacific (Tokyo)\n" +
                "ap-northeast-1\tAsia Pacific (Seoul)\n" +
                "ap-northeast-2\tAsia Pacific (Osaka)\n" +
                "ap-northeast-3\tAsia Pacific (Mumbai)\n" +
                "ap-south-1\tAsia Pacific (Singapore)\n" +
                "ap-southeast-1\tAsia Pacific (Sydney)\n" +
                "ap-southeast-2\tCanada (Central)\n" +
                "ca-central-1\tEU (Frankfurt)\n" +
                "eu-central-1\tEU (Stockholm)\n" +
                "eu-north-1\tEU (Milan)\n" +
                "eu-south-1\tEU (Ireland)\n" +
                "eu-west-1\tEU (London)\n" +
                "eu-west-2\tEU (Paris)\n" +
                "eu-west-3\tMiddle East (Bahrain)\n" +
                "me-south-1\tSA (São Paulo)\n" +
                "sa-east-1\tUS East (N. Virginia)\n" +
                "us-east-1\tUS East (Ohio)\n" +
                "us-east-2\tUS West (N. California)\n" +
                "us-west-1\tUS West (Oregon)\n" +
                "us-west-2";
        String[] clients = aws_regions.split("\t");
        for (int i = 0; i < clients.length; i++) {
            clients[i] = clients[i].replace("\n", " ");
            System.out.println("./data/new/" + clients[i] + ".csv");
        }
        long[][] m = new long[SIZE][SIZE];
          try {
              m = readMatrix("./data/cloudPing/cloudping.csv", SIZE, SIZE, ","); // input
          } catch (Exception e) {

          }

            int[] nodes = {0,1,2,3,5,7,8,9,12,15,16,17,19};
            int k= 3;

        //    DisseminationTree dt = new DisseminationTree(3, nodes, clients, m);
         //   dt.printTree();
          //  System.out.println("CQ time: "  + dt.latencyQC());
            //dt.printTree();


            int[] distribution = new int[1000];
            for (int d = 0; d< distribution.length; d++) {
                distribution[d] = 0;
            }
            List<List<Integer>> roots = generateCombinations(nodes, 1);
            List<Long> latencies = new ArrayList<>();
            long global_min = 99999999999L;
            long global_max = -1;
            DisseminationTree fastestTree = null;
            DisseminationTree slowestTree = null;

            List<int[]> allTrees = new LinkedList<>();

            for (List<Integer> root : roots) {
                int[] remove = new int[1];
                remove[0] = root.get(0);
                int[] otherNodes = removeElements(nodes, remove);

                List<List<Integer>> internals = generateCombinations(otherNodes, k);
                for (List<Integer> internal : internals) {
                    //List<List<Integer>> leaves = new ArrayList<>();
                    int[] remove1 = new int[1];
                    remove1[0] = root.get(0);
                    remove = listToIntArray(internal);
                    int[] otherNodes2 = removeElements(nodes, remove1);
                    otherNodes = removeElements(otherNodes2, remove);

                    List<List<Integer>> leafs1 = generateCombinations(otherNodes, k);
                    for (List<Integer> leaf1: leafs1) {

                        int[] remove2 = new int[1];
                        remove2[0] = root.get(0);
                        int[] otherNodes3 = removeElements(nodes, remove2);
                        int[] otherNodes4 = removeElements(otherNodes3, listToIntArray(internal));

                        int[] otherNodes5 = removeElements(otherNodes4, listToIntArray(leaf1));

                        List<List<Integer>> leafs2 = generateCombinations(otherNodes5, k);
                        for (List<Integer> leaf2: leafs2) {


                            int[] remove3 = new int[1];
                            remove3[0] = root.get(0);
                            int[] otherNodes10 = removeElements(nodes, remove3);
                            int[] otherNodes11 = removeElements(otherNodes10, listToIntArray(internal));
                            int[] otherNodes12 = removeElements(otherNodes11, listToIntArray(leaf1));
                            int[] otherNodes13 = removeElements(otherNodes12, listToIntArray(leaf2));
                            List<Integer> leaf3 = arrayToList(otherNodes13);

                            int tree[] = concat(root, internal, leaf1, leaf2, leaf3);
                           // printArray(tree);
                            allTrees.add(tree);
                            DisseminationTree dTree = new DisseminationTree(k, tree, clients, m);
                            long lat = dTree.latencyQC();
                            int roundedDown = (int) lat / 1000;
                            distribution[roundedDown] = distribution[roundedDown] + 1;
                            long min = Math.min(global_min, lat);
                            if (min < global_min) {
                                global_min = min;
                                fastestTree = dTree;
                            }
                            long max = Math.max(global_max, lat);
                            if (max > global_max) {
                                global_max = max;
                                slowestTree = dTree;
                            }
                        }
                    }
                    /*
                    for (int i = 0; i < 10000; i++) {
                        List<Integer> leaf = arrayToList(randomPermutation(otherNodes));
                        // generate the full dissemiation tree
                        int[] dissTree = new int[nodes.length];
                        dissTree[0] = root.get(0);
                        for (int j=1; j<=k; j++) {
                            dissTree[j] = internal.get(j-1);
                        }
                        for (int j=k+1; j<nodes.length; j++) {
                         //   System.out.println("j, k, j-k-1" + j + " " + k + " " + (j-k-1));
                            dissTree[j] = leaf.get(j-k-1);
                        }
                        DisseminationTree tree = new DisseminationTree(k, dissTree, clients, m);
                        long lat = tree.latencyQC();
                        long min = Math.min(global_min, lat);
                        if (min < global_min) {
                            global_min = min;
                            fastestTree = tree;
                        }
                        long max = Math.max(global_max, lat);
                        if (max > global_max) {
                            global_max = max;
                            slowestTree = tree;
                        }
                    } */
                }

            }


            System.out.println("Number of trees: " + allTrees.size());


         //   List<List<Integer>> internals = generateCombinations(nodes, k);
          //  for (List<Integer> combination : internals) {
           //     System.out.println(combination);
           // }
            System.out.println(" max: " + global_max + "  slowest tree:");
             if (slowestTree != null) slowestTree.printTree();
            System.out.println(" min: " + global_min + " fastest tree:");
              if (fastestTree != null) fastestTree.printTree();


              for (int i=0; i<distribution.length; i++) {
                  System.out.println("" + i + "   " + distribution[i]);
              }

              System.out.println("GetMax " + getmax(distribution, distribution.length));
        /*
        int clientIndex = 0;
        final int parallel = 8;
        final ReentrantLock[] _parallelism_lock = new ReentrantLock[parallel];
        for (int i = 0; i < parallel; i++) {
            _parallelism_lock[i] = new ReentrantLock();
        }

        for (String client : clients) {
            try {
                long[][] m = readMatrix("./data/cloudPing/cloudping.csv", SIZE, SIZE, ","); // input
                int replicaset[] = makeReplicaSet(SIZE);

                int f = (SIZE - 1) / 3;
                int delta = SIZE - (3 * f + 1);
                int u = 2 * f;

                LinkedList<Thread> simulations = new LinkedList<>();

                final boolean[] init = {false};

                while (f > 0) {
                    String finalStrategy = strategy;
                    int finalF = f;
                    int finalDelta = delta;
                    int finalU = u;
                    int finalClientIndex = clientIndex;
                    Thread t = new Thread() {

                        public void run() {

                            SimulationRun sim;

                            _parallelism_lock[finalClientIndex % parallel].lock();

                            if (finalStrategy.equals("SA")) {
                                sim = Simulator.simulatedAnnealing(SIZE, finalF, finalDelta, finalU, replicaset, m, m, 0,  m[finalClientIndex], false);
                            } else {
                                sim = Simulator.exhaustiveSearch(SIZE, finalF, finalDelta, finalU, replicaset, m, m, m[finalClientIndex]);
                            }
                            System.out.println("Client-Region: " + client + " Delta: " + finalDelta + "f: " + finalF);

                            synchronized (this) {
                                try {

                                    BufferedWriter w = new BufferedWriter(
                                            new FileWriter("./data/correctable/" + client + ".csv", true)); // output
                                    if (!init[0]) {
                                        w.write("delta, t, consensus, firstResponse, tVmaxP1, 2tVmaxP1, linearizableT \n" );
                                        init[0] = true;
                                    }
                                    w.write(finalDelta + "," + finalF + "," + sim.getSolutionLatency()/100.0 + "," +
                                            sim.getLatencies()[1]/100.0 + "," + sim.getLatencies()[2]/100.0 + "," +
                                            sim.getLatencies()[3]/100.0 + "," + sim.getLatencies()[4]/100.0 + "\n");
                                    w.close();
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                            _parallelism_lock[finalClientIndex % parallel].unlock();
                        }
                    };

                    // Recalc
                    f--;
                    delta = SIZE - (3 * f + 1);
                    u = 2 * f;

                    simulations.add(t);
                }

                int i= 0;
                for (Thread simulation: simulations) {
                    simulation.setPriority(10 -i);
                    i++;;
                    simulation.start();
                    try {
                        Thread.sleep(1000L);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }

            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } finally {
                clientIndex++;
            }
        }

         */
    }

    private static List<int[]> createAllDisseminationTrees(int[] nodes) {
        LinkedList<int[]> trees = new LinkedList<>();



        return trees;
    }
    private static int[] makeReplicaSet(int n) {
        int ls[] = new int[n];
        for (int i = 0; i < n; i++)
            ls[i] = i;
        return ls;
    }

    private static void printmatrix(long[][] m) {
        for (int i = 0; i < m.length; i++) {
            for (int j = 0; j < m[i].length; j++) {
                System.out.print(m[i][j] + " ");
            }
            System.out.println();
        }
    }

    public static List<List<Integer>> generateCombinations(int[] nodes, int k) {
        List<List<Integer>> result = new ArrayList<>();
        generateCombinations(nodes, k, 0, new ArrayList<>(), result);
        return result;
    }

    private static void generateCombinations(int[] nodes, int k, int start, List<Integer> current, List<List<Integer>> result) {
        if (k == 0) {
            result.add(new ArrayList<>(current));
            return;
        }

        for (int i = start; i < nodes.length; i++) {
            current.add(nodes[i]);
            generateCombinations(nodes, k - 1, i + 1, current, result);
            current.remove(current.size() - 1);
        }
    }


    public static long[][] randomMatrix(int size) {
        Random r = new Random();
        long[][] m = new long[size][size];
        for (int i = 0; i < m.length; i++) {
            for (int j = 0; j < m[i].length; j++) {
                m[i][j] = r.nextInt(200);
            }
        }
        return m;
    }

    /**
     * @param filename
     * @param n        - lines
     * @param m        - columns
     * @return
     * @throws IOException
     */
    public static long[][] readMatrix(String filename, int n, int m, String div) throws IOException {

        BufferedReader reader = new BufferedReader(new FileReader(filename));

        long matrix[][] = new long[n][m];

        String line[];

        for (int i = 0; i < n; i++) {
            line = reader.readLine().split(div);
            for (int j = 0; j < m; j++) {
                matrix[i][j] = Math.round(((Double.parseDouble(line[j]) * 1000) / 2.0));// multiplication to keep two decimals
            }
        }

        reader.close();

        return matrix;
    }

    public static int[] removeElements(int[] array, int[] toRemove) {
        List<Integer> list = new ArrayList<>();
        for (int num : array) {
            if (!contains(toRemove, num)) {
                list.add(num);
            }
        }
        int[] result = new int[list.size()];
        for (int i = 0; i < list.size(); i++) {
            result[i] = list.get(i);
        }
        return result;
    }

    private static boolean contains(int[] array, int num) {
        for (int value : array) {
            if (value == num) {
                return true;
            }
        }
        return false;
    }



    public static int[] listToIntArray(List<Integer> list) {
        int[] array = new int[list.size()];
        int index = 0;
        for (int num : list) {
            array[index++] = num;
        }
        return array;
    }


    public static int[] randomPermutation(int[] nums) {
        int[] permutation = Arrays.copyOf(nums, nums.length);
        Random rand = new Random();

        for (int i = permutation.length - 1; i > 0; i--) {
            int j = rand.nextInt(i + 1);
            swap(permutation, i, j);
        }

        return permutation;
    }

    private static void swap(int[] array, int i, int j) {
        int temp = array[i];
        array[i] = array[j];
        array[j] = temp;
    }

    public static List<Integer> arrayToList(int[] array) {
        List<Integer> list = new ArrayList<>();
        for (int num : array) {
            list.add(num);
        }
        return list;
    }


    public static int[] concat(List<Integer> root, List<Integer> internal, List<Integer> leaf1, List<Integer> leaf2, List<Integer> leaf3) {
        int[] tree = new int[13];
        tree[0] = root.get(0);
        for (int i = 0; i < 3; i++) {
            tree[1+i] = internal.get(i);
            tree[4+i] = leaf1.get(i);
            tree[7+i] = leaf2.get(i);
            tree[10+i] = leaf3.get(i);
        }
        return tree;
    }

    public static void printArray(int[] array) {
        System.out.print("[");
        for (int i=0; i<13; i++)
            System.out.print(" "+array[i] + " ,");

        System.out.print("]");
        System.out.println();
    }
    static int getmax(int arr[], int n){
        if(n==1)
            return arr[0];
        return Math.max(arr[n-1], getmax(arr, n-1));
    }

}