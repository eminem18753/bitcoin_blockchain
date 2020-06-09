package main;

import info.blockchain.api.blockexplorer.*;
import info.blockchain.api.APIException;
import java.io.IOException;

import java.util.*;

public class Checkpoint1 {
    private Block block;

    public Checkpoint1() {
        try {
            block = new BlockExplorer().getBlock("000000000000000f5795bfe1de0381a44d4d5ea2ad81c21d77f275bffa03e8b3");
        } catch (APIException | IOException exp) {
            throw new RuntimeException("getBlock failed", exp);
        }
    }

	/**
	 * Blocks-Q1: What is the size of this block?
	 * 
	 * Hint: Use method getSize() in Block.java
	 * 
	 * @return size of the block
	 */
	public long getBlockSize() {
		return block.getSize();
	}

	/**
	 * Blocks-Q2: What is the Hash of the previous block?
	 * 
	 * Hint: Use method getPreviousBlockHash() in Block.java
	 * 
	 * @return hash of the previous block
	 */
	public String getPrevHash() {
		return block.getPreviousBlockHash();
	}

	/**
	 * Blocks-Q3: How many transactions are included in this block?
	 * 
	 * Hint: To get a list of transactions in a block, use method
	 * getTransactions() in Block.java
	 * 
	 * @return number of transactions in current block
	 */
	public int getTxCount() {
		return block.getTransactions().size();
	}

	/**
	 * Transactions-Q1: Find the transaction with the most outputs, and list the
	 * Bitcoin addresses of all the outputs.
	 * 
	 * Hint: To get the bitcoin address of an Output object, use method
	 * getAddress() in Output.java
	 * 
	 * @return list of output addresses
	 */
	public List<String> getOutputAddresses() {
                List<Transaction> transactions = block.getTransactions();
		Transaction maxTrans = transactions.get(0);

                for (Transaction t : transactions) {
                    if (t.getOutputs().size() > maxTrans.getOutputs().size()) {
                        maxTrans = t;
                    }
                }

                List<String> ret = new ArrayList<>();
                for (Output o : maxTrans.getOutputs()) {
                    ret.add(o.getAddress());
                }
		return ret;
	}

	/**
	 * Transactions-Q2: Find the transaction with the most inputs, and list the
	 * Bitcoin addresses of the previous outputs linked with these inputs.
	 * 
	 * Hint: To get the previous output of an Input object, use method
	 * getPreviousOutput() in Input.java
	 * 
	 * @return list of input addresses
	 */
	public List<String> getInputAddresses() {
                List<Transaction> transactions = block.getTransactions();
		Transaction maxTrans = transactions.get(0);

                for (Transaction t : transactions) {
                    if (t.getInputs().size() > maxTrans.getInputs().size()) {
                        maxTrans = t;
                    }
                }

                List<String> ret = new ArrayList<>();
                for (Input input : maxTrans.getInputs()) {
                    if (input.getPreviousOutput() != null) {
                        ret.add(input.getPreviousOutput().getAddress());
                    }
                }
		return ret;
	}

	/**
	 * Transactions-Q3: What is the bitcoin address that has received the
	 * largest amount of Satoshi in a single transaction?
	 * 
            for (Transaction t : block.getTransactions()) {
                if (t.getInputs().size() == 1 && t.getInputs().get(0).getPreviousOutput() == null) {
                    count++;
                }
            }
            return count;	 * Hint: To get the number of Satoshi received by an Output object, use
	 * method getValue() in Output.java
	 * 
	 * @return the bitcoin address that has received the largest amount of Satoshi
	 */
	public String getLargestRcv() {
		String ret = null;
                long maxAmt = 0;

                for (Transaction t : block.getTransactions()) {
                    Map<String, Long> outputRecvMap = new HashMap<>();
                    for (Output o : t.getOutputs()) {
                        String addr = o.getAddress();
                        outputRecvMap.put(addr, o.getValue() + outputRecvMap.getOrDefault(addr, 0L));
                    }

                    for (Map.Entry<String, Long> entry : outputRecvMap.entrySet()) {
                        if (entry.getValue() > maxAmt) {
                            ret = entry.getKey();
                            maxAmt = entry.getValue();
                        }
                    }
                }

		return ret;
	}

	/**
	 * Transactions-Q4: How many coinbase transactions are there in this block?
	 * 
	 * Hint: In a coinbase transaction, getPreviousOutput() == null
	 * 
	 * @return number of coin base transactions
	 */
	public int getCoinbaseCount() {
            int count = 0;
            for (Transaction t : block.getTransactions()) {
                if (t.getInputs().size() == 1 && t.getInputs().get(0).getPreviousOutput() == null) {
                    count++;
                }
            }
            return count;
	}

	/**
	 * Transactions-Q5: What is the number of Satoshi generated in this block?
	 * 
	 * @return number of Satoshi generated
	 */
	public long getSatoshiGen() {
            long val = 0;
            for (Transaction t : block.getTransactions()) {
                if (t.getInputs().size() == 1 && t.getInputs().get(0).getPreviousOutput() == null) {
                    for (Output o : t.getOutputs()) {
                        val += o.getValue();
                    }
                }
            }
            return val;
	}

}
