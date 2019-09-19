package org.mozilla.rocket.widget

import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import androidx.core.content.ContextCompat
import android.util.AttributeSet
import android.view.View
import org.mozilla.focus.R
import org.mozilla.focus.utils.ViewUtils

class RecFocusView : View {
    private val transparentPaint: Paint = Paint()
    private val path = Path()
    private var startX: Int = 0
    private var startY: Int = 0
    private var statusBarOffset: Int = 0
    private var _width: Int = 0
    private var _height: Int = 0

    constructor(context: Context) : super(context) {
        initPaints()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        initPaints()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        initPaints()
    }

    /** FocusView will draw a spotlight circle(total transparent) at coordinates X = centerX and Y = centerY with radius.
     * The view's background except the spotlight circle is half-transparent. */
    constructor(context: Context, startX: Int, startY: Int, width: Int, height: Int) : super(context) {
        this.startX = startX
        this.startY = startY
        this.statusBarOffset = ViewUtils.getStatusBarHeight(context as Activity)
        this._width = width
        this._height = height

        initPaints()
    }

    private fun initPaints() {
        transparentPaint.color = Color.TRANSPARENT
        transparentPaint.strokeWidth = 10f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        path.reset()

        path.addRect(startX.toFloat(), (startY - statusBarOffset).toFloat(), (startX + _width).toFloat(), (startY - statusBarOffset + _height).toFloat(), Path.Direction.CW)
        path.fillType = Path.FillType.INVERSE_EVEN_ODD

        canvas.drawRect(startX.toFloat(), (startY - statusBarOffset).toFloat(), (startX + _width).toFloat(), (startY + _height).toFloat(), transparentPaint)
        canvas.clipPath(path)
        canvas.drawColor(ContextCompat.getColor(context, R.color.myShotOnBoardingBackground))
    }
}
