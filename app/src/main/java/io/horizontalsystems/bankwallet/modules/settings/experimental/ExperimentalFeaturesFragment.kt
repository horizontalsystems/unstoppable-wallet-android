package io.horizontalsystems.bankwallet.modules.settings.experimental

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.views.SettingsView

class ExperimentalFeaturesFragment : BaseFragment() {

    private val presenter by viewModels<ExperimentalFeaturesPresenter> { ExperimentalFeaturesModule.Factory() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_experimental_features, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<Toolbar>(R.id.toolbar).setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        val router = presenter.router as ExperimentalFeaturesRouter

        router.showBitcoinHodlingLiveEvent.observe(this, Observer {
            findNavController().navigate(R.id.experimentalFeaturesFragment_to_bitcoinHodlingFragment, null, navOptions())
        })

        view.findViewById<SettingsView>(R.id.bitcoinHodling).setOnSingleClickListener {
            presenter.didTapBitcoinHodling()
        }
    }
}
