package io.horizontalsystems.bankwallet.modules.balance

import android.animation.ValueAnimator
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import io.horizontalsystems.core.measureHeight
import kotlinx.android.synthetic.main.view_holder_balance_item.*

object BalanceCellAnimator {

    private var bottomCollapsedHeight: Int = 0
    private var lockedBalanceLineHeight: Int = -1
    private var bottomExpandedHeight: Int = -1

    private const val animationPlaybackSpeed: Double = 1.3
    private val listItemExpandDuration: Long get() = (300L / animationPlaybackSpeed).toLong()

    private var measured = false

    fun measureHeights(holder: BalanceItemViewHolder) {
        if (measured) return

        lockedBalanceLineHeight = holder.lockedBalanceWrapper.measureHeight()
        bottomExpandedHeight = holder.buttonsWrapper.measureHeight()
        measured = true
    }

    private fun getBottomHeight(expanded: Boolean, showLocked: Boolean): Int {
        return when {
            expanded && showLocked -> bottomExpandedHeight
            expanded -> bottomExpandedHeight - lockedBalanceLineHeight
            else -> bottomCollapsedHeight
        }
    }

    fun toggleButtons(holder: BalanceItemViewHolder, viewItem: BalanceViewItem, animated: Boolean) {
        val heightSmall = getBottomHeight(false, viewItem.coinValueLocked.visible)
        val heightBig = getBottomHeight(true, viewItem.coinValueLocked.visible)

        if (animated){
            val rootAnimator =
                    if (viewItem.expanded) ValueAnimator.ofFloat(0f, 1f)
                    else ValueAnimator.ofFloat(1f, 0f)

            rootAnimator.duration = listItemExpandDuration
            rootAnimator.interpolator = AccelerateDecelerateInterpolator()
            rootAnimator.addUpdateListener { valueAnimator ->
                val progress = valueAnimator.animatedValue as Float

                setExpandProgress(holder.buttonsWrapper, heightSmall, heightBig, progress)
            }

            rootAnimator.start()
        } else {
            setExpandProgress(holder.buttonsWrapper, heightSmall, heightBig, if (viewItem.expanded) 1f else 0f)
        }

    }


    private fun setExpandProgress(view: View, smallHeight: Int, bigHeight: Int, progress: Float) {
        view.layoutParams.height = (smallHeight + (bigHeight - smallHeight) * progress).toInt()
        view.requestLayout()
    }

}
