package main;

import java.io.*;
import java.util.*;

public class UserCluster {
    public static class Transaction {
        public final String transId, hash, addr;
        public final Long amount;
        public final Boolean in;

        public Transaction(final String transId, final String hash, final String addr, final long amount, final boolean in) {
            this.transId = transId;
            this.hash = hash;
            this.addr = addr;
            this.amount = new Long(amount);
            this.in = new Boolean(in);
        }

        @Override
        public int hashCode() {
            return transId.hashCode() ^ hash.hashCode() ^ addr.hashCode() ^ amount.hashCode() ^ in.hashCode();
        }

        @Override
        public boolean equals(Object other) {
            if (other instanceof Transaction) {
                final Transaction RHS = (Transaction)other;
                return RHS.transId.equals(this.transId) &&
                       RHS.hash.equals(this.hash) &&
                       RHS.addr.equals(this.addr) &&
                       RHS.amount.equals(this.amount) &&
                       RHS.in.equals(this.in);
            } else {
                return false;
            }
        }
    }

    private static class Uptree { 
        // Implements a disjoint-set datastructure with path compression & Union-by-size as described on wikipedia here: https://en.wikipedia.org/wiki/Disjoint-set_data_structure
        private final int[] parents;
        private final int[] sizes;
        private int maxSize = 1;
        private int maxIdx = 0;
        private int numSets;

        public Uptree(final int N) {
            parents = new int[N];
            sizes = new int[N];
            numSets = N;
            for (int i = 0; i < N; ++i) {
                parents[i] = i;
                sizes[i] = 1;
            }
        }

        public int find(final int i) {
            final int parent = parents[i];
            if (parent == i) {
                return parent;
            } else {
                parents[i] = find(parent);
                return parents[i];
            }
        }

        public int getMaxSize() {
            return maxSize;
        }

        public int getMaxIdx() {
            return maxIdx;
        }

        public void union(final int i, final int j) {
            final int ri = find(i);
            final int rj = find(j);
            if (ri != rj) {
                if (sizes[ri] < sizes[rj]) {
                    parents[ri] = rj;
                    sizes[rj] += sizes[ri];
                    if (sizes[rj] > maxSize) {
                        maxSize = sizes[rj];
                        maxIdx = rj;
                    }
                } else {
                    parents[rj] = ri;
                    sizes[ri] += sizes[rj];
                    if (sizes[ri] > maxSize) {
                        maxSize = sizes[ri];
                        maxIdx = ri;
                    }
                }
                numSets--;
            }
        }

        private Map<Integer, Set<Integer>> getSets() {
            final Map<Integer, Set<Integer>> ret = new HashMap<>();
            for (int i = 0; i < parents.length; ++i) {
                final int ri = find(i);
                final Set<Integer> set = ret.getOrDefault(ri, new HashSet<>());
                set.add(i);
                ret.putIfAbsent(ri, set);
            }
            return ret;
        }

        public int getNumSets() {
            return numSets;
        }
    }

    private final Map<Long, List<String>> userMap = new HashMap<>(); // Map a user id to a list of bitcoin addresses
    private final Map<String, Long> keyMap = new HashMap<>(); // Map a bitcoin address to a user id
    private final List<Transaction> transactions = new ArrayList<>();

    public Map<Long, List<String>> getUserMap() {
        return userMap;
    }

    public Map<String, Long> getKeyMap() {
        return keyMap;
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

    private static final String IN = "in";
    private static final String OUT = "out";
 
    private boolean parseInOut(final String str) {
       if (IN.equals(str)) {
            return true;
        } else if (OUT.equals(str)) {
            return false;
        } else {
            throw new RuntimeException("Failed to read file, read " + str + " as in/out");
        }
    }

    /**
     * Read transactions from file
     * 
     * @param file
     * @return true if read succeeds; false otherwise
     */
    public boolean readTransactions(String file) {
        try (final Scanner scanner = new Scanner(new File(file))) {
            while (scanner.hasNextLine()) {
                final String transId = scanner.next();
                final String hash = scanner.next();
                final String addr = scanner.next();
                final long amount = scanner.nextLong();
                final boolean in = parseInOut(scanner.next());
                transactions.add(new Transaction(transId, hash, addr, amount, in));
                scanner.nextLine();
            }
        } catch (IOException exp) {
            throw new RuntimeException(exp);
        } catch (InputMismatchException exp) {
            throw new RuntimeException("Could not parse input", exp);
        }
        return true;
    }

    private List<String> getAddresses() {
        final Set<String> addresses = new HashSet<>();
        for (final Transaction transaction : transactions) {
            addresses.add(transaction.addr);
        }
        return new ArrayList<>(addresses);
    }

    private <T> Map<T, Integer> indexMapFromList(final List<T> list) {
        final Map<T, Integer> ret = new HashMap<>();
        for (int i = 0; i < list.size(); ++i) {
            ret.put(list.get(i), i);
        }
        return ret;
    }

    private Map<String, Set<String>> makeIdAddrMap() {
        final Map<String, Set<String>> ret = new HashMap<>();
        for (final Transaction transaction : transactions) {
            if (transaction.in) {
                final Set<String> set = ret.getOrDefault(transaction.transId, new HashSet<>());
                set.add(transaction.addr);
                ret.putIfAbsent(transaction.transId, set);
            }
        }
        return ret;
    }


    private int largestClusterSize = -1;

    /**
     * Merge addresses based on joint control
     */
    public void mergeAddresses() {
        final List<String> addresses = getAddresses();
        final Map<String, Integer> indexMap = indexMapFromList(addresses);
        final Uptree uptree = new Uptree(addresses.size());
        for (final Map.Entry<String, Set<String>> pair : makeIdAddrMap().entrySet()) {
            final Iterator<String> iterator = pair.getValue().iterator();
            final String firstAddr = iterator.next();
            final int firstIdx = indexMap.get(firstAddr);
            while (iterator.hasNext()) {
                uptree.union(firstIdx, indexMap.get(iterator.next()));
            }
        }

        largestClusterSize = uptree.getMaxSize();

        long idx = 0L;
        for (final Map.Entry<Integer, Set<Integer>> pair : uptree.getSets().entrySet()) {
            final List<String> addressesForIndex = new ArrayList<>();
            for (Integer i : pair.getValue()) {
                final String address = addresses.get(i);
                addressesForIndex.add(address);
                keyMap.put(address, idx);
            }
            userMap.put(idx, addressesForIndex);
            idx++;
        }
    }

    /**
     * Return number of users (i.e., clusters) in the transaction dataset
     * 
     * @return number of users (i.e., clusters)
     */
    public int getUserNumber() {
        return userMap.size();
    }

    /**
     * Return the largest cluster size
     * 
     * @return size of the largest cluster
     */
    public int getLargestClusterSize() {
        return largestClusterSize;
    }

    public boolean writeUserMap(String file) {
        try {
            BufferedWriter w = new BufferedWriter(new FileWriter(file));
            for (long user : userMap.keySet()) {
                List<String> keys = userMap.get(user);
                w.write(user + " ");
                for (String k : keys) {
                    w.write(k + " ");
                }
                w.newLine();
            }
            w.flush();
            w.close();
        } catch (IOException e) {
            System.err.println("Error in writing user list!");
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public boolean writeKeyMap(String file) {
        try {
            BufferedWriter w = new BufferedWriter(new FileWriter(file));
            for (String key : keyMap.keySet()) {
                w.write(key + " " + keyMap.get(key) + "\n");
                w.newLine();
            }
            w.flush();
            w.close();
        } catch (IOException e) {
            System.err.println("Error in writing key map!");
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public boolean writeUserGraph(String txFile, String userGraphFile) {
        try {
            BufferedReader r1 = new BufferedReader(new FileReader(txFile));
            Map<String, Long> txUserMap = new HashMap<String, Long>();
            String nextLine;
            while ((nextLine = r1.readLine()) != null) {
                String[] s = nextLine.split(" ");
                if (s.length < 5) {
                    System.err.println("Invalid format: " + nextLine);
                    r1.close();
                    return false;
                }
                if (s[4].equals("in") && !txUserMap.containsKey(s[0])) { // new transaction 
                    Long user;
                    if ((user=keyMap.get(s[2])) == null) {
                        System.err.println(s[2] + " is not in the key map!");
                        r1.close();
                        return false;
                    }
                    txUserMap.put(s[0], user);
                } 
            }
            r1.close();

            BufferedReader r2 = new BufferedReader(new FileReader(txFile));
            BufferedWriter w = new BufferedWriter(new FileWriter(userGraphFile));
            while ((nextLine = r2.readLine()) != null) {
                String[] s = nextLine.split(" ");
                if (s.length < 5) {
                    System.err.println("Invalid format: " + nextLine);
                    r2.close();
                    w.flush();
                    w.close();
                    return false;
                }
                if (s[4].equals("out")) {
                    if(txUserMap.get(s[0]) == null) {
                        System.err.println("Did not find input transaction for Tx: " + s[0]);
                        r2.close();
                        w.flush();
                        w.close();
                        return false;
                    }
                    long inputUser = txUserMap.get(s[0]);
                    Long outputUser;
                    if ((outputUser=keyMap.get(s[2])) == null) {
                        System.err.println(s[2] + " is not in the key map!");
                        r2.close();
                        w.flush();
                        w.close();
                        return false;
                    }
                    w.write(inputUser + "," + outputUser + "," + s[3] + "\n");
                } 
            }
            r2.close();
            w.flush();
            w.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

}
