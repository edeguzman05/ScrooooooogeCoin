import java.util.ArrayList;
import java.util.HashSet;
public class TxHandler {

	private UTXOPool currentLedger;
	/* Creates a public ledger whose current UTXOPool (collection of unspent 
	 * transaction outputs) is utxoPool. This should make a defensive copy of 
	 * utxoPool by using the UTXOPool(UTXOPool uPool) constructor.
	 */
	public TxHandler(UTXOPool utxoPool) {
		// IMPLEMENT THIS
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
		// IMPLEMENT THIS
		HashSet <UTXO> currentPool = new HashSet<>();
		ArrayList<Transaction.Input> txInputs = tx.getInputs();
		ArrayList<Transaction.Output> txOutputs = tx.getOutputs();
		double inputSum = 0;
		double outputSum = 0;

		// Checking txInputs
		for(Transaction.Input txInput : txInputs){
			UTXO utxo = new UTXO(txInput.prevTxHash, txInput.outputIndex);

			// Returns false if UTXO is not in ledger
			if (!currentLedger.contains(utxo)){
				// System.out.println((utxo) + " is not in the ledger!");
				return false;
			}

			// Returns false if UTXO is in pool
			if (currentPool.contains(utxo)){
				// System.out.println((utxo) + " is already in the pool!");
				return false;
			}

			inputSum += currentLedger.getTxOutput(utxo).value;
			currentPool.add(utxo);
		}

		// Checking txOutputs
		for (Transaction.Output txOutput : txOutputs) {
			// Returns false if txOutput is negative
			if (txOutput.value < 0) {
				// System.out.println((txOutput.value) + " is negative!");
				return false;
			}
			outputSum += txOutput.value;
		}

		// Returns false if the sum of Input values
		// is less than the sum of Output values
		if (inputSum < outputSum) {
			return false;
		}

		return true;
	}

	/* Handles each epoch by receiving an unordered array of proposed 
	 * transactions, checking each transaction for correctness, 
	 * returning a mutually valid array of accepted transactions, 
	 * and updating the current UTXO pool as appropriate.
	 */
	public Transaction[] handleTxs(Transaction[] possibleTxs) {
		// IMPLEMENT THIS
		return null;
	}

} 
