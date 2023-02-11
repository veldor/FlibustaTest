package net.veldor.flibusta_test.view.components

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
import android.os.Build
import android.view.animation.Animation
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import net.veldor.flibusta_test.R

public class CircleImageViewKotlin(context: Context) : AppCompatImageView(context) {
    var mContext: Context
    private var mListener: Animation.AnimationListener? = null
    private val mShadowRadius: Int
    private var mBackgroundColor: Int
    private fun elevationSupported(): Boolean {
        return Build.VERSION.SDK_INT >= 21
    }

    init {
        mContext = context
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        if (!elevationSupported()) {
            setMeasuredDimension(
                measuredWidth + mShadowRadius * 2, measuredHeight
                        + mShadowRadius * 2
            )
        }
    }

    fun setAnimationListener(listener: Animation.AnimationListener?) {
        mListener = listener
    }

    public override fun onAnimationStart() {
        super.onAnimationStart()
        if (mListener != null && animation != null) {
            mListener!!.onAnimationStart(animation)
        }
    }

    public override fun onAnimationEnd() {
        super.onAnimationEnd()
        if (mListener != null && animation != null) {
            mListener!!.onAnimationEnd(animation)
        }
    }

    override fun setBackgroundColor(color: Int) {
        if (background is ShapeDrawable) {
            (background as ShapeDrawable).paint.color = color
            mBackgroundColor = color
        }
    }

    fun getBackgroundColor(): Int {
        return mBackgroundColor
    }

    private inner class OvalShadow internal constructor(
        private val mCircleImageView: CircleImageViewKotlin,
        shadowRadius: Int
    ) :
        OvalShape() {
        private val mShadowPaint: Paint
        private val mShadowRadius: Int
        override fun onResize(width: Float, height: Float) {
            super.onResize(width, height)
            updateRadialGradient(width.toInt())
        }

        override fun draw(canvas: Canvas, paint: Paint) {
            val x = mCircleImageView.width / 2
            val y = mCircleImageView.height / 2
            canvas.drawCircle(x.toFloat(), y.toFloat(), x.toFloat(), mShadowPaint)
            canvas.drawCircle(x.toFloat(), y.toFloat(), (x - mShadowRadius).toFloat(), paint)
        }

        private fun updateRadialGradient(diameter: Int) {
            mShadowPaint.shader = RadialGradient(
                (diameter / 2).toFloat(),
                (diameter / 2).toFloat(),
                mShadowRadius.toFloat(),
                ResourcesCompat.getColor(resources, R.color.colorPrimary, mContext.theme),
                ResourcesCompat.getColor(resources, R.color.colorPrimaryDark, mContext.theme),
                Shader.TileMode.CLAMP
            )
        }

        init {
            mShadowPaint = Paint()
            mShadowRadius = shadowRadius
            updateRadialGradient(rect().width().toInt())
        }
    }

    companion object {
        private const val DEFAULT_BACKGROUND_COLOR = -0x50506
        private const val FILL_SHADOW_COLOR = 0x3D000000
        private const val KEY_SHADOW_COLOR = 0x1E000000

        // PX
        private const val X_OFFSET = 0f
        private const val Y_OFFSET = 1.75f
        private const val SHADOW_RADIUS = 3.5f
        private const val SHADOW_ELEVATION = 4
    }

    init {
        val density = getContext().resources.displayMetrics.density
        val shadowYOffset = (density * Y_OFFSET).toInt()
        val shadowXOffset = (density * X_OFFSET).toInt()
        mShadowRadius = (density * SHADOW_RADIUS).toInt()

        // The style attribute is named SwipeRefreshLayout instead of CircleImageView because
        // CircleImageView is not part of the public api.
        @SuppressLint("CustomViewStyleable") val colorArray =
            getContext().obtainStyledAttributes(androidx.swiperefreshlayout.R.styleable.SwipeRefreshLayout)
        mBackgroundColor = ResourcesCompat.getColor(resources, R.color.colorPrimary, mContext.theme)
        colorArray.recycle()
        val circle: ShapeDrawable
        if (elevationSupported()) {
            circle = ShapeDrawable(OvalShape())
            ViewCompat.setElevation(this, SHADOW_ELEVATION * density)
        } else {
            circle = ShapeDrawable(OvalShadow(this, mShadowRadius))
            setLayerType(LAYER_TYPE_SOFTWARE, circle.paint)
            circle.paint.setShadowLayer(
                mShadowRadius.toFloat(), shadowXOffset.toFloat(), shadowYOffset.toFloat(),
                KEY_SHADOW_COLOR
            )
            val padding = mShadowRadius
            // set padding so the inner image sits correctly within the shadow.
            setPadding(padding, padding, padding, padding)
        }
        circle.paint.color = mBackgroundColor
        ViewCompat.setBackground(this, circle)
    }
}