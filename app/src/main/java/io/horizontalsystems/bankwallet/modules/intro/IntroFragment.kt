package io.horizontalsystems.bankwallet.modules.intro

import android.os.Bundle
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat.getDrawable
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager.widget.ViewPager
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.modules.welcome.WelcomeModule
import kotlinx.android.synthetic.main.fragment_intro.*

class IntroFragment : BaseFragment() {

    private val presenter by lazy { ViewModelProvider(this, IntroModule.Factory()).get(IntroPresenter::class.java) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_intro, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewPagerAdapter = IntroViewPagerAdapter(childFragmentManager)
        val pagesCount = viewPagerAdapter.count

        viewPager.adapter = viewPagerAdapter

        try {
            // set custom mScroller to viewPager via Reflection to make viewPager swipe smoothly for next and back buttons
            val mScroller = ViewPager::class.java.getDeclaredField("mScroller")
            mScroller.isAccessible = true
            mScroller.set(viewPager, ViewPagerScroller(viewPager.context))
        } catch (e: Exception) {
        }

        val images = arrayOf(R.drawable.ic_onboarding_logo, R.drawable.ic_independence, R.drawable.ic_knowledge, R.drawable.ic_privacy)
        viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            private var skipButtonVisible = true

            override fun onPageScrollStateChanged(state: Int) = Unit

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

            override fun onPageSelected(position: Int) {
                imageSwitcher.setImageResource(images[position])
                if (position == 0) {
                    imageSwitcher.background = null
                } else {
                    imageSwitcher.background = getDrawable(requireContext(), R.drawable.ic_ellipse)
                }
                if (position == pagesCount - 1) {
                    skipButtonVisible = false
                } else if (!skipButtonVisible) {
                    skipButtonVisible = true
                }
                showSkipButton(skipButtonVisible)
            }
        })

        ViewCompat.setTransitionName(imageSwitcher, "intro_wallet_logo")

        imageSwitcher.setFactory { ImageView(activity?.applicationContext) }
        imageSwitcher.setImageResource(images[0])

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
            router.navigateToWelcomeLiveEvent.observe(this, {
                activity?.let { activity -> WelcomeModule.start(activity, imageSwitcher) }
            })
        }
    }

    override fun canHandleOnBackPress(): Boolean {
        return if (viewPager.currentItem == 0) {
            false
        } else {
            viewPager.currentItem = viewPager.currentItem - 1
            true
        }
    }

    private fun showSkipButton(show: Boolean) {
        TransitionManager.beginDelayedTransition(introLayout)
        btnSkip.isVisible = show
    }

    companion object {
        fun instance(): IntroFragment {
            return IntroFragment()
        }
    }
}
