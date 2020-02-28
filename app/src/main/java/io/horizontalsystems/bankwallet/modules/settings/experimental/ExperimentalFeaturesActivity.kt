package io.horizontalsystems.bankwallet.modules.settings.experimental

import android.os.Bundle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.modules.base.BaseActivity
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.settings.experimental.bitcoinhodling.BitcoinHodlingModule
import kotlinx.android.synthetic.main.activity_app_status.toolbar
import kotlinx.android.synthetic.main.activity_experimental_features.*

class ExperimentalFeaturesActivity : BaseActivity() {
    private lateinit var presenter: ExperimentalFeaturesPresenter
    private lateinit var router: ExperimentalFeaturesRouter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_experimental_features)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        presenter = ViewModelProvider(this, ExperimentalFeaturesModule.Factory()).get(ExperimentalFeaturesPresenter::class.java)
        router = presenter.router as ExperimentalFeaturesRouter

        router.showBitcoinHodlingLiveEvent.observe(this, Observer {
            BitcoinHodlingModule.start(this)
        })

        bitcoinHodling.setOnClickListener {
            presenter.didTapBitcoinHodling()
        }
    }

}
