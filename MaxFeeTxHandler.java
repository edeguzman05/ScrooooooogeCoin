import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class MaxFeeTxHandler {

    private UTXOPool ledger;

    /* Creates a public ledger whose current UTXOPool is utxoPool. */
    public MaxFeeTxHandler(UTXOPool utxoPool) {
        ledger = new UTXOPool(utxoPool);
    }

    /* Returns true if tx is valid according to the same rules as TxHandler. */
    private boolean isValidTx(Transaction tx) {
        HashSet<UTXO> seenUTXOs = new HashSet<>();
        double inputSum = 0;
        double outputSum = 0;

        for (int i = 0; i < tx.numInputs(); i++) {
            Transaction.Input in = tx.getInput(i);
            UTXO utxo = new UTXO(in.prevTxHash, in.outputIndex);

            if (!ledger.contains(utxo)) return false;
            if (seenUTXOs.contains(utxo)) return false;

            Transaction.Output prevOut = ledger.getTxOutput(utxo);

            RSAKey publicKey = prevOut.address;
            byte[] message = tx.getRawDataToSign(i);
            byte[] signature = in.signature;

            if (!publicKey.verifySignature(message, signature)) return false;

            inputSum += prevOut.value;
            seenUTXOs.add(utxo);
        }

        for (Transaction.Output out : tx.getOutputs()) {
            if (out.value < 0) return false;
            outputSum += out.value;
        }

        return inputSum >= outputSum;
    }

    /* Compute the transaction fee (inputSum - outputSum). */
    private double calculateFee(Transaction tx) {
        double inputSum = 0, outputSum = 0;

        for (int i = 0; i < tx.numInputs(); i++) {
            Transaction.Input in = tx.getInput(i);
            UTXO utxo = new UTXO(in.prevTxHash, in.outputIndex);
            if (ledger.contains(utxo)) {
                inputSum += ledger.getTxOutput(utxo).value;
            }
        }

        for (Transaction.Output out : tx.getOutputs()) {
            outputSum += out.value;
        }

        return inputSum - outputSum;
    }

    /* Handle transactions, selecting the set with maximum total fees. */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        List<Transaction> accepted = new ArrayList<>();
        boolean progress = true;

        // Greedy approach: keep taking the highest fee tx that’s valid
        while (progress) {
            progress = false;
            Transaction bestTx = null;
            double bestFee = Double.NEGATIVE_INFINITY;

            for (Transaction tx : possibleTxs) {
                if (accepted.contains(tx)) continue;

                if (isValidTx(tx)) {
                    double fee = calculateFee(tx);
                    if (fee > bestFee) {
                        bestFee = fee;
                        bestTx = tx;
                    }
                }
            }

            if (bestTx != null) {
                accepted.add(bestTx);

                // Update ledger: remove spent UTXOs, add new outputs
                for (Transaction.Input in : bestTx.getInputs()) {
                    UTXO utxo = new UTXO(in.prevTxHash, in.outputIndex);
                    ledger.removeUTXO(utxo);
                }
                for (int i = 0; i < bestTx.numOutputs(); i++) {
                    UTXO utxo = new UTXO(bestTx.getHash(), i);
                    ledger.addUTXO(utxo, bestTx.getOutput(i));
                }

                progress = true;
            }
        }

        return accepted.toArray(new Transaction[0]);
    }
}
