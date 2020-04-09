package io.horizontalsystems.bankwallet.modules.balance

import android.animation.ValueAnimator
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.core.view.isVisible
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.core.measureHeight
import kotlinx.android.synthetic.main.view_holder_coin.*

object BalanceCellAnimator {

    private var lockedBalanceWrapperHeight: Int = -1
    private var balanceWrapperHeight: Int = -1

    private var collapsedHeight: Int = -1
    private var expandedHeight: Int = -1
    private var expandedHeightBalanceHidden: Int = -1
    private var collapsedBalanceHiddenHeight: Int = -1

    private val animationPlaybackSpeed: Double = 1.3
    private val listItemExpandDuration: Long get() = (300L / animationPlaybackSpeed).toLong()

    private var measured = false

    fun measureHeights(containerView: View) {
        if (measured) return

        balanceWrapperHeight = containerView.findViewById<FrameLayout>(R.id.balanceWrapper)?.measureHeight() ?: -1
        lockedBalanceWrapperHeight = containerView.findViewById<FrameLayout>(R.id.lockedBalanceWrapper)?.measureHeight() ?: -1

        expandedHeight = containerView.measureHeight()
        expandedHeightBalanceHidden = expandedHeight - balanceWrapperHeight - lockedBalanceWrapperHeight

        val buttonsWrapperHeight = containerView.findViewById<ConstraintLayout>(R.id.buttonsWrapper)?.measureHeight() ?: -1
        val borderHeight = containerView.findViewById<FrameLayout>(R.id.border)?.measureHeight() ?: -1
        collapsedHeight = expandedHeight - buttonsWrapperHeight
        collapsedBalanceHiddenHeight = expandedHeight - buttonsWrapperHeight - balanceWrapperHeight - borderHeight

        measured = true
    }

    private fun getHeight(expanded: Boolean, balanceHidden: Boolean, showLocked: Boolean): Int {
        if (expanded) {
            if (balanceHidden) {
                return expandedHeightBalanceHidden
            } else {
                return if (showLocked) expandedHeight else expandedHeight - lockedBalanceWrapperHeight
            }
        } else {
            return if (balanceHidden) collapsedBalanceHiddenHeight else collapsedHeight
        }
    }

    fun toggleBalanceAndButtons(holder: ViewHolderCoin, viewItem: BalanceViewItem) {
        val smallHeight = getHeight(false, true, viewItem.coinValueLocked.visible)
        val bigHeight = getHeight(viewItem.expanded, viewItem.hideBalance, viewItem.coinValueLocked.visible)

        holder.balanceCoinLocked.isVisible = viewItem.coinValueLocked.visible && !viewItem.hideBalance
        holder.balanceFiatLocked.isVisible = viewItem.coinValueLocked.visible && !viewItem.hideBalance

        setExpandProgress(holder.balanceWrapper, 0, balanceWrapperHeight, if(!viewItem.hideBalance) 1f else 0f)

        val forwardAnimation = viewItem.expanded || !viewItem.hideBalance
        setExpandProgress(holder.rootWrapper, smallHeight, bigHeight, if (forwardAnimation) 1f else 0f)
    }

    fun toggleBalanceAndButtonsAnimate(holder: ViewHolderCoin, current: BalanceViewItem, prev: BalanceViewItem) {
        val toggleBalance = current.hideBalance != prev.hideBalance
        val toggleActions = !toggleBalance && current.expanded != prev.expanded

        val animationForward = when {
            toggleBalance -> !current.hideBalance
            toggleActions -> current.expanded
            else -> true
        }

        var heightSmall = 0
        var heightBig = 0

        when {
            toggleBalance -> {
                heightSmall = getHeight(current.expanded, true, current.coinValueLocked.visible)
                heightBig = getHeight(current.expanded, false, current.coinValueLocked.visible)
            }
            toggleActions -> {
                heightSmall = getHeight(false, current.hideBalance, current.coinValueLocked.visible)
                heightBig = getHeight(true, current.hideBalance, current.coinValueLocked.visible)
            }
        }

        val rootAnimator =
                if (animationForward) ValueAnimator.ofFloat(0f, 1f)
                else ValueAnimator.ofFloat(1f, 0f)

        rootAnimator.duration = listItemExpandDuration
        rootAnimator.interpolator = AccelerateDecelerateInterpolator()
        rootAnimator.addUpdateListener { valueAnimator ->
            val progress = valueAnimator.animatedValue as Float

            if (toggleBalance) {
                setExpandProgress(holder.balanceWrapper, 0, balanceWrapperHeight, progress)
                if (current.coinValueLocked.visible) {
                    setExpandProgress(holder.lockedBalanceWrapper, 0, lockedBalanceWrapperHeight, progress)
                }
            }

            setExpandProgress(holder.rootWrapper, heightSmall, heightBig, progress)
        }

        if (animationForward) { //expand animation
            rootAnimator.doOnStart {
                if (toggleBalance || toggleActions) {
                    holder.buttonsWrapper.isVisible = true
                }
            }
            rootAnimator.doOnEnd {
                holder.balanceCoinLocked.isVisible = current.coinValueLocked.visible && !current.hideBalance
                holder.balanceFiatLocked.isVisible = current.coinValueLocked.visible && !current.hideBalance
            }
        } else { //collapse animation
            rootAnimator.doOnStart {
                if (toggleBalance) {
                    if (current.coinValueLocked.visible) {
                        holder.balanceCoinLocked.isVisible = false
                        holder.balanceFiatLocked.isVisible = false
                    }
                }
            }
            rootAnimator.doOnEnd {
                if (toggleActions) {
                    holder.buttonsWrapper.isVisible = false
                }
            }
        }

        rootAnimator.start()
    }


    private fun setExpandProgress(view: View, smallHeight: Int, bigHeight: Int, progress: Float) {
        view.layoutParams.height = (smallHeight + (bigHeight - smallHeight) * progress).toInt()
        view.requestLayout()
    }


}