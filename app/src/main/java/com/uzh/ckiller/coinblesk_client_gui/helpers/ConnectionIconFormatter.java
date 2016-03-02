package com.uzh.ckiller.coinblesk_client_gui.helpers;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.widget.ImageView;

import com.uzh.ckiller.coinblesk_client_gui.R;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by ckiller on 08/02/16.
 */
public class ConnectionIconFormatter implements IPreferenceStrings {


    private Context mContext;
    private Set<String> connectionSettings;

    public ConnectionIconFormatter(Context context) {
        this.mContext = context;
    }

    public void setIconColor(ImageView imageView, String status) {

        // Get shared Preferences
        SharedPreferences preferences = PreferenceManager
                .getDefaultSharedPreferences(mContext);
        this.connectionSettings = preferences.
                getStringSet(CONNECTION_SETTINGS, new HashSet<String>());

        // Set the Icon Color and Visibility
        if (connectionSettings != null) {
            for (String s : connectionSettings) {
                switch (s) {
                    case NFC_ACTIVATED:
                        if (status == NFC_ACTIVATED) {
                            this.makeVisible(imageView);
                        }
                        break;
                    case BT_ACTIVATED:
                        if (status == BT_ACTIVATED) {
                            this.makeVisible(imageView);
                        }
                        break;
                    case WIFIDIRECT_ACTIVATED:
                        if (status == WIFIDIRECT_ACTIVATED) {
                            this.makeVisible(imageView);
                        }
                        break;
                }

            }

        }

    }

    private void makeVisible(ImageView imageView) {
        imageView.setAlpha(ICON_VISIBLE);
        imageView.setColorFilter(ContextCompat.getColor(mContext,R.color.colorAccent));
    }
}
