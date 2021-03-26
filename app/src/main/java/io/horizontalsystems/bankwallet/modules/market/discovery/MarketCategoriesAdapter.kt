package io.horizontalsystems.bankwallet.modules.market.discovery

import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.core.view.isInvisible
import com.google.android.material.tabs.TabLayout
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.views.helpers.LayoutHelper
import kotlin.math.max

class MarketCategoriesAdapter(
        private val context: Context,
        private val tabLayout: TabLayout,
        private val marketCategories: List<MarketCategory>,
        private val listener: Listener,
) : TabLayout.OnTabSelectedListener {

    private val itemViewMaxLength = LayoutHelper.dp(212f, context)
    private val itemViewMinLength = LayoutHelper.dp(98f, context)
    private val activeColor = context.getColor(R.color.yellow_d)
    private val inactiveColor = context.getColor(R.color.lawrence)

    init {
        marketCategories.forEach { category ->
            tabLayout.newTab()
                    .setCustomView(LayoutInflater.from(context).inflate(R.layout.view_market_category, null))
                    .setText(category.titleResId)
                    .setIcon(category.iconResId)
                    .setDescription(category.descriptionResId)
                    .let {
                        tabLayout.addTab(it, false)
                    }
        }
        tabLayout.setSelectedTabIndicator(null)
        tabLayout.tabRippleColor = null
        tabLayout.addOnTabSelectedListener(this)
    }

    fun selectCategory(category: MarketCategory?) {
        val tabIndex = marketCategories.indexOf(category)
        val tabToBeSelected = tabLayout.getTabAt(tabIndex)
        tabLayout.selectTab(tabToBeSelected)

        if (tabToBeSelected != null) {
            onTabSelected(tabToBeSelected, true)
        } else {
            listener.onSelect(null)
        }
    }

    private fun onTabSelected(tab: TabLayout.Tab, isInitial: Boolean) {
        tab.customView?.isActivated = true

        listener.onSelect(marketCategories.getOrNull(tab.position))

        //hide icon
        val iconView = tab.view.findViewById<ImageView>(android.R.id.icon)
        animateIcon(iconView, 1f, 0f, 100L)

        //translate title to top
        animateTitle(tab.view, 150L, isInitial)

        //expand layout
        val lastTabPosition = if (tab.position == tabLayout.tabCount - 1) tab.position else null
        animateTabWidth(tab.view, itemViewMaxLength, 300L, lastTabPosition)

        //show description
        animateDescription(tab, 0f, 1f, 250L)

        //color animation
        tab.customView?.let { animateTabColor(it, inactiveColor, activeColor, 300L) }
    }

    override fun onTabSelected(tab: TabLayout.Tab) {
        onTabSelected(tab, false)
    }

    override fun onTabUnselected(tab: TabLayout.Tab) {
        tab.customView?.isActivated = false

        //hide description
        animateDescription(tab, 1f, 0f, 50L)

        //show icon
        val iconView = tab.view.findViewById<ImageView>(android.R.id.icon)
        animateIcon(iconView, 0f, 1f, 200L)

        //translate title to bottom
        animateTitle(tab.view, 200L)

        //collapse layout
        val titleTextView = tab.view.findViewById<TextView>(android.R.id.text1)
        val toWidth = max(titleTextView.width + LayoutHelper.dp(40f, context), itemViewMinLength)
        animateTabWidth(tab.view, toWidth, 300L)

        //color animation
        tab.customView?.let { animateTabColor(it, activeColor, inactiveColor, 200L) }
    }

    private fun animateIcon(view: View, fromAlpha: Float, toAlpha: Float, animDuration: Long) {
        ObjectAnimator.ofFloat(view, ImageView.ALPHA, fromAlpha, toAlpha).apply {
            duration = animDuration
            start()
        }
    }

    private fun animateDescription(tab: TabLayout.Tab, fromAlpha: Float, toAlpha: Float, animDuration: Long) {
        val descriptionTextView = tab.view.findViewById<TextView>(R.id.description)

        ValueAnimator.ofFloat(fromAlpha, toAlpha).apply {
            addUpdateListener { valueAnimator ->
                descriptionTextView.alpha = valueAnimator.animatedValue as Float
            }
            doOnStart { if (toAlpha == 1f) descriptionTextView.visibility = View.VISIBLE }
            doOnEnd { if (toAlpha == 0f) descriptionTextView.visibility = View.GONE }
            duration = animDuration
            start()
        }
    }

    private fun animateTitle(tabView: TabLayout.TabView, animDuration: Long, isInitial: Boolean = false) {
        val titleTextView = tabView.findViewById<TextView>(android.R.id.text1)
        val topBorder = tabView.findViewById<View>(R.id.topBorder)

        if (isInitial) {
            // This trick needed to avoid initial animation when restoring fragment
            titleTextView.isInvisible = true
            titleTextView.postDelayed({
                titleTextView.translationY = topBorder.y - titleTextView.y
                titleTextView.isInvisible = false
            }, 250)
            return
        }

        ObjectAnimator.ofFloat(titleTextView, View.TRANSLATION_Y, topBorder.y - titleTextView.y).apply {
            duration = animDuration
            interpolator = DecelerateInterpolator()
            start()
        }
    }

    private fun animateTabWidth(tabView: TabLayout.TabView, toWidth: Int, animDuration: Long, lastTabPosition: Int? = null) {
        ValueAnimator.ofInt(tabView.width, toWidth).apply {
            addUpdateListener { valueAnimator ->
                val params = tabView.layoutParams
                params.width = valueAnimator.animatedValue as Int
                tabView.layoutParams = params
                lastTabPosition?.let { position ->
                    //fixes expanding animation of last item
                    tabLayout.setScrollPosition(position, 0f, false)
                }
            }
            interpolator = DecelerateInterpolator()
            duration = animDuration
            start()
        }
    }

    private fun animateTabColor(view: View, fromColor: Int, toColor: Int, duration: Long) {
        val colorAnimation = ValueAnimator.ofObject(ArgbEvaluator(), fromColor, toColor)
        colorAnimation.duration = duration
        colorAnimation.addUpdateListener { animator -> view.backgroundTintList = ColorStateList.valueOf(animator.animatedValue as Int) }
        colorAnimation.start()
    }

    override fun onTabReselected(tab: TabLayout.Tab) {
        tabLayout.selectTab(null)
        listener.onSelect(null)
    }

    private fun TabLayout.Tab.setDescription(@StringRes descriptionResId: Int): TabLayout.Tab {
        view.findViewById<TextView>(R.id.description).setText(descriptionResId)
        return this
    }

    interface Listener {
        fun onSelect(marketCategory: MarketCategory?)
    }

}
