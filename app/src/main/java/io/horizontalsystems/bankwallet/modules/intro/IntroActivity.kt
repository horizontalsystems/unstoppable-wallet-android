package io.horizontalsystems.bankwallet.modules.intro

import android.os.Bundle
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

        setContentView(R.layout.activity_intro)

        val viewPagerAdapter = IntroViewPagerAdapter(supportFragmentManager)
        val pagesCount = viewPagerAdapter.count

        viewPager.adapter = viewPagerAdapter

        val images = arrayOf(R.drawable.ic_independence, R.drawable.ic_knowledge, R.drawable.ic_privacy)
        viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) = Unit

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                root.progress = (position + positionOffset) / (pagesCount - 1)
            }

            override fun onPageSelected(position: Int) {
                imageSwitcher.setImageResource(images[position])
            }
        })

        circleIndicator.setViewPager(viewPager)

        btnNext.setOnClickListener {
            if (viewPager.currentItem < pagesCount - 1) {
                viewPager.currentItem = viewPager.currentItem + 1
            }
        }

        btnSkip.setOnClickListener {
            presenter.skip()
        }

        btnStart.setOnClickListener {
            presenter.start()
        }

        (presenter.router as? IntroRouter)?.let { router ->
            router.navigateToWelcomeLiveEvent.observe(this, Observer {
                WelcomeModule.start(this)
            })
        }
    }
}
