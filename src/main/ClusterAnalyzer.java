package main;

import java.io.*;
import java.util.*;

public class ClusterAnalyzer {
    private static Map<Long, List<String>> userMap;
    private static Map<String, Long> keyMap;
    private static List<UserCluster.Transaction> transactions;

    private static void printMostReceipts() {
        final Map<Long, Integer> numReceipts = new HashMap<>();
        for (final UserCluster.Transaction transaction : transactions) {
            if (!transaction.in) {
                final long user = keyMap.get(transaction.addr);
                numReceipts.put(user, 1 + numReceipts.getOrDefault(user, 0));
            }
        }
        long maxUser = -1;
        int maxAmt = 0;
        for (final Map.Entry<Long, Integer> pair : numReceipts.entrySet()) {
            if (pair.getValue() > maxAmt) {
                maxUser = pair.getKey();
                maxAmt = pair.getValue();
            }
        }
        System.out.println("User " + maxUser + " had most receipts");
        MapToFile(numReceipts, "NumReceipts.txt");
    }

    private static void printMostReceived() {
        final Map<Long, Long> moneyReceived = new HashMap<>();
        for (final UserCluster.Transaction transaction : transactions) {
            if (!transaction.in) {
                final long user = keyMap.get(transaction.addr);
                moneyReceived.put(user, transaction.amount + moneyReceived.getOrDefault(user, 0L));
            }
        }
        long maxUser = -1;
        long maxAmt = 0;
        for (final Map.Entry<Long, Long> pair : moneyReceived.entrySet()) {
            if (pair.getValue() > maxAmt) {
                maxUser = pair.getKey();
                maxAmt = pair.getValue();
            }
        }
        System.out.println("User " + maxUser + " had most money received");
        MapToFile(moneyReceived, "MoneyReceived.txt");
    }

    private static <K,V> void MapToFile(final Map<K, V> map, final String name) {
        try (final FileWriter writer = new FileWriter(name, false)) {
            for (final Map.Entry<K, V> pair : map.entrySet()) {
                writer.write(pair.getValue().toString() + " " + pair.getKey().toString() + "\n");
            }
        } catch (final IOException exp) {
            throw new RuntimeException(exp);
        }
    }

    private static final long FBI_ID = 31124L;

    private static void printPaidFBI() {
        final Set<String> addrs = new HashSet<>(userMap.get(FBI_ID));
        final Set<String> ids = new HashSet<>();
        for (final UserCluster.Transaction transaction : transactions) {
            if (!transaction.in && addrs.contains(transaction.addr)) {
                ids.add(transaction.transId);
            }
        }
        final Set<Long> users = new HashSet<>();
        for (final UserCluster.Transaction transaction : transactions) {
            if (transaction.in && ids.contains(transaction.transId)) {
                users.add(keyMap.get(transaction.addr));
            }
        }
        System.out.println(users.size() + " Seem to have paid the FBI");
        for (final Long user : users) {
            for (final String str : userMap.get(user)) {
                System.out.println(str + " Paid the FBI with ID " + user);
            }
        }
    }

    public static void main(String[] args) {
        UserCluster uc = new UserCluster();
        uc.readTransactions("transactions.txt");
        uc.mergeAddresses();
        userMap = uc.getUserMap();
        keyMap = uc.getKeyMap();
        transactions = uc.getTransactions();
        printMostReceipts();
        printMostReceived();
        printPaidFBI();
    }
}
