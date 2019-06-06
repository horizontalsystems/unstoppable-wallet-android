package io.horizontalsystems.bankwallet.modules.syncmodule

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import io.horizontalsystems.bankwallet.BaseActivity
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.SyncMode
import io.horizontalsystems.bankwallet.modules.pin.PinModule
import io.horizontalsystems.bankwallet.ui.dialogs.BottomConfirmAlert
import io.horizontalsystems.bankwallet.ui.extensions.TopMenuItem
import io.horizontalsystems.bankwallet.viewHelpers.HudHelper
import kotlinx.android.synthetic.main.activity_about_settings.shadowlessToolbar
import kotlinx.android.synthetic.main.activity_sync_mode.*

class SyncModeActivity : BaseActivity(), BottomConfirmAlert.Listener {

    private lateinit var viewModel: SyncModeViewModel
    private var wordList: List<String> = listOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sync_mode)

        shadowlessToolbar.bind(
                title = getString(R.string.CoinOption_Title),
                leftBtnItem = TopMenuItem(R.drawable.back) { onBackPressed() },
                rightBtnItem = TopMenuItem(R.drawable.checkmark_orange) { viewModel.delegate.onNextClick() }
        )

        intent?.getStringExtra(keyWords)?.let { wordList = it.split(", ") }

        viewModel = ViewModelProviders.of(this).get(SyncModeViewModel::class.java)
        viewModel.init()

        viewModel.navigateToSetPinLiveEvent.observe(this, Observer {
            PinModule.startForSetPin(this)
        })

        viewModel.syncModeUpdatedLiveEvent.observe(this, Observer { syncMode ->
            syncMode?.let {
                fastCheckmarkIcon.visibility = if (it == SyncMode.FAST) View.VISIBLE else View.GONE
                slowCheckmarkIcon.visibility = if (it == SyncMode.FAST) View.GONE else View.VISIBLE
            }
        })

        viewModel.showConfirmationDialogLiveEvent.observe(this, Observer {
            val confirmationList = mutableListOf(
                    R.string.Backup_Confirmation_SecretKey,
                    R.string.Backup_Confirmation_DeleteAppWarn,
                    R.string.Backup_Confirmation_LockAppWarn,
                    R.string.Backup_Confirmation_Disclaimer
            )
            BottomConfirmAlert.show(this, confirmationList, this)
        })

        viewModel.keyStoreSafeExecute.observe(this, Observer { triple ->
            triple?.let {
                val (action, onSuccess, onFailure) = it
                safeExecuteWithKeystore(action, onSuccess, onFailure)
            }
        })

        viewModel.errorLiveData.observe(this, Observer { errorId ->
            errorId?.let { HudHelper.showErrorMessage(it) }
        })

        fastSync.setOnClickListener { viewModel.delegate.onFastSyncModeSelect() }
        slowSync.setOnClickListener { viewModel.delegate.onSlowSyncModeSelect() }
    }

    override fun onConfirmationSuccess() {
        viewModel.delegate.didConfirm(wordList)
    }

    companion object {
        private const val keyWords = "key_words"

        fun start(context: Context, words: List<String>) {
            val intent = Intent(context, SyncModeActivity::class.java)
            intent.putExtra(keyWords, words.joinToString())
            context.startActivity(intent)
        }
    }

}
