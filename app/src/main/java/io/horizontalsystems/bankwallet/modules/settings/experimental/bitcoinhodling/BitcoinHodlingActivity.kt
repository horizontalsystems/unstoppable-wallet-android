package io.horizontalsystems.bankwallet.modules.settings.experimental.bitcoinhodling

import android.os.Bundle
import android.widget.CompoundButton
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.BaseActivity
import io.horizontalsystems.bankwallet.R
import kotlinx.android.synthetic.main.activity_app_status.toolbar
import kotlinx.android.synthetic.main.activity_bitcoin_hodling.*

class BitcoinHodlingActivity : BaseActivity() {
    private lateinit var presenter: BitcoinHodlingPresenter
    private lateinit var view: BitcoinHodlingView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_bitcoin_hodling)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        presenter = ViewModelProvider(this, BitcoinHodlingModule.Factory()).get(BitcoinHodlingPresenter::class.java)
        view = presenter.view as BitcoinHodlingView

        view.lockTimeEnabledLiveEvent.observe(this, Observer { enabled ->
            switchLockTime.showSwitch(enabled, CompoundButton.OnCheckedChangeListener { _, isChecked ->
                presenter.onSwitchLockTime(isChecked)
            })
        })

        switchLockTime.setOnClickListener {
            switchLockTime.switchToggle()
        }

        presenter.onLoad()
    }

}
