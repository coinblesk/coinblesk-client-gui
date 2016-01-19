package com.uzh.ckiller.coinblesk_client_gui;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Created by ckiller
 */

/**
 * TODO In the Emulator the bottom row of keypad elements is not correctly shown
 * (Because the emulator device doesnt have hardware buttons) -> whole layout calculations are wrong...
 */



public class NumericKeypadFragment extends Fragment implements View.OnClickListener {

    KeypadClicked mCallback;
    private TextView tvSendAmount;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_numeric_keypad, container, false);

        // Getting the reference to the TextView where the Amount is shown
        tvSendAmount = (TextView) this.getActivity().findViewById(R.id.send_amount_large);

        // Numbers 0 through 9
        TextView tvOne = (TextView) view.findViewById(R.id.keypad_first_row_first_col);
        tvOne.setOnClickListener(this);
        TextView tvTwo = (TextView) view.findViewById(R.id.keypad_first_row_second_col);
        tvTwo.setOnClickListener(this);
        TextView tvThree = (TextView) view.findViewById(R.id.keypad_first_row_third_col);
        tvThree.setOnClickListener(this);
        TextView tvFour = (TextView) view.findViewById(R.id.keypad_second_row_first_col);
        tvFour.setOnClickListener(this);
        TextView tvFive = (TextView) view.findViewById(R.id.keypad_second_row_second_col);
        tvFive.setOnClickListener(this);
        TextView tvSix = (TextView) view.findViewById(R.id.keypad_second_row_third_col);
        tvSix.setOnClickListener(this);
        TextView tvSeven = (TextView) view.findViewById(R.id.keypad_third_row_first_col);
        tvSeven.setOnClickListener(this);
        TextView tvEight = (TextView) view.findViewById(R.id.keypad_third_row_second_col);
        tvEight.setOnClickListener(this);
        TextView tvNine = (TextView) view.findViewById(R.id.keypad_third_row_third_col);
        tvNine.setOnClickListener(this);
        TextView tvDot = (TextView) view.findViewById(R.id.keypad_fourth_row_first_col);
        tvDot.setOnClickListener(this);
        TextView tvZero = (TextView) view.findViewById(R.id.keypad_fourth_row_second_col);
        tvZero.setOnClickListener(this);

        // Special characters (ImageViews)

        return view;
    }

    @Override
    public void onClick(View v) {

        // TODO Fragments should actually not COMMUNICATE DIRECTLY WITH EACH OTHER.
        switch (v.getId()) {
            case R.id.keypad_first_row_first_col:
                Toast.makeText(getActivity(), R.string.keypad_first_row_first_col, Toast.LENGTH_LONG).show();
                mCallback.sendBundle(createBundle("keypad_first_row_first_col"));
                break;

            case R.id.keypad_first_row_second_col:
                Toast.makeText(getActivity(), R.string.keypad_first_row_second_col, Toast.LENGTH_LONG).show();
                mCallback.sendBundle(createBundle("keypad_first_row_second_col"));
                break;

            case R.id.keypad_first_row_third_col:
                Toast.makeText(getActivity(), R.string.keypad_first_row_third_col, Toast.LENGTH_LONG).show();
                mCallback.sendBundle(createBundle("keypad_first_row_third_col"));
                break;

            case R.id.keypad_second_row_first_col:
                Toast.makeText(getActivity(), R.string.keypad_second_row_first_col, Toast.LENGTH_LONG).show();
                mCallback.sendBundle(createBundle("keypad_second_row_first_col"));
                break;

            case R.id.keypad_second_row_second_col:
                Toast.makeText(getActivity(), R.string.keypad_second_row_second_col, Toast.LENGTH_LONG).show();
                mCallback.sendBundle(createBundle("keypad_second_row_second_col"));
                break;

            case R.id.keypad_second_row_third_col:
                Toast.makeText(getActivity(), R.string.keypad_second_row_third_col, Toast.LENGTH_LONG).show();
                mCallback.sendBundle(createBundle("keypad_second_row_third_col"));
                break;

            case R.id.keypad_third_row_first_col:
                Toast.makeText(getActivity(), R.string.keypad_third_row_first_col, Toast.LENGTH_LONG).show();
                mCallback.sendBundle(createBundle("keypad_third_row_first_col"));
                break;

            case R.id.keypad_third_row_second_col:
                Toast.makeText(getActivity(), R.string.keypad_third_row_second_col, Toast.LENGTH_LONG).show();
                mCallback.sendBundle(createBundle("keypad_third_row_second_col"));
                break;

            case R.id.keypad_third_row_third_col:
                Toast.makeText(getActivity(), R.string.keypad_third_row_third_col, Toast.LENGTH_LONG).show();
                mCallback.sendBundle(createBundle("keypad_third_row_third_col"));
                break;

            case R.id.keypad_fourth_row_first_col:
                Toast.makeText(getActivity(), R.string.keypad_fourth_row_first_col, Toast.LENGTH_LONG).show();
                mCallback.sendBundle(createBundle("keypad_fourth_row_first_col"));
                break;

            case R.id.keypad_fourth_row_second_col:
                Toast.makeText(getActivity(), R.string.keypad_fourth_row_second_col, Toast.LENGTH_LONG).show();
                mCallback.sendBundle(createBundle("keypad_fourth_row_second_col"));
                break;

            default:
                break;
        }
    }

    public interface KeypadClicked {
        public void sendBundle(Bundle bundle);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mCallback = (KeypadClicked) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement KeypadClicked");
        }
    }

    @Override
    public void onDetach() {
        mCallback = null; // => avoid leaking
        super.onDetach();
    }

    private Bundle createBundle(String resourceIdentifier) {
        final Bundle bundle = new Bundle();
        int resId = getResourceId(resourceIdentifier, "string", getContext().getPackageName());
        bundle.putString(resourceIdentifier, getContext().getString(resId));
        return bundle;

    }

    public int getResourceId(String pVariableName, String pResourcename, String pPackageName) {
        try {
            return getResources().getIdentifier(pVariableName, pResourcename, pPackageName);
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }


}

