package io.horizontalsystems.bankwallet.modules.intro

import android.content.Context
import android.view.animation.Interpolator
import android.widget.Scroller

class ViewPagerScroller : Scroller {
    private val mScrollDuration = 600

    constructor(context: Context?) : super(context) {}
    constructor(context: Context?, interpolator: Interpolator?) : super(context, interpolator) {}

    override fun startScroll(startX: Int, startY: Int, dx: Int, dy: Int, duration: Int) {
        super.startScroll(startX, startY, dx, dy, mScrollDuration)
    }

    override fun startScroll(startX: Int, startY: Int, dx: Int, dy: Int) {
        super.startScroll(startX, startY, dx, dy, mScrollDuration)
    }
}
