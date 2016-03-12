package com.coinblesk.payments.communications.http;

import com.google.common.collect.ImmutableList;
import com.coinblesk.payments.PaymentProtocol;
import com.coinblesk.payments.models.User;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.crypto.TransactionSignature;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.mock.Calls;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 06/02/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
public class MockServerInterface implements ServerInterface {

    private final UUID uuid = UUID.randomUUID();
    private final Map<UUID, User> users = new HashMap<>();
    private final Map<UUID, ECKey> keys = new HashMap<>();

    // this would be handled by authentication
    private UUID userUUID;

    @Override
    public Call<User> createUser(@Body User user) {
        this.users.put(user.getUuid(), user);
        ECKey ecKey = new ECKey();
        ECKey publicEcKey = ECKey.fromPublicOnly(ecKey.getPubKey());
        this.keys.put(user.getUuid(), ecKey);
        this.userUUID = user.getUuid();
        return Calls.response(new User(this.uuid, publicEcKey));
    }

    @Override
    public Call<TransactionSignature> createRefund(@Body Transaction refundTransaction, @Body TransactionSignature clientSignature) {
        final List<ECKey> keys = ImmutableList.of(this.users.get(this.userUUID).getEcKey(), this.keys.get(this.userUUID));
        final Script script = ScriptBuilder.createMultiSigOutputScript(2, keys);

        final TransactionSignature serverTransactionSignature = PaymentProtocol.getInstance().signMultisigTransaction(script, refundTransaction, this.keys.get(this.userUUID));

        Script refundTransactionInputScript = ScriptBuilder.createMultiSigInputScript(clientSignature, serverTransactionSignature);
        refundTransaction.getInput(0).setScriptSig(refundTransactionInputScript);
        refundTransaction.getInput(0).verify();
        this.users.get(this.userUUID).getTransactions().add(refundTransaction);

        return Calls.response(serverTransactionSignature);
    }

    @Override
    public Call<Transaction> createMultisig(@Body Transaction toMultisigTransaction) {
        for (Transaction transaction : this.users.get(this.userUUID).getTransactions()) {
            if (toMultisigTransaction.getOutput(0).getOutPointFor().equals(transaction.getInput(0).getOutpoint())) {
                System.out.println("found matching refund for multisig, ");

                this.users.get(this.userUUID).getTransactions().add(toMultisigTransaction);

                return Calls.response(transaction);
            }
        }

        return null;
    }

    @Override
    public Call<TransactionSignature> createTransaction(@Body Address recipient, @Body Coin amount) {

        User user = this.users.get(this.userUUID);
        for (Transaction transaction : user.getTransactions()) {
            if (transaction.getOutput(0).getValue().isGreaterThan(amount)
                    && transaction.getOutput(0).getScriptPubKey().getNumberOfSignaturesRequiredToSpend() == 2) {
                Transaction fromMultisigTransaction = PaymentProtocol.getInstance().generateFromMultisigTransaction(transaction.getOutput(0),
                        ECKey.fromPublicOnly(this.keys.get(this.userUUID).getPubKey()), ECKey.fromPublicOnly(user.getEcKey().getPubKey()), amount, recipient);
                user.getTransactions().add(fromMultisigTransaction);

                return Calls.response(PaymentProtocol.getInstance().signMultisigTransaction(transaction.getOutput(0), fromMultisigTransaction, this.keys.get(this.userUUID)));
            }
        }
        // todo remove the old transactions

        return null;
    }
}
