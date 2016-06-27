/*
 * Copyright 2016 The Coinblesk team and the CSG Group at University of Zurich
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.coinblesk.client.models;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionBag;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 15/02/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
public class TransactionWrapper {
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
