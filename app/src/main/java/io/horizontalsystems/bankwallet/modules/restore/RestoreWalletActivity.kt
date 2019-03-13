package io.horizontalsystems.bankwallet.modules.restore

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v7.widget.GridLayoutManager
import io.horizontalsystems.bankwallet.BaseActivity
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.Utils
import io.horizontalsystems.bankwallet.lib.EditTextViewHolder
import io.horizontalsystems.bankwallet.lib.WordsInputAdapter
import io.horizontalsystems.bankwallet.modules.pin.PinModule
import io.horizontalsystems.bankwallet.ui.dialogs.BottomConfirmAlert
import io.horizontalsystems.bankwallet.ui.extensions.TopMenuItem
import io.horizontalsystems.bankwallet.viewHelpers.HudHelper
import kotlinx.android.synthetic.main.activity_restore_wallet.*

class RestoreWalletActivity : BaseActivity(), BottomConfirmAlert.Listener {

    private lateinit var viewModel: RestoreViewModel

    private val words = MutableList(12) { "" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_restore_wallet)

        shadowlessToolbar.bind(
                title = getString(R.string.Restore_Title),
                leftBtnItem = TopMenuItem(R.drawable.back, { onBackPressed() }),
                rightBtnItem = TopMenuItem(R.drawable.checkmark_orange, { viewModel.delegate.restoreDidClick(words) })
        )

        viewModel = ViewModelProviders.of(this).get(RestoreViewModel::class.java)
        viewModel.init()

        viewModel.errorLiveData.observe(this, Observer { errorId ->
            errorId?.let {
                HudHelper.showErrorMessage(it)
            }
        })

        viewModel.navigateToSetPinLiveEvent.observe(this, Observer {
            PinModule.startForSetPin(this)
        })

        viewModel.keyStoreSafeExecute.observe(this, Observer { triple ->
            triple?.let {
                val (action, onSuccess, onFailure) = it
                safeExecuteWithKeystore(action, onSuccess, onFailure)
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

        recyclerInputs.layoutManager = GridLayoutManager(this, 2)
        recyclerInputs.adapter = WordsInputAdapter(object : EditTextViewHolder.WordsChangedListener {
            override fun set(position: Int, value: String) {
                if (isUsingNativeKeyboard()) {
                    words[position] = value
                }
            }
        })
    }

    private fun isUsingNativeKeyboard(): Boolean {
        if (Utils.isUsingCustomKeyboard(this)) {
            showCustomKeyboardAlert()
            return false
        }
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onConfirmationSuccess() {
        viewModel.delegate.didConfirm(words)
    }
}
