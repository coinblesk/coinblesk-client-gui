package com.coinblesk.client;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 15/02/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
public interface OnKeyboardListener {
    public void onDigit(int digit);
    public void onDot();
    public void onEnter();
    public void onCustom(int digit);
    public void onPlus(String value);
}
