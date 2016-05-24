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

package com.coinblesk.client.about;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.view.View;
import android.widget.TextView;

import com.coinblesk.client.AppConstants;
import com.coinblesk.client.R;
import com.coinblesk.client.utils.AppUtils;

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
        body.setText(Html.fromHtml(getString(R.string.about_build_info_main, AppUtils.getVersionName())));

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
    }


    private void openUrl(String url) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
    }
}


