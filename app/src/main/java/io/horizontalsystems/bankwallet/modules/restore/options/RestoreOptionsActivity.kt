package io.horizontalsystems.bankwallet.modules.restore.options

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import io.horizontalsystems.bankwallet.BaseActivity
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.putParcelableExtra
import io.horizontalsystems.bankwallet.core.utils.ModuleField
import io.horizontalsystems.bankwallet.entities.AccountType.Derivation
import io.horizontalsystems.bankwallet.entities.SyncMode
import io.horizontalsystems.bankwallet.ui.extensions.TopMenuItem
import kotlinx.android.synthetic.main.activity_about_settings.shadowlessToolbar
import kotlinx.android.synthetic.main.activity_restore_options.*

class RestoreOptionsActivity : BaseActivity() {

    private lateinit var viewModel: RestoreOptionsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_restore_options)

        shadowlessToolbar.bind(
                title = getString(R.string.CoinOption_Title),
                leftBtnItem = TopMenuItem(R.drawable.back, onClick = { onBackPressed() }),
                rightBtnItem = TopMenuItem(R.drawable.checkmark_orange, onClick = { viewModel.delegate.onDone() })
        )

        viewModel = ViewModelProviders.of(this).get(RestoreOptionsViewModel::class.java)
        viewModel.init()

        viewModel.notifyOptionsLiveEvent.observe(this, Observer { (syncMode, derivation) ->
            setResult(RESULT_OK, Intent().apply {
                putParcelableExtra(ModuleField.DERIVATION, derivation)
                putParcelableExtra(ModuleField.SYNCMODE, syncMode)
            })

            finish()
        })

        viewModel.syncModeLiveEvent.observe(this, Observer {
            fastSync.checked = it == SyncMode.FAST
            slowSync.checked = it == SyncMode.SLOW
        })

        viewModel.derivationLiveEvent.observe(this, Observer {
            bip44.checked = it == Derivation.bip44
            bip49.checked = it == Derivation.bip49
        })

        fastSync.setOnClickListener { viewModel.delegate.onSelect(SyncMode.FAST) }
        slowSync.setOnClickListener { viewModel.delegate.onSelect(SyncMode.SLOW) }

        bip44.setOnClickListener { viewModel.delegate.onSelect(Derivation.bip44) }
        bip49.setOnClickListener { viewModel.delegate.onSelect(Derivation.bip49) }
    }
}
