package io.horizontalsystems.bankwallet.modules.guideview

import android.os.Bundle
import androidx.core.content.ContextCompat
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseActivity
import io.horizontalsystems.core.CoreApp
import kotlinx.android.synthetic.main.activity_guide_viewer.*


class GuideViewerActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_guide_viewer)
        appBarLayout.outlineProvider = null
        setTransparentStatusBar()

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = ""

        guideWebView.setBackgroundColor(ContextCompat.getColor(this, if (CoreApp.themeStorage.isLightModeOn) R.color.white else R.color.dark))
        guideWebView.loadUrl("https://stackoverflow.com/questions/3407256/height-of-status-bar-in-android")

    }

}
