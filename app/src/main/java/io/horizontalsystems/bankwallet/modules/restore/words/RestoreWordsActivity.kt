package io.horizontalsystems.bankwallet.modules.restore.words

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.GridLayoutManager
import io.horizontalsystems.bankwallet.BaseActivity
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.putParcelableExtra
import io.horizontalsystems.bankwallet.core.utils.ModuleCode
import io.horizontalsystems.bankwallet.core.utils.Utils
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.SyncMode
import io.horizontalsystems.bankwallet.lib.InputTextViewHolder
import io.horizontalsystems.bankwallet.lib.WordsInputAdapter
import io.horizontalsystems.bankwallet.modules.syncmodule.SyncModeModule
import io.horizontalsystems.bankwallet.ui.extensions.TopMenuItem
import io.horizontalsystems.bankwallet.viewHelpers.HudHelper
import kotlinx.android.synthetic.main.activity_restore_words.*

class RestoreWordsActivity : BaseActivity() {

    private lateinit var viewModel: RestoreWordsViewModel

    private val words = MutableList(12) { "" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_restore_words)

        shadowlessToolbar.bind(
                title = getString(R.string.Restore_Title),
                leftBtnItem = TopMenuItem(R.drawable.back) { onBackPressed() },
                rightBtnItem = TopMenuItem(R.drawable.checkmark_orange) {
                    viewModel.delegate.restoreDidClick(words)
                }
        )

        viewModel = ViewModelProviders.of(this).get(RestoreWordsViewModel::class.java)
        viewModel.init()

        viewModel.errorLiveData.observe(this, Observer { errorId ->
            errorId?.let {
                HudHelper.showErrorMessage(it)
            }
        })

        viewModel.startSyncModeModule.observe(this, Observer {
            SyncModeModule.startForResult(this, ModuleCode.SYNC_MODE)
        })

        recyclerInputs.isNestedScrollingEnabled = false
        recyclerInputs.layoutManager = GridLayoutManager(this, 2)
        recyclerInputs.adapter = WordsInputAdapter(object : InputTextViewHolder.WordsChangedListener {
            override fun set(position: Int, value: String) {
                if (isUsingNativeKeyboard()) {
                    words[position] = value.toLowerCase()
                }
            }

            override fun done() {
                viewModel.delegate.restoreDidClick(words)
            }
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == ModuleCode.SYNC_MODE && data != null && resultCode == RESULT_OK) {
            val syncMode = data.getParcelableExtra<SyncMode>("syncMode")

            val intent = Intent().apply {
                putExtra("accountType", AccountType.Mnemonic(words, AccountType.Derivation.bip44, ""))
                putParcelableExtra("syncMode", syncMode)
            }

            setResult(RESULT_OK, intent)
            finish()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun isUsingNativeKeyboard(): Boolean {
        if (Utils.isUsingCustomKeyboard(this)) {
            showCustomKeyboardAlert()
            return false
        }

        return true
    }
}
