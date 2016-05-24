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
