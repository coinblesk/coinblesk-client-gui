package com.coinblesk.client.utils;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Switch;

/**
 * Ugly hack, due to a bug in Android:
 * http://stackoverflow.com/questions/27986154/multiple-switch-settexton-not-updating-switchs-text
 *
 * @author Thomas Bocek
 */
public class UpdatableSwitch extends Switch {

    public UpdatableSwitch(Context context) {
        super(context);
    }

    public UpdatableSwitch(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public UpdatableSwitch(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public UpdatableSwitch(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public void requestLayout() {
        try {
            java.lang.reflect.Field mOnLayout = Switch.class.getDeclaredField("mOnLayout");
            mOnLayout.setAccessible(true);
            mOnLayout.set(this, null);
            java.lang.reflect.Field mOffLayout = Switch.class.getDeclaredField("mOffLayout");
            mOffLayout.setAccessible(true);
            mOffLayout.set(this, null);
        } catch (Exception x) {
            x.printStackTrace();
        }
        super.requestLayout();
    }
}
