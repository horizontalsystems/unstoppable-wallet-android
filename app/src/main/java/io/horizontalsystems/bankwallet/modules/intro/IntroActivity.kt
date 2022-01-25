package io.horizontalsystems.bankwallet.modules.intro

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
import android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.viewpager.widget.ViewPager
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseActivity
import io.horizontalsystems.bankwallet.databinding.ActivityIntroBinding
import io.horizontalsystems.bankwallet.modules.main.MainModule
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow

class IntroActivity : BaseActivity() {

    val viewModel by viewModels<IntroViewModel> { IntroModule.Factory() }
    private lateinit var binding: ActivityIntroBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityIntroBinding.inflate(layoutInflater)
        val view = binding.root
        setStatusBarTransparent()
        setContentView(view)

        val viewPagerAdapter = IntroViewPagerAdapter(supportFragmentManager)

        binding.viewPager.adapter = viewPagerAdapter

        try {
            // set custom mScroller to viewPager via Reflection to make viewPager swipe smoothly for next and back buttons
            val mScroller = ViewPager::class.java.getDeclaredField("mScroller")
            mScroller.isAccessible = true
            mScroller.set(binding.viewPager, ViewPagerScroller(binding.viewPager.context))
        } catch (e: Exception) {
        }

        val textes = listOf(
            Pair(R.string.Intro_Wallet_Screen2Title, R.string.Intro_Wallet_Screen2Description),
            Pair(R.string.Intro_Wallet_Screen3Title, R.string.Intro_Wallet_Screen3Description),
            Pair(R.string.Intro_Wallet_Screen4Title, R.string.Intro_Wallet_Screen4Description),
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
                binding.title.setText(getString(textes[position].first))
                binding.description.setText(getString(textes[position].second))
            }
        })

        binding.title.setText(getString(textes[0].first))
        binding.description.setText(getString(textes[0].second))

        binding.circleIndicator.setViewPager(binding.viewPager)

        binding.buttonNextCompose.setContent {
            ComposeAppTheme {
                ButtonPrimaryYellow(
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp),
                    title = stringResource(R.string.Intro_Wallet_Screen1Description),
                    onClick = {
                        viewModel.onStartClicked()
                        MainModule.start(this)
                        finish()
                    }
                )
            }
        }
    }

    private fun setStatusBarTransparent() {
        if (Build.VERSION.SDK_INT in 26..29) {
            window.statusBarColor = Color.TRANSPARENT
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.decorView.systemUiVisibility =
                SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or SYSTEM_UI_FLAG_LAYOUT_STABLE

        } else if (Build.VERSION.SDK_INT >= 30) {
            window.statusBarColor = Color.TRANSPARENT
            // Making status bar overlaps with the activity
            WindowCompat.setDecorFitsSystemWindows(window, false)
        }

        /*
         *  Making the Navigation system bar not overlapping with the activity
         */
        if (Build.VERSION.SDK_INT >= 30) {

            // Root ViewGroup of activity
            ViewCompat.setOnApplyWindowInsetsListener(binding.introLayout) { view, windowInsets ->

                val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

                // Apply the insets as a margin to the view. Here the system is setting
                // only the bottom, left, and right dimensions, but apply whichever insets are
                // appropriate to your layout. You can also update the view padding
                // if that's more appropriate.

                view.layoutParams = (view.layoutParams as FrameLayout.LayoutParams).apply {
                    leftMargin = insets.left
                    bottomMargin = insets.bottom
                    rightMargin = insets.right
                }

                // Return CONSUMED if you don't want want the window insets to keep being
                // passed down to descendant views.
                WindowInsetsCompat.CONSUMED
            }

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
