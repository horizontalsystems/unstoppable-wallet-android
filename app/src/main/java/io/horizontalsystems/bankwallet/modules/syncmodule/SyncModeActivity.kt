package io.horizontalsystems.bankwallet.modules.syncmodule

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import io.horizontalsystems.bankwallet.BaseActivity
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.putParcelableExtra
import io.horizontalsystems.bankwallet.entities.SyncMode
import io.horizontalsystems.bankwallet.ui.extensions.TopMenuItem
import kotlinx.android.synthetic.main.activity_about_settings.shadowlessToolbar
import kotlinx.android.synthetic.main.activity_sync_mode.*

class SyncModeActivity : BaseActivity() {

    private lateinit var viewModel: SyncModeViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sync_mode)

        shadowlessToolbar.bind(
                title = getString(R.string.CoinOption_Title),
                leftBtnItem = TopMenuItem(R.drawable.back, onClick = { onBackPressed() }),
                rightBtnItem = TopMenuItem(R.drawable.checkmark_orange, onClick = { viewModel.delegate.didConfirm() })
        )

        viewModel = ViewModelProviders.of(this).get(SyncModeViewModel::class.java)
        viewModel.init()

        viewModel.notifySyncModeSelected.observe(this, Observer { syncMode: SyncMode? ->
            syncMode?.let {
                val intent = Intent().apply {
                    putParcelableExtra("syncMode", it)
                }
                setResult(RESULT_OK, intent)
                finish()
            }
        })

        viewModel.syncModeUpdatedLiveEvent.observe(this, Observer { syncMode ->
            syncMode?.let {
                fastCheckmarkIcon.visibility = if (it == SyncMode.FAST) View.VISIBLE else View.GONE
                slowCheckmarkIcon.visibility = if (it == SyncMode.FAST) View.GONE else View.VISIBLE
            }
        })

        fastSync.setOnClickListener { viewModel.delegate.onSyncModeSelect(isFast = true) }
        slowSync.setOnClickListener { viewModel.delegate.onSyncModeSelect(isFast = false) }
    }
}
