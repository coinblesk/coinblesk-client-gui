package com.coinblesk.client;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.view.View;
import android.widget.TextView;

import com.coinblesk.client.coinblesk_client_gui.BuildConfig;
import com.coinblesk.client.coinblesk_client_gui.R;

/**
 * Created by ckiller on 02/03/16.
 */
public class AboutActivity extends AppCompatActivity {

    private View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.social_github:
                    openUrl(AppConstants.URL_GITHUB);
                    break;
                case R.id.social_website:
                    openUrl(AppConstants.URL_BITCOIN_CSG_WEBSITE);
                    break;
                case R.id.social_tos:
                    AboutUtils.showTos(AboutActivity.this);
                    break;
                case R.id.uzh_icon_box:
                    openUrl(AppConstants.URL_UZH_WEBSITE);
                    break;
                case R.id.ifi_icon_box:
                    openUrl(AppConstants.URL_IFI_WEBSITE);
                    break;
                case R.id.twitter_icon_box:
                    openUrl(AppConstants.URL_COINBLESK_TWITTER);
                    break;
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_about);
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        findViewById(R.id.social_github).setOnClickListener(onClickListener);
        findViewById(R.id.social_website).setOnClickListener(onClickListener);
        findViewById(R.id.social_tos).setOnClickListener(onClickListener);
        findViewById(R.id.ifi_icon_box).setOnClickListener(onClickListener);
        findViewById(R.id.uzh_icon_box).setOnClickListener(onClickListener);
        findViewById(R.id.twitter_icon_box).setOnClickListener(onClickListener);

        TextView body = (TextView) findViewById(R.id.about_build_info);
        body.setText(Html.fromHtml(getString(R.string.about_build_info_main, BuildConfig.VERSION_NAME)));

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
    }


    private void openUrl(String url) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
    }
}


