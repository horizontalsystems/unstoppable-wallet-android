package io.horizontalsystems.bankwallet.modules.intro

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.viewpager.widget.ViewPager
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseActivity
import io.horizontalsystems.bankwallet.databinding.ActivityIntroBinding
import io.horizontalsystems.bankwallet.modules.main.MainModule
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryDefault
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow

class IntroActivity : BaseActivity() {

    val viewModel by viewModels<IntroViewModel> { IntroModule.Factory() }
    private lateinit var binding: ActivityIntroBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityIntroBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        val viewPagerAdapter = IntroViewPagerAdapter(supportFragmentManager)
        val pagesCount = viewPagerAdapter.count

        binding.viewPager.adapter = viewPagerAdapter

        try {
            // set custom mScroller to viewPager via Reflection to make viewPager swipe smoothly for next and back buttons
            val mScroller = ViewPager::class.java.getDeclaredField("mScroller")
            mScroller.isAccessible = true
            mScroller.set(binding.viewPager, ViewPagerScroller(binding.viewPager.context))
        } catch (e: Exception) {
        }

        val images = arrayOf(
            R.drawable.ic_onboarding_logo,
            R.drawable.ic_independence,
            R.drawable.ic_knowledge,
            R.drawable.ic_privacy
        )
        binding.viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) = Unit

            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
            }

            override fun onPageSelected(position: Int) {
                binding.imageSwitcher.setImageResource(images[position])
                if (position == 0) {
                    binding.imageSwitcher.background = null
                } else {
                    binding.imageSwitcher.background =
                        ContextCompat.getDrawable(this@IntroActivity, R.drawable.ic_ellipse)
                }
                setNextButton(pagesCount, position == pagesCount - 1)
            }
        })

        binding.imageSwitcher.setFactory { ImageView(applicationContext) }
        binding.imageSwitcher.setImageResource(images[0])

        binding.circleIndicator.setViewPager(binding.viewPager)

        setNextButton(pagesCount)

        viewModel.openMainLiveEvent.observe(this, {
            MainModule.start(this)
            finish()
        })
    }

    private fun setNextButton(pagesCount: Int, lastSlide: Boolean = false) {
        val title = getString(if (lastSlide) R.string.Button_Start else R.string.Button_Next)
        binding.buttonNextCompose.setContent {
            ComposeAppTheme {
                if (lastSlide) {
                    ButtonPrimaryYellow(
                        modifier = Modifier.padding(start = 24.dp, end = 24.dp),
                        title = title,
                        onClick = {
                            onButtonClick(pagesCount)
                        }
                    )
                } else {
                    ButtonPrimaryDefault(
                        modifier = Modifier.padding(start = 24.dp, end = 24.dp),
                        title = title,
                        onClick = {
                            onButtonClick(pagesCount)
                        }
                    )
                }
            }
        }
    }

    private fun onButtonClick(pagesCount: Int) {
        if (binding.viewPager.currentItem < pagesCount - 1) {
            binding.viewPager.currentItem = binding.viewPager.currentItem + 1
        } else {
            viewModel.onClickStart()
        }
    }

    override fun onBackPressed() {
        if (canHandleOnBackPress()) {
            return
        }

        super.onBackPressed()
    }

    private fun canHandleOnBackPress(): Boolean {
        return if (binding.viewPager.currentItem == 0) {
            false
        } else {
            binding.viewPager.currentItem = binding.viewPager.currentItem - 1
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
