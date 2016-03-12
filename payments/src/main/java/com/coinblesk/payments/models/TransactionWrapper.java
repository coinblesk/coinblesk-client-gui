package com.coinblesk.payments.models;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionBag;

import ch.papers.objectstorage.models.AbstractUuidObject;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 15/02/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
public class TransactionWrapper extends AbstractUuidObject {
    private final Transaction transaction;
    private final Coin amount;

    public TransactionWrapper(Transaction transaction, TransactionBag transactionBag) {
        this.transaction = transaction;
        this.amount = transaction.getValue(transactionBag);
    }

    public Coin getAmount() {
        return amount;
    }

    public Transaction getTransaction() {

        return transaction;
    }
}
