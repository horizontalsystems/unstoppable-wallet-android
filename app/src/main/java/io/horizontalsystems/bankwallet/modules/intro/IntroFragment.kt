package io.horizontalsystems.bankwallet.modules.intro

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat.getDrawable
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.viewpager.widget.ViewPager
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.core.findNavController
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

        val images = arrayOf(R.drawable.ic_onboarding_logo, R.drawable.ic_knowledge, R.drawable.ic_independence, R.drawable.ic_privacy)
        viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) = Unit

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

            override fun onPageSelected(position: Int) {
                imageSwitcher.setImageResource(images[position])
                if (position == 0) {
                    imageSwitcher.background = null
                } else {
                    imageSwitcher.background = getDrawable(requireContext(), R.drawable.ic_ellipse)
                }
                updateButton(position == pagesCount - 1)
            }
        })

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

        (presenter.router as? IntroRouter)?.let { router ->
            router.navigateToWelcomeLiveEvent.observe(viewLifecycleOwner, Observer {
                val extras = FragmentNavigatorExtras(imageSwitcher to "welcome_wallet_logo")
                findNavController().navigate(R.id.introFragment_to_welcomeFragment, null, null, extras)
            })
        }
    }

    private fun updateButton(lastSlide: Boolean) {
        btnNext.setBackgroundResource(if (lastSlide) R.drawable.button_yellow_background else R.drawable.button_steel_background)
        btnNext.setText(if (lastSlide) R.string.Button_GetStarted else R.string.Button_Next)
    }

    fun canHandleOnBackPress(): Boolean {
        return if (viewPager.currentItem == 0) {
            false
        } else {
            viewPager.currentItem = viewPager.currentItem - 1
            true
        }
    }

}
