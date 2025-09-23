import java.util.ArrayList;
import java.util.HashSet;

public class TxHandler {

    private UTXOPool currentLedger;

    /* Creates a public ledger whose current UTXOPool (collection of unspent 
     * transaction outputs) is utxoPool. This should make a defensive copy of 
     * utxoPool by using the UTXOPool(UTXOPool uPool) constructor.
     */
    public TxHandler(UTXOPool utxoPool) {
        currentLedger = new UTXOPool(utxoPool);
    }

    /* Returns true if 
     * (1) all outputs claimed by tx are in the current UTXO pool, 
     * (2) the signatures on each input of tx are valid, 
     * (3) no UTXO is claimed multiple times by tx, 
     * (4) all of tx’s output values are non-negative, and
     * (5) the sum of tx’s input values is greater than or equal to the sum of   
     *     its output values;
     * and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
        HashSet<UTXO> usedUTXOs = new HashSet<>();
        double inputSum = 0.0;
        double outputSum = 0.0;

        // check inputs
        for (int i = 0; i < tx.numInputs(); i++) {
            Transaction.Input in = tx.getInput(i);
            UTXO utxo = new UTXO(in.prevTxHash, in.outputIndex);

            // (1) must be in current ledger
            if (!currentLedger.contains(utxo)) {
                return false;
            }

            Transaction.Output prevOut = currentLedger.getTxOutput(utxo);

            // (2) signature must be valid
            byte[] msg = tx.getRawDataToSign(i);
            if (!prevOut.address.verifySignature(msg, in.signature)) {
                return false;
            }

            // (3) no double claims
            if (usedUTXOs.contains(utxo)) {
                return false;
            }
            usedUTXOs.add(utxo);

            inputSum += prevOut.value;
        }

        // check outputs
        for (Transaction.Output out : tx.getOutputs()) {
            // (4) must be non-negative
            if (out.value < 0) {
                return false;
            }
            outputSum += out.value;
        }

        // (5) inputSum >= outputSum
        return inputSum >= outputSum;
    }

    /* Handles each epoch by receiving an unordered array of proposed 
     * transactions, checking each transaction for correctness, 
     * returning a mutually valid array of accepted transactions, 
     * and updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        ArrayList<Transaction> accepted = new ArrayList<>();
        boolean progress = true;

        // keep looping until no more tx can be added (handles dependencies)
        while (progress) {
            progress = false;
            for (Transaction tx : possibleTxs) {
                if (!accepted.contains(tx) && isValidTx(tx)) {
                    // accept transaction
                    accepted.add(tx);
                    progress = true;

                    // update UTXO pool:
                    // remove spent outputs
                    for (Transaction.Input in : tx.getInputs()) {
                        UTXO utxo = new UTXO(in.prevTxHash, in.outputIndex);
                        currentLedger.removeUTXO(utxo);
                    }

                    // add new outputs
                    for (int i = 0; i < tx.numOutputs(); i++) {
                        UTXO utxo = new UTXO(tx.getHash(), i);
                        currentLedger.addUTXO(utxo, tx.getOutput(i));
                    }
                }
            }
        }

        return accepted.toArray(new Transaction[0]);
    }
}
