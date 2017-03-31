import java.util.ArrayList;

public class TxHandler {

    private UTXOPool utxoPool;

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public TxHandler(UTXOPool utxoPool) {
        this.utxoPool = new UTXOPool(utxoPool);
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool, 
     * (2) the signatures on each input of {@code tx} are valid, 
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
        double input = 0.0, output = 0.0;

        for (int i = 0; i < tx.numInputs(); i++) {
            Transaction.Input ti = tx.getInput(i);
            UTXO utxo = new UTXO(ti.prevTxHash, ti.outputIndex);

            if (utxoPool.contains(utxo) == false) {
                return false;
            }

            for (int j = i + 1; j < tx.numInputs(); j++) {
                Transaction.Input ti2 = tx.getInput(j);
                UTXO utxo2 = new UTXO(ti2.prevTxHash, ti2.outputIndex);

                if (utxo2.equals(utxo)) {
                    return false;
                }
            }

            Transaction.Output to = utxoPool.getTxOutput(utxo);

            input += to.value;

            if (Crypto.verifySignature(to.address, tx.getRawDataToSign(i), ti.signature) != true) {
                return false;
            }
        }

        for (int i = 0; i < tx.numOutputs(); i++) {
            Transaction.Output to = tx.getOutput(i);

            if (to.value < 0) {
                return false;
            }

            output += to.value;
        }

        if (output > input) {
            return false;
        }

        return true;
    }

    private boolean inPool(Transaction tx) {
        ArrayList<Transaction.Input> inputs = tx.getInputs();
        Transaction.Input in;
        UTXO ut;
        for (int i = 0; i < inputs.size(); i++) {
            in = inputs.get(i);
            ut = new UTXO(in.prevTxHash, in.outputIndex);
            if (!utxoPool.contains(ut))
                return false;
        }
        return true;
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        Transaction[] tempArray = new Transaction[possibleTxs.length];
        int tempArrayLength = possibleTxs.length;
        int foundNum = 0;

        for (int i = 0; i < tempArrayLength; i++) {
            tempArray[i] = possibleTxs[i];
        }

        while (true) {
            boolean newFound = false;

            for (int i = 0; i < tempArrayLength; i++) {
                if (tempArray[i] == null) {
                    continue;
                }

                if (inPool(tempArray[i]) == true && isValidTx(tempArray[i]) == true) {

                    for (int j = 0; j < tempArray[i].numInputs(); j++) {
                        Transaction.Input ti = tempArray[i].getInput(j);
                        UTXO utxo = new UTXO(ti.prevTxHash, ti.outputIndex);

                        utxoPool.removeUTXO(utxo);
                    }

                    for (int j = 0; j < tempArray[i].numOutputs(); j++) {
                        Transaction.Output to = tempArray[i].getOutput(j);

                        UTXO utxo = new UTXO(tempArray[i].getHash(), j);
                        utxoPool.addUTXO(utxo, to);
                    }

                    tempArray[i] = null;
                    foundNum++;
                    newFound = true;
                }
            }

            if (newFound == false) {
                break;
            }
        }

        Transaction[] result = new Transaction[foundNum];

        for (int i = 0, j = 0; i < tempArrayLength; i++) {
            if (tempArray[i] == null) {
                result[j] = possibleTxs[i];
                j++;
            }
        }

        return result;
    }

    public UTXOPool getUTXOPool() {
	    UTXOPool ret = new UTXOPool(utxoPool);

	    return ret;
    }
}
