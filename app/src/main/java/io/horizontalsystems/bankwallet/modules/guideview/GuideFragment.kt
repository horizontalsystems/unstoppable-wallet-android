package io.horizontalsystems.bankwallet.modules.guideview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.modules.guides.LoadStatus
import kotlinx.android.synthetic.main.fragment_guide.*

class GuideFragment : BaseFragment(), GuideContentAdapter.Listener {

    private val contentAdapter = GuideContentAdapter(this)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_guide, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        appBarLayout.outlineProvider = null

        (activity as? AppCompatActivity)?.let {
            it.setSupportActionBar(toolbar)
            it.supportActionBar?.setDisplayHomeAsUpEnabled(true)
            it.supportActionBar?.title = ""
        }

        rvBlocks.adapter = contentAdapter

        val guideUrl = arguments?.getString(guideUrlKey)
        val viewModel by viewModels<GuideViewModel> { GuideModule.Factory(guideUrl) }

        observe(viewModel)
    }

    private fun observe(viewModel: GuideViewModel) {
        viewModel.blocks.observe(viewLifecycleOwner, Observer {
            contentAdapter.submitList(it)
        })

        viewModel.statusLiveData.observe(viewLifecycleOwner, Observer {
            error.isVisible = it is LoadStatus.Failed
        })
    }

    //  GuideContentAdapter listener

    override fun onGuideClick(url: String) {
        findNavController().navigate(R.id.guideFragment_guideFragment, bundleOf(guideUrlKey to url), navOptions())
    }

    companion object {
        const val guideUrlKey = "urlKey"
    }
}
