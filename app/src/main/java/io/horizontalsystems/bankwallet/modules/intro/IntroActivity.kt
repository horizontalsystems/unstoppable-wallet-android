package io.horizontalsystems.bankwallet.modules.intro

import android.os.Bundle
import android.transition.Fade
import android.view.Window
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseActivity

class IntroActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        with(window) {
            requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS)

            val fade = Fade()
            fade.duration = 700

            exitTransition = fade
        }

        setContentView(R.layout.activity_intro)
    }

    override fun onBackPressed() {
        supportFragmentManager.primaryNavigationFragment?.childFragmentManager?.fragments?.lastOrNull()?.let { fragment ->
            if ((fragment as? IntroFragment)?.canHandleOnBackPress() == true) {
                return
            }
        }

        super.onBackPressed()
    }

}
