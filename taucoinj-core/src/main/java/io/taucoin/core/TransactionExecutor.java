package io.taucoin.core;

import io.taucoin.db.ByteArrayWrapper;
import io.taucoin.listener.TaucoinListener;
import io.taucoin.util.ByteUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.util.*;

import static org.apache.commons.lang3.ArrayUtils.getLength;
import static org.apache.commons.lang3.ArrayUtils.isEmpty;
import static io.taucoin.config.SystemProperties.CONFIG;
import static io.taucoin.util.BIUtil.*;
import static io.taucoin.util.ByteUtil.EMPTY_BYTE_ARRAY;
import static io.taucoin.util.ByteUtil.toHexString;

/**
 * @author Roman Mandeleil
 * @since 19.12.2014
 */
public class TransactionExecutor {

    private static final Logger logger = LoggerFactory.getLogger("execute");

    private Transaction tx;
    private Repository track;
    private byte[] coinbase;
    private Blockchain blockchain;
    private TaucoinListener listener;
    /**
     * this is a temporary strategy.
     * a transaction per second.
     * allow old but low fee transaction have opportunity
     * to be recorded in block.
     */
    private static final int MaxHistoryCount = 144;

    long basicTxAmount = 0;
    long basicTxFee = 0;

    //constructor
    public TransactionExecutor(Transaction tx, Repository track,Blockchain blockchain,TaucoinListener listener) {
        this.tx= tx;
        this.track= track;
        this.blockchain = blockchain;
        this.listener = listener;
    }

    /**
     * Do all the basic validation
     */
    public boolean init() {

		// Check In Transaction Amount
        basicTxAmount = toBI(tx.getAmount()).longValue();
        if (basicTxAmount < 0 ) {
            if (logger.isWarnEnabled())
                logger.warn("Transaction amount [{}] is invalid!", basicTxAmount);
            return false;
        }

        // Check In Transaction Fee
        basicTxFee = toBI(tx.transactionCost()).longValue();
        if (basicTxFee < 1 ) {
            if (logger.isWarnEnabled())
                logger.warn("Transaction fee [{}] is invalid!", basicTxFee);
            tx.TRANSACTION_STATUS = "Not enough fee for transaction";
            return false;
        }

        /**
         * node need to check whether this transaction has been recorded in block.
         * a honest node need to avoid transactions duplicated in block chain.
         */
        AccountState accountState = track.getAccountState(tx.getSender());
        if(accountState == null){
            if(logger.isErrorEnabled())
                logger.error("in valid account ,address is: {}", ByteUtil.toHexString(tx.getSender()));
            return false;
        }
        long tranTime = ByteUtil.byteArrayToLong(tx.getTime());
        Set<Long> txHistory = accountState.getTranHistory().keySet();
        if(!txHistory.isEmpty()) {
            long txTimeCeil = Collections.max(txHistory);
            long txTimeFloor = Collections.min(txHistory);

            /**
             * System should be concurrency high rather than 1 transaction per second.
             */
            if (tranTime <= txTimeCeil) {
                if (accountState.getTranHistory().containsKey(tranTime)) {
                    logger.error("duplicate transaction ,tx is: {}", ByteUtil.toHexString(tx.getHash()));
                    return false;
                }

                if (tranTime < txTimeFloor && blockchain.getSize() > MaxHistoryCount) {
                    long freshTime = blockchain.getSize() - MaxHistoryCount;
                    if(freshTime == 1 && tranTime < ByteUtil.byteArrayToLong(CONFIG.getGenesis().getTimestamp())){
                        logger.error("overflow attacking transaction ,tx is: {}", ByteUtil.toHexString(tx.getHash()));
                        return false;
                    }

                    if (freshTime > 1 && tranTime < ByteUtil.byteArrayToLong(blockchain.getBlockByNumber(freshTime -1).getTimestamp())) {
                        logger.error("attacking transaction ,tx is: {}", ByteUtil.toHexString(tx.getHash()));
                        return false;
                    }
                }
            }
        }

        BigInteger totalCost = toBI(tx.getAmount()).add(toBI(tx.transactionCost()));
        BigInteger senderBalance = track.getBalance(tx.getSender());

        if (!isCovers(senderBalance, totalCost)) {

            if (logger.isWarnEnabled())
                logger.warn("No enough balance: Require: {}, Sender's balance: {}", totalCost, senderBalance);
            tx.TRANSACTION_STATUS = "No enough balance";
            return false;
        }
        return true;
    }

    /**
     * Do the executation
     * 1. add balance to received address 
     * 2. add transaction fee to actually miner 
     */
    public void executeFinal(byte[] blockhash, boolean isTxCompleted) {
        // Sender subtract balance
        BigInteger totalCost = toBI(tx.getAmount()).add(toBI(tx.transactionCost()));
        //logger.info("in executation sender is "+Hex.toHexString(tx.getSender()));
        track.addBalance(tx.getSender(), totalCost.negate());

        TransactionExecuatedOutcome outcome = new TransactionExecuatedOutcome();
        outcome.setBlockHash(blockhash);
        outcome.setTxComplete(isTxCompleted);
        outcome.setTxid(tx.getHash());
        outcome.setSenderAddress(tx.getSender());

        // Receiver add balance
        track.addBalance(tx.getReceiveAddress(), toBI(tx.getAmount()));
        outcome.setReceiveAddress(tx.getReceiveAddress());

        FeeDistributor feeDistributor = new FeeDistributor(ByteUtil.byteArrayToLong(tx.transactionCost()));
        //lookup sender account state.
        AccountState senderAccountState = track.getAccountState(tx.getSender());
        if (feeDistributor.distributeFee()) {
            // Transfer fees to forger
            //logger.error("1 current witness share {}",feeDistributor.getCurrentWitFee());
            track.addBalance(coinbase, toBI(feeDistributor.getCurrentWitFee()));
            HashMap<byte[],Long> currentWintess = new HashMap<>();
            currentWintess.put(coinbase,feeDistributor.getCurrentWitFee());
            outcome.setCurrentWintess(currentWintess);

            // Transfer fees to receiver
            //track.addBalance(tx.getReceiveAddress(), toBI(feeDistributor.getReceiveFee()));
            if (senderAccountState.getWitnessAddress() != null) {
                //logger.error("2 last witness {} share {}",
                //Hex.toHexString(track.getAccountState(tx.getSender()).getWitnessAddress()),
                //feeDistributor.getLastWitFee());
                // Transfer fees to last witness
                track.addBalance(senderAccountState.getWitnessAddress(), toBI(feeDistributor.getLastWitFee()));
                HashMap<byte[],Long> lastWintess = new HashMap<>();
                lastWintess.put(senderAccountState.getWitnessAddress(), feeDistributor.getLastWitFee());
                outcome.setLastWintess(lastWintess);
            }

            int senderAssSize = senderAccountState.getAssociatedAddress().size();
            if (senderAssSize != 0) {
                //logger.error("3 associated size {} share {}",
                //track.getAccountState(tx.getSender()).getAssociatedAddress().size(),
                //feeDistributor.getLastAssociFee());
                // Transfer fees to last associate
                AssociatedFeeDistributor assDistributor = new AssociatedFeeDistributor(
                        senderAssSize,
                        feeDistributor.getLastAssociFee());

                if (assDistributor.assDistributeFee()) {
                    ArrayList<byte[]> senderAssAddress = senderAccountState.getAssociatedAddress();
                    for (int i = 0; i < senderAssSize; ++i) {
                        if(i != senderAssSize -1) {
                            //logger.info("3-1 associated address is {} average is {}",Hex.toHexString(
                            //track.getAccountState(tx.getSender()).getAssociatedAddress().get(i)),assDistributor.getAverageShare());
                            track.addBalance(senderAssAddress.get(i), toBI(assDistributor.getAverageShare()));
                            outcome.updateSenderAssociated(senderAssAddress.get(i), assDistributor.getAverageShare());
                        } else {
                            track.addBalance(senderAssAddress.get(i), toBI(assDistributor.getLastShare()));
                            outcome.updateSenderAssociated(senderAssAddress.get(i), assDistributor.getLastShare());
                        }
                    }
                }
            }

            /**
             * 2 special situation is dealt by distribute associated fee to current forger
             */
            if (senderAccountState.getWitnessAddress() == null) {
                //logger.error("4 coinbase {} share {}",
                //Hex.toHexString(coinbase),
                //feeDistributor.getLastWitFee());
                // Transfer fees to current witness
                track.addBalance(coinbase, toBI(feeDistributor.getLastWitFee()));
                outcome.updateCurrentWintessBalance(coinbase,feeDistributor.getLastWitFee());
            }

            if (senderAssSize == 0) {
                //logger.error("5 coinbase {} share {}",
                //Hex.toHexString(coinbase),
                //feeDistributor.getLastAssociFee());
                // Transfer fees to current associate
                track.addBalance(coinbase, toBI(feeDistributor.getLastAssociFee()));
                outcome.updateCurrentWintessBalance(coinbase,feeDistributor.getLastAssociFee());
            }
            listener.onTransactionExecuated(outcome);
        }

        // Increase forge power.
        //logger.info("before increase sender address is {} power is {}",Hex.toHexString(tx.getSender()),track.getforgePower(tx.getSender()));
        track.increaseforgePower(tx.getSender());
        //logger.info("after increase sender address is {} power is {}",Hex.toHexString(tx.getSender()),track.getforgePower(tx.getSender()));

        logger.info("Pay fees to miner: [{}], feesEarned: [{}]", Hex.toHexString(coinbase), basicTxFee);

        //AccountState accountState = track.getAccountState(tx.getSender());
        if(blockchain.getSize() > MaxHistoryCount && senderAccountState.getTranHistory().size() !=0 ){
            long txTime = Collections.min(senderAccountState.getTranHistory().keySet());
            // if earliest transaction is beyond expire time
            // it will be removed.
            long freshTime = blockchain.getSize() - MaxHistoryCount;
            if (freshTime > 1) {
                long bechTime = ByteUtil.byteArrayToLong(blockchain.getBlockByNumber(freshTime -1 ).getTimestamp());
                while (txTime < bechTime) {
                    senderAccountState.getTranHistory().remove(txTime);
                    if (senderAccountState.getTranHistory().size() == 0) {
                        break;
                    }
                    txTime = Collections.min(senderAccountState.getTranHistory().keySet());
                }
            } else {
                long txTimeTemp = ByteUtil.byteArrayToLong(tx.getTime());
                senderAccountState.getTranHistory().put(txTimeTemp, tx.getHash());
            }
        }else{
            long txTime = ByteUtil.byteArrayToLong(tx.getTime());
            senderAccountState.getTranHistory().put(txTime,tx.getHash());
        }

    }

    public void undoTransaction() {
        // add sender balance
        BigInteger totalCost = toBI(tx.getAmount()).add(toBI(tx.transactionCost()));
        track.addBalance(tx.getSender(), totalCost);

        // Subtract receiver balance
        track.addBalance(tx.getReceiveAddress(), toBI(tx.getAmount()).negate());

        FeeDistributor feeDistributor = new FeeDistributor(ByteUtil.byteArrayToLong(tx.transactionCost()));

        AccountState senderAccountState = track.getAccountState(tx.getSender());

        if (feeDistributor.distributeFee()) {
            // Transfer fees to forger
            track.addBalance(coinbase, toBI(feeDistributor.getCurrentWitFee()).negate());
            if (senderAccountState.getWitnessAddress() != null) {
                // Transfer fees to last witness
                track.addBalance(senderAccountState.getWitnessAddress(),
                        toBI(feeDistributor.getLastWitFee()).negate());
            }

            int size = senderAccountState.getAssociatedAddress().size();
            if (size != 0) {
                // Transfer fees to last associate
                AssociatedFeeDistributor assDistributor = new AssociatedFeeDistributor(
                        size, feeDistributor.getLastAssociFee());

                if (assDistributor.assDistributeFee()) {
                    for (int i = 0; i < size; ++i) {
                        if(i != size -1) {
                            track.addBalance(senderAccountState.getAssociatedAddress().get(i),
                                    toBI(assDistributor.getAverageShare()).negate());
                        } else {
                            track.addBalance(senderAccountState.getAssociatedAddress().get(i),
                                    toBI(assDistributor.getLastShare()).negate());
                        }
                    }
                }
            }

            /**
             * 2 special situation is dealt by distribute associated fee to current forger
             */
            if (senderAccountState.getWitnessAddress() == null) {
                // Transfer fees to last witness
                track.addBalance(coinbase,
                        toBI(feeDistributor.getLastWitFee()).negate());
            }

            if (size == 0) {
                // Transfer fees to last associate
                track.addBalance(coinbase,
                        toBI(feeDistributor.getLastAssociFee()).negate());
            }
        }

        // undo account transaction history
        if (senderAccountState.getTranHistory().keySet().contains( ByteUtil.byteArrayToLong(tx.getTime()) ) ) {
            senderAccountState.getTranHistory().remove( ByteUtil.byteArrayToLong(tx.getTime()) );
        }

        track.reduceForgePower(tx.getSender());
    }

	/**
	 * Set Miner Address
	 */
	public void setCoinbase(byte [] address){
        this.coinbase= address;
    }
}
