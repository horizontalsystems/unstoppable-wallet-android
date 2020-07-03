package io.horizontalsystems.bankwallet.modules.guideview

import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseActivity
import io.horizontalsystems.bankwallet.entities.Guide
import io.horizontalsystems.bankwallet.modules.guides.LoadStatus
import kotlinx.android.synthetic.main.activity_guide.*


class GuideActivity : BaseActivity() {

    private val contentAdapter = GuideContentAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_guide)
        appBarLayout.outlineProvider = null

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = ""

        rvBlocks.adapter = contentAdapter

        val guide = intent.extras?.getParcelable<Guide>(GuideModule.GuideKey)
        val viewModel by viewModels<GuideViewModel> { GuideModule.Factory(guide) }

        viewModel.blocks.observe(this, Observer {
            contentAdapter.submitList(it)
        })

        viewModel.statusLiveData.observe(this, Observer {
            error.isVisible = it is LoadStatus.Failed
            error.text = (it as? LoadStatus.Failed)?.e?.message
        })
    }
}
