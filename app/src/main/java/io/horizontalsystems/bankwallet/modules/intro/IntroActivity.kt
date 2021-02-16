package io.horizontalsystems.bankwallet.modules.intro

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.viewpager.widget.ViewPager
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseActivity
import io.horizontalsystems.bankwallet.modules.main.MainModule
import kotlinx.android.synthetic.main.activity_intro.*

class IntroActivity : BaseActivity() {

    val viewModel by viewModels<IntroViewModel> { IntroModule.Factory() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_intro)

        val viewPagerAdapter = IntroViewPagerAdapter(supportFragmentManager)
        val pagesCount = viewPagerAdapter.count

        viewPager.adapter = viewPagerAdapter

        try {
            // set custom mScroller to viewPager via Reflection to make viewPager swipe smoothly for next and back buttons
            val mScroller = ViewPager::class.java.getDeclaredField("mScroller")
            mScroller.isAccessible = true
            mScroller.set(viewPager, ViewPagerScroller(viewPager.context))
        } catch (e: Exception) {
        }

        val images = arrayOf(R.drawable.ic_onboarding_logo, R.drawable.ic_independence, R.drawable.ic_privacy, R.drawable.ic_knowledge)
        viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) = Unit

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

            override fun onPageSelected(position: Int) {
                imageSwitcher.setImageResource(images[position])
                if (position == 0) {
                    imageSwitcher.background = null
                } else {
                    imageSwitcher.background = ContextCompat.getDrawable(this@IntroActivity, R.drawable.ic_ellipse)
                }
                updateButton(position == pagesCount - 1)
            }
        })

        imageSwitcher.setFactory { ImageView(applicationContext) }
        imageSwitcher.setImageResource(images[0])

        circleIndicator.setViewPager(viewPager)

        btnNext.setOnClickListener {
            if (viewPager.currentItem < pagesCount - 1) {
                viewPager.currentItem = viewPager.currentItem + 1
            } else {
                viewModel.onClickStart()
            }
        }

        viewModel.openMainLiveEvent.observe(this, {
            MainModule.start(this)
            finish()
        })
    }

    override fun onBackPressed() {
        if (canHandleOnBackPress()) {
            return
        }

        super.onBackPressed()
    }

    private fun updateButton(lastSlide: Boolean) {
        btnNext.setBackgroundResource(if (lastSlide) R.drawable.button_yellow_background else R.drawable.button_steel_background)
        btnNext.setText(if (lastSlide) R.string.Button_Start else R.string.Button_Next)
    }

    private fun canHandleOnBackPress(): Boolean {
        return if (viewPager.currentItem == 0) {
            false
        } else {
            viewPager.currentItem = viewPager.currentItem - 1
            true
        }
    }

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, IntroActivity::class.java)
            context.startActivity(intent)
        }
    }

}
