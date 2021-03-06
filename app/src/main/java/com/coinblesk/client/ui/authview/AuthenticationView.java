/*
 * Copyright 2016 The Coinblesk team and the CSG Group at University of Zurich
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */

package com.coinblesk.client.ui.authview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.Point;
import android.util.Log;
import android.view.View;

import com.coinblesk.client.R;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Alessandro De Carli
 * @author Andreas Albrecht
 */
public class AuthenticationView extends View {

    private static final String TAG = AuthenticationView.class.getName();

    private final byte[] digest;
    private final List<Point> points = new ArrayList<Point>();
    private final Paint dotPaint = new Paint();
    private final Paint patternPaint = new Paint();

    public AuthenticationView(Context context, byte[] key) {
        super(context);
        byte[] digest = new byte[0];
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            messageDigest.update(key);
            digest = messageDigest.digest();
        } catch (NoSuchAlgorithmException e) {
            Log.w(TAG, "MessageDigest algorithm not found.");
        }

        this.digest = digest;
        this.dotPaint.setColor(getContext().getResources().getColor(R.color.colorPrimaryDark));
        this.dotPaint.setAntiAlias(true);
        this.dotPaint.setStyle(Paint.Style.FILL);

        this.patternPaint.setColor(Color.GREEN);
        this.patternPaint.setAntiAlias(true);
        this.patternPaint.setStyle(Paint.Style.FILL);
        this.patternPaint.setStrokeJoin(Paint.Join.ROUND);    // set the join to round you want
        this.patternPaint.setStrokeCap(Paint.Cap.ROUND);      // set the paint cap to round too
        this.patternPaint.setPathEffect(new CornerPathEffect(10));
        this.patternPaint.setDither(true);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(View.MeasureSpec.getSize(widthMeasureSpec), View.MeasureSpec.getSize(widthMeasureSpec));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(generateDigestColor());

        int squareSize = Math.min(canvas.getHeight(), canvas.getWidth());

        int cellSize = squareSize / 4;
        int cellPadding = cellSize / 2;
        int circleSize = cellPadding / 8;

        patternPaint.setColor(getComplementColor(generateDigestColor()));
        patternPaint.setStrokeWidth(circleSize * 2);

        for (int i = 0; i < 4; i++) {
            for (int k = 0; k < 4; k++) {
                canvas.drawCircle(cellPadding + cellSize * i, cellPadding + cellSize * k, circleSize, dotPaint);
                points.add(new Point(cellPadding + cellSize * i, cellPadding + cellSize * k));
            }
        }

        for (int i = 6; i < 32; i++) {
            if (isBitSet((byte)((int)digest[i]&digest[i-1]), i % 8)) {
                final int currentNumber = toHalfByteInt(digest[i]);
                final int lastNumber = toOtherHalfByteInt(digest[i]);
                canvas.drawLine(
                        points.get(lastNumber).x, points.get(lastNumber).y,         /* start */
                        points.get(currentNumber).x, points.get(currentNumber).y,   /* end */
                        patternPaint);
            }
        }
    }

    private int generateDigestColor() {
        final int baseColor = Color.WHITE;

        final int baseRed = Color.red(baseColor);
        final int baseGreen = Color.green(baseColor);
        final int baseBlue = Color.blue(baseColor);

        final int red = (baseRed + toUnsignedInt(digest[0])) / 2;
        final int green = (baseGreen + toUnsignedInt(digest[1])) / 2;
        final int blue = (baseBlue + toUnsignedInt(digest[2])) / 2;

        return Color.rgb(red, green, blue);
    }

    private int getComplementColor(int color) {
        return Color.rgb(
                255 - Color.red(color),
                255 - Color.green(color),
                255 - Color.blue(color));
    }

    private int toUnsignedInt(byte x) {
        return ((int) x) & 0xff;
    }

    private boolean isBitSet(byte value, int bitIndex) {
        return (value & (1 << bitIndex)) != 0;
    }

    private int toHalfByteInt(byte x) {
        // get low order nibble, 4 bits.
        return ((int) x) & 0xf;
    }

    private int toOtherHalfByteInt(byte x) {
        // get high order nibble, 4 bits and move to low order.
        return ((int) x) >> 4 & 0xf;
    }

}
