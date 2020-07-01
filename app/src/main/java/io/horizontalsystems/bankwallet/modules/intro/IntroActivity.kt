package io.horizontalsystems.bankwallet.modules.intro

import android.app.ActivityOptions
import android.os.Bundle
import android.transition.Fade
import android.view.View
import android.view.Window
import android.view.animation.AccelerateInterpolator
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.Animation.AnimationListener
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager.widget.ViewPager
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseActivity
import io.horizontalsystems.bankwallet.modules.welcome.WelcomeModule
import kotlinx.android.synthetic.main.activity_intro.*

class IntroActivity : BaseActivity() {

    private val presenter by lazy { ViewModelProvider(this, IntroModule.Factory()).get(IntroPresenter::class.java) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        with(window) {
            requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS)

            val fade = Fade()
            fade.duration = 700

            exitTransition = fade
        }

        setTransparentStatusBar()
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

        val images = arrayOf(R.drawable.ic_independence, R.drawable.ic_knowledge, R.drawable.ic_privacy)
        viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            private var skipButtonVisible = true

            override fun onPageScrollStateChanged(state: Int) = Unit

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

            override fun onPageSelected(position: Int) {
                imageSwitcher.setImageResource(images[position])
                if (position == pagesCount - 1) {
                    skipButtonVisible = false
                    fadeOutAnimation(btnSkip)
                } else if (!skipButtonVisible) {
                    skipButtonVisible = true
                    fadeInAnimation(btnSkip)
                }
            }
        })

        imageSwitcher.setFactory { ImageView(applicationContext) }
        imageSwitcher.setImageResource(R.drawable.ic_independence)

        circleIndicator.setViewPager(viewPager)

        btnNext.setOnClickListener {
            if (viewPager.currentItem < pagesCount - 1) {
                viewPager.currentItem = viewPager.currentItem + 1
            } else {
                presenter.start()
            }
        }

        btnSkip.setOnClickListener {
            presenter.skip()
        }

        (presenter.router as? IntroRouter)?.let { router ->
            router.navigateToWelcomeLiveEvent.observe(this, Observer {
                WelcomeModule.start(this, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
            })
        }
    }

    private fun fadeOutAnimation(view: View) {
        val fadeOut: Animation = AlphaAnimation(1.0f, 0.0f)
        fadeOut.interpolator = AccelerateInterpolator()
        fadeOut.duration = 300

        fadeOut.setAnimationListener(object : AnimationListener {
            override fun onAnimationStart(animation: Animation?) {}
            override fun onAnimationEnd(animation: Animation?) {
                view.isInvisible = true
            }
            override fun onAnimationRepeat(animation: Animation?) {}
        })

        view.startAnimation(fadeOut)
    }

    private fun fadeInAnimation(view: View) {
        val fadeIn: Animation = AlphaAnimation(0.0f, 1.0f)
        fadeIn.interpolator = DecelerateInterpolator()
        fadeIn.duration = 300

        fadeIn.setAnimationListener(object : AnimationListener {
            override fun onAnimationStart(animation: Animation) {}
            override fun onAnimationEnd(animation: Animation) {
                view.isVisible = true
            }
            override fun onAnimationRepeat(animation: Animation) {}
        })

        view.startAnimation(fadeIn)
    }

    override fun onBackPressed() {
        if (viewPager.currentItem == 0) {
            super.onBackPressed()
        } else {
            viewPager.currentItem = viewPager.currentItem - 1
        }
    }
}
