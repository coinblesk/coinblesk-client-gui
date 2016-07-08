package com.coinblesk.client;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.support.v7.app.AlertDialog;
import android.util.Log;

import com.coinblesk.bitcoin.BitcoinNet;
import com.coinblesk.client.config.AppConfig;
import com.coinblesk.client.utils.ClientUtils;
import com.coinblesk.json.v1.VersionTO;
import com.coinblesk.util.CoinbleskException;


import java.io.IOException;
import java.lang.ref.WeakReference;

import retrofit2.Response;

/**
 * @author Andreas Albrecht
 */
public class VersionCheckTask extends AsyncTask<Void, Void, VersionTO> {

    private final WeakReference<Activity> weakActivity;
    private Exception exception;
    private final AppConfig appConfig;
    private final String clientAppVersion;
    private final BitcoinNet clientNetwork;

    public VersionCheckTask(AppConfig appConfig, String clientAppVersion, Activity activity) {
        this.appConfig = appConfig;
        if (ClientUtils.isMainNet(appConfig.getNetworkParameters())) {
            clientNetwork = BitcoinNet.MAINNET;
        } else if (ClientUtils.isTestNet(appConfig.getNetworkParameters())
                || ClientUtils.isLocalTestNet(appConfig.getNetworkParameters())) {
            clientNetwork = BitcoinNet.TESTNET;
        } else {
            throw new RuntimeException("Unknown network");
        }

        this.clientAppVersion = clientAppVersion;
        this.weakActivity = new WeakReference<>(activity);
    }

    protected VersionTO doInBackground(Void... params) {
        try {
            CoinbleskWebService service = appConfig.getCoinbleskService();

            VersionTO requestTO = new VersionTO()
                    .clientVersion(clientAppVersion)
                    .bitcoinNet(clientNetwork);
            Response<VersionTO> response = service.version(requestTO).execute();
            if (!response.isSuccessful()) {
                throw new CoinbleskException(
                        "Version compatibility check: request failed with code: "
                                + response.code());
            }

            VersionTO responseTO = response.body();
            if (!responseTO.isSuccess()) {
                throw new CoinbleskException(
                        "Version compatibility check: server responded with error: "
                                + responseTO.type().toString());
            }

            return responseTO;
        } catch (IOException e) {
            Log.w("VersionCheckTask", "Could not do version check, the client or server is probably offline: " + e.getMessage());
            this.exception = e;
        } catch (Exception e) {
            Log.e("VersionCheckTask", "Could not do version check: ", e);
            this.exception = e;
        }
        return null;
    }

    protected void onPostExecute(VersionTO responseTO) {
        if (responseTO == null || exception != null) {
            return;
        }

        Log.d("VersionCheckTask", String.format(
                "Version compatibility check: clientAppVersion=%s, isSupported=%s, serverNetwork=%s",
                clientAppVersion, responseTO.isSupported(), responseTO.bitcoinNet()));

        if (!responseTO.isSupported()) {
            if (clientNetwork.equals(responseTO.bitcoinNet())) {
                showIncompatibleVersionDialog();
            } else {
                showIncompatibleNetworkDialog(responseTO.bitcoinNet());
            }

        }
    }

    private void showIncompatibleVersionDialog() {
        final Activity activity = weakActivity.get();
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return;
        }

        Dialog dialog = new AlertDialog.Builder(activity, R.style.AlertDialogAccent)
                .setTitle(R.string.version_compatibility_dialog_title)
                .setMessage(activity.getString(R.string.version_compatibility_dialog_message, clientAppVersion))
                .setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .create();
        dialog.show();
    }

    private void showIncompatibleNetworkDialog(BitcoinNet serverNetwork) {
        final Activity activity = weakActivity.get();
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return;
        }

        Dialog dialog = new AlertDialog.Builder(activity, R.style.AlertDialogAccent)
                .setTitle(R.string.network_compatibility_dialog_title)
                .setMessage(activity.getString(R.string.network_compatibility_dialog_message, clientNetwork, serverNetwork))
                .setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .create();
        dialog.show();
    }
}
