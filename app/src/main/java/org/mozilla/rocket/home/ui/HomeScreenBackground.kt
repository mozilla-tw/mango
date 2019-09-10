/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.rocket.home.ui

import android.app.Activity
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ComposeShader
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.Rect
import android.graphics.Shader
import android.util.AttributeSet
import org.mozilla.focus.R
import org.mozilla.focus.utils.ViewUtils
import org.mozilla.rocket.nightmode.themed.ThemedImageView
import org.mozilla.rocket.theme.ThemeManager

class HomeScreenBackground : ThemedImageView, ThemeManager.Themeable {
    private lateinit var paint: Paint
    private var isNight = false

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs, 0) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init()
    }

    internal fun init() {
        val rect = Rect()
        (context as Activity).window.decorView.getWindowVisibleDisplayFrame(rect)
        val bitmap = BitmapFactory.decodeResource(resources, R.drawable.home_pattern)
        paint = Paint()
        val shader1 = BitmapShader(bitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
        val colors = intArrayOf(Color.parseColor("#99FFFFFF"), Color.parseColor("#4dFFFFFF"), Color.parseColor("#1aFFFFFF"), Color.parseColor("#00FFFFFF"))
        val positions = floatArrayOf(0.0f, 0.4f, 0.7f, 1f)
        val shader2 = LinearGradient(0f, rect.top.toFloat(), 0f, rect.bottom.toFloat(), colors, positions, Shader.TileMode.CLAMP)
        paint.shader = ComposeShader(shader2, shader1, PorterDuff.Mode.MULTIPLY)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (this.isNight) {
            // Add status bar's height as a padding on top to let HomeFragment star background align with TabTrayFragment
            setPadding(0, ViewUtils.getStatusBarHeight(context as Activity), 0, 0)
        } else {
            setPadding(0, 0, 0, 0)
        }
    }

    override fun onThemeChanged() {
        val drawable = context.theme.getDrawable(R.drawable.bg_homescreen_color)
        background = drawable
    }

    override fun setNightMode(isNight: Boolean) {
        super.setNightMode(isNight)
        this.isNight = isNight
        if (this.isNight) {
            setImageResource(R.drawable.star_bg)
        } else {
            setImageResource(R.drawable.firefox_lite_bg)
        }
    }
}
