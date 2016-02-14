package com.uzh.ckiller.coinblesk_client_gui;

import android.app.Activity;
import android.content.res.Configuration;
import android.media.Image;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.SpannableString;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Created by ckiller on 24/01/16.
 */

public class KeyboardFragment extends Fragment implements View.OnClickListener {

    public static final String ARG_PAGE = "ARG_PAGE";
    int mScreenLayout;
    KeyboardClicked mCallback;
    SwipeRefreshLayout mSwipeRefreshLayout;
    private Handler handler = new Handler();
    View view;
    private int mPage;


    public static KeyboardFragment newInstance(int page) {
        Bundle args = new Bundle();
        args.putInt(ARG_PAGE, page);
        KeyboardFragment fragment = new KeyboardFragment();
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPage = getArguments().getInt(ARG_PAGE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_keyboard, container, false);

        mScreenLayout = getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK;
        switch (mScreenLayout) {
            case Configuration.SCREENLAYOUT_SIZE_SMALL:
            case Configuration.SCREENLAYOUT_SIZE_NORMAL:
            case Configuration.SCREENLAYOUT_UNDEFINED:
                initStandard(view);
                break;
            case Configuration.SCREENLAYOUT_SIZE_LARGE:
            case Configuration.SCREENLAYOUT_SIZE_XLARGE:
                initLarge(view);
                break;
            default:
                initStandard(view);
                break;

        }

        mSwipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.keyboard_swipe_refresh_layout);
        try {
            mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    //TODO: Refresh Exchange Rate Here
                    handler.post(refreshing);
                }
            });

            // sets the colors used in the refresh animation
            mSwipeRefreshLayout.setColorSchemeResources(R.color.colorAccent, R.color.material_lime_A100,
                    R.color.material_lime_A400, R.color.material_lime_A400);

        } catch (NullPointerException e) {
            e.printStackTrace();
        }

        return view;
    }
/*
    // http://stackoverflow.com/a/27073879
    @Override
    public void onPause() {
        super.onPause();

        if (mSwipeRefreshLayout != null) {
            mSwipeRefreshLayout.setRefreshing(false);
            mSwipeRefreshLayout.destroyDrawingCache();
            mSwipeRefreshLayout.clearAnimation();
        }
    }*/

    private void initStandard(View view) {

        // Numbers '0 1 2 3 4 5 6 7 8 9,' and '.'
        TextView tvOne = (TextView) view.findViewById(R.id.key_one);
        tvOne.setOnClickListener(this);
        TextView tvTwo = (TextView) view.findViewById(R.id.key_two);
        tvTwo.setOnClickListener(this);
        TextView tvThree = (TextView) view.findViewById(R.id.key_three);
        tvThree.setOnClickListener(this);
        TextView tvFour = (TextView) view.findViewById(R.id.key_four);
        tvFour.setOnClickListener(this);
        TextView tvFive = (TextView) view.findViewById(R.id.key_five);
        tvFive.setOnClickListener(this);
        TextView tvSix = (TextView) view.findViewById(R.id.key_six);
        tvSix.setOnClickListener(this);
        TextView tvSeven = (TextView) view.findViewById(R.id.key_seven);
        tvSeven.setOnClickListener(this);
        TextView tvEight = (TextView) view.findViewById(R.id.key_eight);
        tvEight.setOnClickListener(this);
        TextView tvNine = (TextView) view.findViewById(R.id.key_nine);
        tvNine.setOnClickListener(this);
        TextView tvDot = (TextView) view.findViewById(R.id.key_dot);
        tvDot.setOnClickListener(this);
        TextView tvZero = (TextView) view.findViewById(R.id.key_zero);
        tvZero.setOnClickListener(this);

        // Backspace Button
        ImageView ivBackspace = (ImageView) view.findViewById(R.id.amount_backspace_image_view);
        ivBackspace.setOnClickListener(this);

        // Switch Currency Button
        ImageView ivSwitchCurrencies = (ImageView) view.findViewById(R.id.amount_switch_image_view);
        ivSwitchCurrencies.setOnClickListener(this);

        ImageView ivAccept = (ImageView) view.findViewById(R.id.key_accept);
        ivAccept.setOnClickListener(this);
    }

    private void initLarge(View view) {

        // Numbers '00 0 1 2 3 4 5 6 7 8 9,' and '.'
        TextView tvOne = (TextView) view.findViewById(R.id.key_one);
        tvOne.setOnClickListener(this);
        TextView tvTwo = (TextView) view.findViewById(R.id.key_two);
        tvTwo.setOnClickListener(this);
        TextView tvThree = (TextView) view.findViewById(R.id.key_three);
        tvThree.setOnClickListener(this);
        TextView tvFour = (TextView) view.findViewById(R.id.key_four);
        tvFour.setOnClickListener(this);
        TextView tvFive = (TextView) view.findViewById(R.id.key_five);
        tvFive.setOnClickListener(this);
        TextView tvSix = (TextView) view.findViewById(R.id.key_six);
        tvSix.setOnClickListener(this);
        TextView tvSeven = (TextView) view.findViewById(R.id.key_seven);
        tvSeven.setOnClickListener(this);
        TextView tvEight = (TextView) view.findViewById(R.id.key_eight);
        tvEight.setOnClickListener(this);
        TextView tvNine = (TextView) view.findViewById(R.id.key_nine);
        tvNine.setOnClickListener(this);
        TextView tvDot = (TextView) view.findViewById(R.id.key_dot);
        tvDot.setOnClickListener(this);
        TextView tvZero = (TextView) view.findViewById(R.id.key_zero);
        tvZero.setOnClickListener(this);
        TextView tvZeroZero = (TextView) view.findViewById(R.id.key_zero_zero);
        tvZeroZero.setOnClickListener(this);

        // Backspace Button
        ImageView ivBackspace = (ImageView) view.findViewById(R.id.amount_backspace_image_view);
        ivBackspace.setOnClickListener(this);

        // Switch Currency Button
        ImageView ivSwitchCurrencies = (ImageView) view.findViewById(R.id.amount_switch_image_view);
        ivSwitchCurrencies.setOnClickListener(this);

        // CLEAR, multiply, plus and accept
        TextView tvClear = (TextView) view.findViewById(R.id.key_clear);
        tvClear.setOnClickListener(this);
        TextView tvMultiply = (TextView) view.findViewById(R.id.key_multiply);
        tvMultiply.setOnClickListener(this);
        TextView tvPlus = (TextView) view.findViewById(R.id.key_plus);
        tvPlus.setOnClickListener(this);

    }


    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.key_one:
                mCallback.onKeyboardClicked("1");
                break;

            case R.id.key_two:
                mCallback.onKeyboardClicked("2");
                break;

            case R.id.key_three:
                mCallback.onKeyboardClicked("3");
                break;

            case R.id.key_four:
                mCallback.onKeyboardClicked("4");
                break;

            case R.id.key_five:
                mCallback.onKeyboardClicked("5");
                break;

            case R.id.key_six:
                mCallback.onKeyboardClicked("6");
                break;

            case R.id.key_seven:
                mCallback.onKeyboardClicked("7");
                break;

            case R.id.key_eight:
                mCallback.onKeyboardClicked("8");
                break;

            case R.id.key_nine:
                mCallback.onKeyboardClicked("9");
                break;

            case R.id.key_dot:
                mCallback.onKeyboardClicked(".");
                break;

            case R.id.key_zero:
                mCallback.onKeyboardClicked("0");
                break;

            case R.id.amount_backspace_image_view:
                mCallback.onKeyboardClicked("backspace");
                break;

            case R.id.amount_switch_image_view:
                mCallback.onKeyboardClicked("switch");
                break;

            case R.id.key_accept:
                mCallback.onKeyboardClicked("accept");

            default:
                break;
        }
    }

    public interface KeyboardClicked {
        public void onKeyboardClicked(String string);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mCallback = (KeyboardClicked) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement KeyboardClicked");
        }
    }

    @Override
    public void onDetach() {
        mCallback = null; // => avoid leaking
        super.onDetach();
    }

    public void onSmallAmountUpdate(SpannableString value) {
        final TextView tvSmall = (TextView) view.findViewById(R.id.amount_small_text_view);
        tvSmall.setText(value);
    }

    public void onLargeAmountUpdate(SpannableString value) {
        final TextView tvLarge = (TextView) view.findViewById(R.id.amount_large_text_view);
        tvLarge.setText(value);
    }

    // Code partly from https://yassirh.com/2014/05/how-to-use-swiperefreshlayout-the-right-way/
    // and here: http://stackoverflow.com/a/28173911

    private final Runnable refreshing = new Runnable() {
        public void run() {
            try {
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mSwipeRefreshLayout.setRefreshing(false);
                    }
                }, 5000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };
}


