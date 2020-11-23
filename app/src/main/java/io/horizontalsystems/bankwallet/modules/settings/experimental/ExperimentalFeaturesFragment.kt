package io.horizontalsystems.bankwallet.modules.settings.experimental

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.core.findNavController
import kotlinx.android.synthetic.main.fragment_experimental_features.*

class ExperimentalFeaturesFragment : BaseFragment() {

    private val presenter by viewModels<ExperimentalFeaturesPresenter> { ExperimentalFeaturesModule.Factory() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_experimental_features, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        val router = presenter.router as ExperimentalFeaturesRouter

        router.showBitcoinHodlingLiveEvent.observe(this, Observer {
            findNavController().navigate(R.id.experimentalFeaturesFragment_to_bitcoinHodlingFragment, null, navOptions())
        })

        bitcoinHodling.setOnSingleClickListener {
            presenter.didTapBitcoinHodling()
        }
    }
}
