package io.horizontalsystems.bankwallet.modules.guideview

import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseActivity
import io.horizontalsystems.bankwallet.entities.Guide
import kotlinx.android.synthetic.main.activity_guide.*
import org.commonmark.parser.Parser
import java.io.InputStreamReader


class GuideActivity : BaseActivity() {

    private val contentAdapter = GuideContentAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_guide)
        appBarLayout.outlineProvider = null
        setTransparentStatusBar()

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = ""

        val guide = intent.extras?.getParcelable<Guide>(GuideModule.GuideKey)
        val viewModel by viewModels<GuideViewModel> { GuideModule.Factory(guide) }

        viewModel.guideLiveData.observe(this, Observer {
            showContent(it)
        })

        rvBlocks.adapter = contentAdapter
    }

    private fun showContent(guide: Guide) {
        val fileStream = assets.open("guides/${guide.fileName}.md")

        val parser = Parser.builder().build()
        val document = parser.parseReader(InputStreamReader(fileStream, Charsets.UTF_8))

        val guideVisitor = GuideVisitorBlock()
        document.accept(guideVisitor)

        contentAdapter.submitList(guideVisitor.blocks)
    }
}
