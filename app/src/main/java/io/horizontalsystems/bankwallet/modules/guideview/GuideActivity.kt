package io.horizontalsystems.bankwallet.modules.guideview

import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseActivity
import io.horizontalsystems.bankwallet.entities.Guide
import io.noties.markwon.Markwon
import kotlinx.android.synthetic.main.activity_guide.*
import org.apache.commons.io.IOUtils
import java.io.StringWriter


class GuideActivity : BaseActivity() {

    private lateinit var markwon: Markwon
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

        markwon = buildMarkwon()

        viewModel.guideLiveData.observe(this, Observer {
            showContent(it)
        })

        rvBlocks.adapter = contentAdapter
    }

    private fun showContent(guide: Guide) {
        val writer = StringWriter()
        IOUtils.copy(assets.open("guides/${guide.fileName}.md"), writer, Charsets.UTF_8)

        val document = markwon.parse(writer.toString())
        val guideVisitor = GuideVisitor(markwon)
        document.accept(guideVisitor)

        contentAdapter.submitList(guideVisitor.blocks)
    }

    private fun buildMarkwon(): Markwon {
        return Markwon.builder(this).build()
    }
}
