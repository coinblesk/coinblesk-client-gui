package com.coinblesk.payments.models;

import org.bitcoinj.core.Transaction;

import ch.papers.objectstorage.models.AbstractUuidObject;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 15/02/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
public class RefundTransactionWrapper extends AbstractUuidObject {
    private final Transaction transaction;

    public RefundTransactionWrapper(Transaction transaction) {
        this.transaction = transaction;
    }

    public Transaction getTransaction() {
        return transaction;
    }
}
