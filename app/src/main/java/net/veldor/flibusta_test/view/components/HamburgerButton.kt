package net.veldor.flibusta_test.view.components

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable

/**
 * Simple animating drawable between the "hamburger" icon and cross icon
 *
 * Based on [android.support.v7.graphics.drawable.DrawerArrowDrawable]
 */
class HamburgerButton(
    /** Width and height of the drawable (the drawable is always square) */
    private val size: Int,
    /** Thickness of each individual line */
    private val barThickness: Float,
    /** The space between bars when they are parallel */
    private val barGap: Float
) : Drawable() {

    private val paint = Paint()
    private val thick2 = barThickness / 2.0f

    init {
        paint.style = Paint.Style.STROKE
        paint.strokeJoin = Paint.Join.MITER
        paint.strokeCap = Paint.Cap.BUTT
        paint.isAntiAlias = true

        paint.strokeWidth = barThickness
    }

    override fun draw(canvas: Canvas) {
        if (progress < 0.5) {
            drawHamburger(canvas)
        } else {
            drawCross(canvas)
        }
    }

    private fun drawHamburger(canvas: Canvas) {
        val bounds = bounds
        val centerY = bounds.exactCenterY()
        val left = bounds.left.toFloat() + thick2
        val right = bounds.right.toFloat() - thick2

        // Draw middle line
        canvas.drawLine(
            left, centerY,
            right, centerY,
            paint)

        // Calculate Y offset to top and bottom lines
        val offsetY = barGap * (2 * (0.5f - progress))

        // Draw top & bottom lines
        canvas.drawLine(
            left, centerY - offsetY,
            right, centerY - offsetY,
            paint)
        canvas.drawLine(
            left, centerY + offsetY,
            right, centerY + offsetY,
            paint)
    }

    private fun drawCross(canvas: Canvas) {
        val bounds = bounds
        val centerX = bounds.exactCenterX()
        val centerY = bounds.exactCenterY()
        val crossHeight = barGap * 2 + barThickness * 3
        val crossHeight2 = crossHeight / 2

        // Calculate current cross position
        val distanceY = crossHeight2 * (2 * (progress - 0.5f))
        val top = centerY - distanceY
        val bottom = centerY + distanceY
        val left = centerX - crossHeight2
        val right = centerX + crossHeight2

        // Draw cross
        canvas.drawLine(
            left, top,
            right, bottom,
            paint)
        canvas.drawLine(
            left, bottom,
            right, top,
            paint)
    }

    override fun setAlpha(alpha: Int) {
        if (alpha != paint.alpha) {
            paint.alpha = alpha
            invalidateSelf()
        }
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
        invalidateSelf()
    }

    override fun getIntrinsicWidth(): Int {
        return size
    }

    override fun getIntrinsicHeight(): Int {
        return size
    }

    override fun getOpacity(): Int {
        return PixelFormat.TRANSLUCENT
    }

    /**
     * Drawable color
     * Can be animated
     */
    var color: Int = 0xFFFFFFFF.toInt()
        set(value) {
            field = value
            paint.color = value
            invalidateSelf()
        }

    /**
     * Animate this property to transition from hamburger to cross
     * 0 = hamburger
     * 1 = cross
     */
    var progress: Float = 0.0f
        set(value) {
            field = value.coerceIn(0.0f, 1.0f)
            invalidateSelf()
        }

}