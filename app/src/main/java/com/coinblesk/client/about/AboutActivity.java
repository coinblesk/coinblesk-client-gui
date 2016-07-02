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

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.view.View;
import android.widget.TextView;

import com.coinblesk.client.R;
import com.coinblesk.client.utils.AppUtils;

/**
 * @author ckiller
 * @author Andreas Albrecht
 */
public class AboutActivity extends AppCompatActivity {

    private View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Context context = v.getContext();
            String urlToOpen = null;

            switch (v.getId()) {
                case R.id.social_github:
                    urlToOpen = context.getString(R.string.url_github_coinblesk);
                    break;
                case R.id.social_website:
                    urlToOpen = context.getString(R.string.url_bitcoin_csg_website);
                    break;
                case R.id.social_tos:
                    showTosDialog();
                    return;
                case R.id.uzh_icon_box:
                    urlToOpen = context.getString(R.string.url_uzh_website);
                    break;
                case R.id.ifi_icon_box:
                    urlToOpen = context.getString(R.string.url_ifi_website);
                    break;
                case R.id.twitter_icon_box:
                    urlToOpen = context.getString(R.string.url_twitter_coinblesk);
                    break;
                default:
                    urlToOpen = null;
            }

            if (urlToOpen != null) {
                openUrl(urlToOpen);
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_about);

        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        findViewById(R.id.social_github).setOnClickListener(onClickListener);
        findViewById(R.id.social_website).setOnClickListener(onClickListener);
        findViewById(R.id.social_tos).setOnClickListener(onClickListener);
        findViewById(R.id.ifi_icon_box).setOnClickListener(onClickListener);
        findViewById(R.id.uzh_icon_box).setOnClickListener(onClickListener);
        findViewById(R.id.twitter_icon_box).setOnClickListener(onClickListener);

        TextView body = (TextView) findViewById(R.id.about_build_info);
        body.setText(Html.fromHtml(getString(R.string.about_build_info_main, AppUtils.getVersionName())));

    }

    private void openUrl(String url) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
    }

    private void showTosDialog() {
        FragmentManager fm = this.getFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        Fragment prev = fm.findFragmentByTag("dialog_tos");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        EulaDialog.newInstance().show(ft, "dialog_tos");
    }
}