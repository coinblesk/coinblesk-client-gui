package com.coinblesk.payments.communications.peers.steps.cltv;

import com.coinblesk.json.SignTO;
import com.coinblesk.json.TxSig;
import com.coinblesk.payments.Constants;
import com.coinblesk.payments.WalletService;
import com.coinblesk.payments.communications.http.CoinbleskWebService;
import com.coinblesk.payments.communications.messages.DERObject;
import com.coinblesk.payments.communications.peers.steps.Step;
import com.coinblesk.payments.models.TimeLockedAddressWrapper;
import com.coinblesk.util.BitcoinUtils;
import com.coinblesk.util.CoinbleskException;
import com.coinblesk.util.InsufficientFunds;
import com.coinblesk.util.SerializeUtils;
import org.bitcoinj.core.*;
import org.bitcoinj.crypto.TransactionSignature;
import org.bitcoinj.uri.BitcoinURI;
import retrofit2.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class CLTVInstantPaymentStep implements Step {
    private final WalletService.WalletServiceBinder walletServiceBinder;
    private final BitcoinURI paymentRequest;

    public CLTVInstantPaymentStep(WalletService.WalletServiceBinder walletServiceBinder, BitcoinURI paymentRequest) {
        this.walletServiceBinder = walletServiceBinder;
        this.paymentRequest = paymentRequest;
    }


    @Override
    public DERObject process(DERObject input) {

        try {
            final ECKey clientKey = walletServiceBinder.getMultisigClientKey();
            final Address toAddress = paymentRequest.getAddress();
            final Address changeAddress = walletServiceBinder.getCurrentReceiveAddress();
            final Coin amount = paymentRequest.getAmount();
            final List<TransactionOutput> outputs = walletServiceBinder.getUnspentInstantOutputs();

            // create unsigned Tx that spends all outputs
            final Transaction tx = BitcoinUtils.createTx(
                    Constants.PARAMS,
                    outputs,
                    changeAddress,
                    toAddress,
                    amount.longValue());

            final List<TransactionInput> inputs = tx.getInputs();
            List<TransactionSignature> txSignatures = new ArrayList<>(inputs.size());
            for (int i = 0; i < inputs.size(); ++i) {
                TransactionInput txIn = inputs.get(i);
                TransactionOutput prevTxOut = txIn.getConnectedOutput();

                byte[] sentToHash = prevTxOut.getScriptPubKey().getPubKeyHash();
                TimeLockedAddressWrapper redeemData = walletServiceBinder.findTimeLockedAddressByHash(sentToHash);
                if (redeemData == null) {
                    // TODO: cannot sign without redeem script
                }
                ECKey signKey = redeemData.getClientKey().getKey();
                byte[] redeemScript = redeemData.getTimeLockedAddress().createRedeemScript().getProgram();
                TransactionSignature signature = tx.calculateSignature(i, signKey, redeemScript, Transaction.SigHash.ALL, false);
                txSignatures.add(signature);
            }

            byte[] serializedTx = tx.unsafeBitcoinSerialize();
            List<TxSig> serializedTxSignatures = SerializeUtils.serializeSignatures(txSignatures);
            SignTO signTO = new SignTO();
            signTO.currentDate(System.currentTimeMillis())
                    .publicKey(clientKey.getPubKey())
                    .transaction(serializedTx)
                    .signatures(serializedTxSignatures);
            SerializeUtils.signJSON(signTO, clientKey);

            CoinbleskWebService service = Constants.RETROFIT.create(CoinbleskWebService.class);
            Response<SignTO> response = service.signTx(signTO).execute();
            if (response.isSuccess()) {
                SignTO responseTO = response.body();

            }
        } catch (InsufficientFunds e) {
            // TODO
            e.printStackTrace();
        } catch (CoinbleskException e) {
            // TODO
            e.printStackTrace();
        } catch (IOException e) {
            // TODO
            // e.printStackTrace();
        }

        /*
        PaymentRequestSendStep paymentRequestSendStep = new PaymentRequestSendStep(paymentRequest);
        PaymentRequestReceiveStep paymentRequestReceiveStep = new PaymentRequestReceiveStep(walletServiceBinder);
        PaymentAuthorizationReceiveStep paymentAuthorizationReceiveStep = new PaymentAuthorizationReceiveStep(paymentRequest);

        DERObject output = paymentRequestSendStep.process(input);
        output = paymentRequestReceiveStep.process(output);
        output = paymentAuthorizationReceiveStep.process(output);

        PaymentFinalSignatureOutpointsSendStep paymentFinalSignatureSendStep = new PaymentFinalSignatureOutpointsSendStep(
                walletServiceBinder,
                paymentRequest.getAddress(),
                paymentRefundSendStep.getClientSignatures(),
                paymentRefundSendStep.getServerSignatures(),
                paymentRefundSendStep.getFullSignedTransaction(),
                paymentRefundSendStep.getHalfSignedRefundTransaction());
        PaymentFinalSignatureOutpointsReceiveStep paymentFinalSignatureReceiveStep = new PaymentFinalSignatureOutpointsReceiveStep(
                walletServiceBinder.getMultisigClientKey(),
                paymentAuthorizationReceiveStep.getServerSignatures(),
                paymentRequest);
        output = paymentFinalSignatureSendStep.process(output);
        output = paymentFinalSignatureReceiveStep.process(output);
        */
        // walletServiceBinder.commitAndBroadcastTransaction(paymentFinalSignatureReceiveStep.getFullSignedTransaction());

        return null;
    }
}
