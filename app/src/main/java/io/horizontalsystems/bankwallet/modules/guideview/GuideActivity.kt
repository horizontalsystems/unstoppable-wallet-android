package io.horizontalsystems.bankwallet.modules.guideview

import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseActivity
import io.horizontalsystems.bankwallet.modules.guides.LoadStatus
import kotlinx.android.synthetic.main.activity_guide.*


class GuideActivity : BaseActivity(), GuideContentAdapter.Listener {

    private val contentAdapter = GuideContentAdapter(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_guide)
        appBarLayout.outlineProvider = null

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = ""

        rvBlocks.adapter = contentAdapter

        val guideUrl = intent.extras?.getString(GuideModule.GuideUrlKey)
        val viewModel by viewModels<GuideViewModel> { GuideModule.Factory(guideUrl) }

        viewModel.blocks.observe(this, Observer {
            contentAdapter.submitList(it)
        })

        viewModel.statusLiveData.observe(this, Observer {
            error.isVisible = it is LoadStatus.Failed
        })
    }

    override fun onGuideClick(url: String) {
        GuideModule.start(this, url)
    }
}
