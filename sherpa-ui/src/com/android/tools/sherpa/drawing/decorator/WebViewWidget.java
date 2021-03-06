/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.tools.sherpa.drawing.decorator;

import com.android.tools.sherpa.drawing.ViewTransform;
import android.support.constraint.solver.widgets.ConstraintWidget;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

/**
 * WebView widget decorator
 */
public class WebViewWidget extends WidgetDecorator {
    protected int mPadding = 5;
    private Font mFont = new Font("Helvetica", Font.PLAIN, 12);

    /**
     * Base constructor
     *
     * @param widget the widget we are decorating
     */
    public WebViewWidget(ConstraintWidget widget) {
        super(widget);
        wrapContent();
    }

    /**
     * Utility method computing the size of the widget if dimensions are set
     * to wrap_content, using the default font
     */
    protected void wrapContent() {
        mWidget.setMinWidth(100);
        mWidget.setMinHeight(100);
        int tw = mWidget.getMinWidth();
        int th = mWidget.getMinHeight();
        if (mWidget.getHorizontalDimensionBehaviour()
                == ConstraintWidget.DimensionBehaviour.WRAP_CONTENT) {
            mWidget.setWidth(tw);
        }
        if (mWidget.getVerticalDimensionBehaviour()
                == ConstraintWidget.DimensionBehaviour.WRAP_CONTENT) {
            mWidget.setHeight(th);
        }
        if (mWidget.getHorizontalDimensionBehaviour() ==
                ConstraintWidget.DimensionBehaviour.FIXED) {
            if (mWidget.getWidth() <= mWidget.getMinWidth()) {
                mWidget.setHorizontalDimensionBehaviour(
                        ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
            }
        }
        if (mWidget.getVerticalDimensionBehaviour() == ConstraintWidget.DimensionBehaviour.FIXED) {
            if (mWidget.getHeight() <= mWidget.getMinHeight()) {
                mWidget.setVerticalDimensionBehaviour(
                        ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
            }
        }
        mWidget.setBaselineDistance(0);
    }

    @Override
    public void onPaintBackground(ViewTransform transform, Graphics2D g) {
        super.onPaintBackground(transform, g);
        if (mColorSet.drawBackground()) {
            fakeUIPaint(transform,g, mWidget.getDrawX(), mWidget.getDrawY());
        }
    }

    protected void fakeUIPaint(ViewTransform transform, Graphics2D g, int x, int y) {
        int tx = transform.getSwingX(x);
        int ty = transform.getSwingY(y);
        int w = transform.getSwingDimension(mWidget.getDrawWidth());
        int h = transform.getSwingDimension(mWidget.getDrawHeight());

        int padding = transform.getSwingDimension(mPadding);
        int originalSize = mFont.getSize();
        int scaleSize = transform.getSwingDimension(originalSize);
        g.setFont(mFont.deriveFont((float) scaleSize));
        FontMetrics fontMetrics = g.getFontMetrics();
        g.setColor(Color.WHITE);

        g.drawString("WWW", tx + padding, ty + fontMetrics.getAscent() + padding);
        String text = "WebView";
        Rectangle2D bounds = fontMetrics.getStringBounds(text, g);
        g.drawString(text, tx + (int) ((w - bounds.getWidth()) / 2f), ty + (int) ((h - bounds.getHeight()) / 2f));
    }
}
