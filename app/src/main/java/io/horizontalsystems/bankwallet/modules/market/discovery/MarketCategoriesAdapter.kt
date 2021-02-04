package io.horizontalsystems.bankwallet.modules.market.discovery

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.StringRes
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
        listener.onSelect(category)

        val index = marketCategories.indexOf(category)
        tabLayout.apply {
            selectTab(getTabAt(index))
        }
    }

    override fun onTabSelected(tab: TabLayout.Tab) {
        listener.onSelect(marketCategories.getOrNull(tab.position))

        //hide icon
        tab.view.findViewById<ImageView>(android.R.id.icon).apply {
            ObjectAnimator.ofFloat(this, ImageView.ALPHA, 1f, 0f).apply {
                duration = 200
                start()
            }
        }

        //translate title to top
        val topBorderYPosition = tab.view.findViewById<View>(R.id.topBorder).y
        tab.view.findViewById<TextView>(android.R.id.text1).let { titleTextView ->
            ObjectAnimator.ofFloat(titleTextView, TextView.TRANSLATION_Y, topBorderYPosition - titleTextView.y).apply {
                duration = 100
                start()
            }
        }

        //expand layout
        tab.view.let { containerView ->
            ValueAnimator.ofInt(containerView.width, itemViewMaxLength).apply {
                addUpdateListener { valueAnimator ->
                    val params = containerView.layoutParams
                    params.width = valueAnimator.animatedValue as Int
                    containerView.layoutParams = params
                }
                duration = 100
                start()
            }
        }

        //show description
        val descriptionTextView = tab.view.findViewById<TextView>(R.id.description)
        descriptionTextView.alpha = 0f
        descriptionTextView.visibility = View.VISIBLE

        ValueAnimator.ofFloat(0f, 1f).apply {
            addUpdateListener { valueAnimator ->
                descriptionTextView.alpha = valueAnimator.animatedValue as Float
            }
            duration = 500
            start()
        }
    }

    override fun onTabUnselected(tab: TabLayout.Tab) {
        //hide description
        tab.view.findViewById<TextView>(R.id.description).visibility = View.GONE

        //show icon
        tab.view.findViewById<ImageView>(android.R.id.icon).apply {
            ObjectAnimator.ofFloat(this, ImageView.ALPHA, 0f, 1f).apply {
                duration = 200
                start()
            }
        }

        //translate title to bottom
        val titleTextView = tab.view.findViewById<TextView>(android.R.id.text1)
        val topBorderYPosition = tab.view.findViewById<View>(R.id.topBorder).y
        ObjectAnimator.ofFloat(titleTextView, TextView.TRANSLATION_Y, topBorderYPosition - titleTextView.y).apply {
            duration = 100
            start()
        }

        //shrink layout
        val containerView = tab.view as ViewGroup
        containerView.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        )

        tab.view.isActivated = false
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
