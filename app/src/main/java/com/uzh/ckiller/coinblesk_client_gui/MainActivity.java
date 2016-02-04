package com.uzh.ckiller.coinblesk_client_gui;


import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;


/**
 * Created by ckiller
 */

public class MainActivity extends AppCompatActivity implements KeyboardFragment.KeyboardClicked {

    private Amount amount = Amount.getInstance();
    private NavigationView navigationView;
    private DrawerLayout drawerLayout;

    private SampleFragmentPagerAdapter fragmentPagerAdapter;

//    private final int mIndicatorColor = Color.BLACK;
//    private final int mDividerColor = Color.BLUE;

    private CurrencyFormatter currencyFormatter;
    private FragmentManager fragmentManager = getSupportFragmentManager();

    private String[] mDrawerItems;
    private ListView mDrawerList;
    private DrawerLayout mDrawerLayout;
    private Toolbar mToolbar;
    private ActionBarDrawerToggle mDrawerToggle;
    private CharSequence mDrawerTitle;
    private ProgressBar mProgressBar;
    private CharSequence mTitle;
    private QrDialogFragment mQrDialogFragment;
    ViewPager viewPager;

//TODO Create Landscape views for all Fragments. E.g. Landscape View for send / receive with smaller representation of the Balance fragment.

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        currencyFormatter = new CurrencyFormatter(this);

        initToolbar();
        initNavigationView();
        initViewPager();

    }

    private void initViewPager() {

        // Get the ViewPager and set its PagerAdapter so that it can display items
        viewPager = (ViewPager) findViewById(R.id.viewpager);
        fragmentPagerAdapter = new SampleFragmentPagerAdapter(getSupportFragmentManager(), MainActivity.this);
        viewPager.setAdapter(fragmentPagerAdapter);


        // Give the TabLayout the ViewPager
        TabLayout tabLayout = (TabLayout) findViewById(R.id.sliding_tabs);
        tabLayout.setTabMode(TabLayout.MODE_FIXED);
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);
        tabLayout.setupWithViewPager(viewPager);


    }

    private void initToolbar() {
        Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
    }

    @Override
    public void setTitle(CharSequence title) {
        mTitle = title;
        getActionBar().setTitle(mTitle);
    }


    private void initNavigationView() {
        navigationView = (NavigationView) findViewById(R.id.navigation_view);

        //Setting Navigation View Item Selected Listener to handle the item click of the navigation menu
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {

            @Override
            public boolean onNavigationItemSelected(MenuItem menuItem) {

                if (menuItem.isChecked()) menuItem.setChecked(false);
                else menuItem.setChecked(true);

                //Closing drawer on item click
                drawerLayout.closeDrawers();

                switch (menuItem.getItemId()) {

                    case R.id.verified_users:
                        Toast.makeText(getApplicationContext(), "Verified UsersSelected", Toast.LENGTH_SHORT).show();
                        return true;
                    case R.id.backup:
                        Toast.makeText(getApplicationContext(), "Backup Selected", Toast.LENGTH_SHORT).show();
                        return true;
                    case R.id.activate_vendor_mode:
                        Toast.makeText(getApplicationContext(), "Activate Vendor Mode Selected", Toast.LENGTH_SHORT).show();
                        return true;
                    case R.id.settings:
                        Toast.makeText(getApplicationContext(), "Settings Selected", Toast.LENGTH_SHORT).show();
                        Intent newAct = new Intent(getApplicationContext(), SettingsActivity.class);
                        startActivity(newAct);
                        return (true);
                    case R.id.about_coinblesk:
                        Toast.makeText(getApplicationContext(), "About Coinblesk Selected", Toast.LENGTH_SHORT).show();
                        return true;
                    default:
                        Toast.makeText(getApplicationContext(), "Somethings Wrong", Toast.LENGTH_SHORT).show();
                        return true;

                }
            }
        });

        // Initializing Drawer Layout and ActionBarToggle
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer);
        ActionBarDrawerToggle actionBarDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout, mToolbar, R.string.openDrawer, R.string.closeDrawer) {

            @Override
            public void onDrawerClosed(View drawerView) {
                // Code here will be triggered once the drawer opens, we don't need anything to happen yet, hence leave it blank
                super.onDrawerClosed(drawerView);
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                // Code here will be triggered once the drawer opens, we don't need anything to happen yet, hence leave it blank
                super.onDrawerOpened(drawerView);
            }
        };

        //Setting the actionbarToggle to the drawerLayout
        drawerLayout.setDrawerListener(actionBarDrawerToggle);

        //calling sync state is necessary because the menu icon wouldnt show up otheriwse
        actionBarDrawerToggle.syncState();

    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
         /*
         Handle action bar item clicks here. The action bar will
         automatically handle clicks on the Home/Up button, so long
         as you specify a parent activity in AndroidManifest.xml.
         */
        switch (item.getItemId()) {
            case android.R.id.home:
                drawerLayout.openDrawer(GravityCompat.START);
                return true;
            case R.id.action_settings:
                return true;
            case R.id.action_qr_code:
                showQrDialog();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);

        MenuItem qrCodeItem = menu.add(0, R.id.action_qr_code, 0, R.string.action_qr_code);
        qrCodeItem.setIcon(R.drawable.ic_action_qr_code);
        MenuItemCompat.setShowAsAction(qrCodeItem, MenuItem.SHOW_AS_ACTION_IF_ROOM);

        return true;
    }

    public void showQrDialog() {
        mQrDialogFragment = new QrDialogFragment();
        mQrDialogFragment.show(getFragmentManager(), "sample");
    }

    private void updateAmount() {

        KeyboardFragment keyboardFragment = (KeyboardFragment) fragmentPagerAdapter.getRegisteredFragment(viewPager.getCurrentItem());

        if (keyboardFragment != null) {
            String largeCurrency = amount.getLargeCurrency();
            String smallCurrency = amount.getSmallCurrency();
            keyboardFragment.onLargeAmountUpdate(currencyFormatter.formatLarge(amount.getAmountOf(largeCurrency), largeCurrency));
            keyboardFragment.onSmallAmountUpdate(currencyFormatter.formatSmall(amount.getAmountOf(smallCurrency), smallCurrency));
        }

    }

    @Override
    public void onKeyboardClicked(String input) {
        amount.processInput(input);
        updateAmount();
    }

}

