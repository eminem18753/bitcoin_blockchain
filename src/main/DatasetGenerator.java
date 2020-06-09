package main;

import info.blockchain.api.blockexplorer.*;
import info.blockchain.api.APIException;
import java.io.IOException;
import java.io.FileWriter;

import java.util.*;

public class DatasetGenerator {
	String file;

	public DatasetGenerator(String file) {
		this.file = file;
	}

        private List<Block> getBlocks() throws APIException, IOException {
            final BlockExplorer explorer = new BlockExplorer();
            final Set<Block> result = new HashSet<>();
            final int MIN_HEIGHT = 265852;
            final int MAX_HEIGHT = 266085;
            for (int i = MIN_HEIGHT; i <= MAX_HEIGHT; ++i) {
                result.addAll(explorer.getBlocksAtHeight(i));
            }
            return new ArrayList<>(result);
        }

        private boolean isCoinbase(final Transaction t) {
            return t.getInputs().size() == 1 && t.getInputs().get(0).getPreviousOutput() == null;
        }

	public boolean writeTransactions() {
            try (final FileWriter writer = new FileWriter(file, false)) {
                for (final Block block : getBlocks()) {
                    for (final Transaction transaction : block.getTransactions()) {
                        final long index = transaction.getIndex();
                        final String hash = transaction.getHash();
                        if (!isCoinbase(transaction)) {
                            for (final Input input : transaction.getInputs()) {
                                final Output lastOutput = input.getPreviousOutput();
                                writer.append(generateInputRecord(index, hash, lastOutput.getAddress(), lastOutput.getValue()) + "\n");
                            }
                            for (final Output output : transaction.getOutputs()) {
                                final String addr = output.getAddress();
                                if (!addr.isEmpty()) {
                                    writer.append(generateOutputRecord(index, hash, addr, output.getValue()) + "\n");
                                }
                            }
                        }
                    }
                }
            } catch (final IOException | APIException exp) {
                throw new RuntimeException(exp);
            }
            return true;
	}

	/**
	 * Generate a record in the transaction dataset
	 * 
	 * @param txIndex
	 *            Transaction index
	 * @param txHash
	 *            Transaction hash
	 * @param address
	 *            Previous output address of the input
	 * @param value
	 *            Number of Satoshi transferred
	 * @return A record of the input
	 */
	private String generateInputRecord(long txIndex, String txHash,
			String address, long value) {
		return txIndex + " " + txHash + " " + address + " " + value + " in";
	}

	/**
	 * Generate a record in the transaction dataset
	 * 
	 * @param txIndex
	 *            Transaction index
	 * @param txHash
	 *            Transaction hash
	 * @param address
	 *            Output bitcoin address
	 * @param value
	 *            Number of Satoshi transferred
	 * @return A record of the output
	 */
	private String generateOutputRecord(long txIndex, String txHash,
			String address, long value) {
		return txIndex + " " + txHash + " " + address + " " + value + " out";
	}

}
