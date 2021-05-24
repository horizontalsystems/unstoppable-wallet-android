package io.horizontalsystems.bankwallet.ui.extensions

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.recyclerview.widget.RecyclerView


class ControlledRecyclerView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : RecyclerView(context, attrs, defStyleAttr) {

    private var verticalScrollingEnabled = true

    fun enableVerticalScroll(enabled: Boolean) {
        verticalScrollingEnabled = enabled
    }

    fun isVerticalScrollingEnabled(): Boolean {
        return verticalScrollingEnabled
    }

    override fun computeVerticalScrollRange(): Int {
        return if (isVerticalScrollingEnabled()) super.computeVerticalScrollRange() else 0
    }

    override fun onInterceptTouchEvent(e: MotionEvent?): Boolean {
        return if (isVerticalScrollingEnabled()) super.onInterceptTouchEvent(e) else false
    }
}
