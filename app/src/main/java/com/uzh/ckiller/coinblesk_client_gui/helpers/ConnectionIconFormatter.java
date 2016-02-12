package com.uzh.ckiller.coinblesk_client_gui.helpers;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.widget.ImageView;

import com.uzh.ckiller.coinblesk_client_gui.R;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by ckiller on 08/02/16.
 */
public class ConnectionIconFormatter implements IPreferenceStrings {


    private Context mContext;
    private Set<String> mConnectionSettings;

    public ConnectionIconFormatter(Context context) {
        this.mContext = context;
    }

    public void format(ImageView imageView, String status) {

        // Get shared Preferences
        SharedPreferences preferences = PreferenceManager
                .getDefaultSharedPreferences(mContext);
        mConnectionSettings = preferences.
                getStringSet(CONNECTION_SETTINGS, new HashSet<String>());

        // Set the Icon Color and Visibility
        if (mConnectionSettings != null) {
            for (String s : mConnectionSettings) {
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
        imageView.setColorFilter(mContext.getResources()
                .getColor(R.color.colorAccent));
    }
}
