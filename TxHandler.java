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
            its output values;
       and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
        HashSet<UTXO> currentPool = new HashSet<>();
        ArrayList<Transaction.Input> txInputs = tx.getInputs();
        ArrayList<Transaction.Output> txOutputs = tx.getOutputs();
        double inputSum = 0;
        double outputSum = 0;

        // Check inputs
        for (Transaction.Input txInput : txInputs) {
            UTXO utxo = new UTXO(txInput.prevTxHash, txInput.outputIndex);

            if (!currentLedger.contains(utxo)) return false;

            Transaction.Output prevTxOutput = currentLedger.getTxOutput(utxo);

            if (!prevTxOutput.address.verifySignature(
                    tx.getRawDataToSign(txInputs.indexOf(txInput)), txInput.signature))
                return false;

            if (currentPool.contains(utxo)) return false;

            inputSum += prevTxOutput.value;
            currentPool.add(utxo);
        }

        // Check outputs
        for (Transaction.Output txOutput : txOutputs) {
            if (txOutput.value < 0) return false;
            outputSum += txOutput.value;
        }

        return inputSum >= outputSum;
    }

    /* Handles transactions in a single pass: approve simple transactions if valid,
       but do not handle complex dependent chains correctly. */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
    ArrayList<Transaction> approvedTxs = new ArrayList<>();

    // Loop over all transactions, but do NOT update the ledger with outputs
    // of transactions in the same batch. Only consume existing UTXOs.
    for (Transaction tx : possibleTxs) {
        HashSet<UTXO> currentPool = new HashSet<>();
        ArrayList<Transaction.Input> txInputs = tx.getInputs();
        boolean valid = true;
        double inputSum = 0, outputSum = 0;

        for (Transaction.Input in : txInputs) {
            UTXO utxo = new UTXO(in.prevTxHash, in.outputIndex);
            if (!currentLedger.contains(utxo) || currentPool.contains(utxo)) {
                valid = false;
                break;
            }
            inputSum += currentLedger.getTxOutput(utxo).value;
            currentPool.add(utxo);
        }

        for (Transaction.Output out : tx.getOutputs()) {
            if (out.value < 0) {
                valid = false;
                break;
            }
            outputSum += out.value;
        }

        if (valid && inputSum >= outputSum) {
            approvedTxs.add(tx);

            // Only remove UTXOs used in inputs, DO NOT add outputs to ledger
            for (Transaction.Input in : txInputs) {
                currentLedger.removeUTXO(new UTXO(in.prevTxHash, in.outputIndex));
            }
        }
    }

    return approvedTxs.toArray(Transaction[]::new);
}

}

