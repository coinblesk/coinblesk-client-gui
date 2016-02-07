package ch.papers.payments.models;

import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Transaction;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 06/02/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
public class User {
    private final UUID uuid;
    private final ECKey ecKey;
    private final List<Transaction> transactions = new ArrayList<>();

    public User(UUID uuid, ECKey ecKey){
        this.uuid = uuid;
        this.ecKey = ecKey;
    }

    public User(){
        this(UUID.randomUUID(),new ECKey());
    }

    public UUID getUuid() {
        return uuid;
    }

    public ECKey getEcKey() {
        return ecKey;
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

    public List<Transaction> getRefundTransactions(){
        final List<Transaction> refundTransactions = new ArrayList<>();
        for (Transaction transaction:this.transactions) {
            if(transaction.isTimeLocked()){
                refundTransactions.add(transaction);
            }
        }
        return refundTransactions;
    }
}
