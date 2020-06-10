package io.horizontalsystems.bankwallet.modules.guides

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.guideview.GuideModule
import kotlinx.android.synthetic.main.fragment_guides.*


class GuidesFragment : Fragment(), GuidesAdapter.Listener {

    private val viewModel by viewModels<GuidesViewModel> { GuidesModule.Factory() }
    private val adapter = GuidesAdapter(this)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_guides, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerGuides.adapter = adapter

        observeLiveData()
    }

    override fun onItemClick(position: Int) {
        viewModel.onGuideClick(position)
    }

    private fun observeLiveData() {
        viewModel.viewItemsLiveData.observe(viewLifecycleOwner, Observer {
            adapter.items = it
            adapter.notifyDataSetChanged()
        })

        viewModel.openGuide.observe(viewLifecycleOwner, Observer { guide ->
            context?.let {
                GuideModule.start(it, guide)
            }

        })
    }

}
