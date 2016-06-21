package com.coinblesk.client.additionalservices;


import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.widget.ListView;

import com.coinblesk.client.R;

public class AdditionalServicesActivity extends AppCompatActivity {
    private final static String TAG = AdditionalServicesActivity.class.getName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.additional_services);
    }

    @Override
    protected void onStart() {
        Log.d(TAG, "onStart");
        super.onStart();
        AdditionalServiceAdapter adapter = new AdditionalServiceAdapter(this);
        ListView listView = (ListView) findViewById(R.id.additional_services_list);
        listView.setAdapter(adapter);
        initToolbar();
    }

    private void initToolbar() {
        Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
    }
}
