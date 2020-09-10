package io.horizontalsystems.bankwallet.modules.settings.experimental

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.bankwallet.modules.settings.experimental.bitcoinhodling.BitcoinHodlingModule
import kotlinx.android.synthetic.main.fragment_experimental_features.*

class ExperimentalFeaturesFragment : BaseFragment() {

    private val presenter by viewModels<ExperimentalFeaturesPresenter> { ExperimentalFeaturesModule.Factory() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_experimental_features, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (activity as? AppCompatActivity)?.let {
            it.setSupportActionBar(toolbar)
            it.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        }

        val router = presenter.router as ExperimentalFeaturesRouter

        router.showBitcoinHodlingLiveEvent.observe(this, Observer {
            activity?.let { BitcoinHodlingModule.start(it) }
        })

        bitcoinHodling.setOnSingleClickListener {
            presenter.didTapBitcoinHodling()
        }
    }
}
