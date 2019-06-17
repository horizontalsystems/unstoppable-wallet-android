package io.horizontalsystems.bankwallet.modules.restore

import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.GridLayoutManager
import io.horizontalsystems.bankwallet.BaseActivity
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.utils.Utils
import io.horizontalsystems.bankwallet.lib.InputTextViewHolder
import io.horizontalsystems.bankwallet.lib.WordsInputAdapter
import io.horizontalsystems.bankwallet.modules.syncmodule.SyncModeModule
import io.horizontalsystems.bankwallet.ui.extensions.TopMenuItem
import io.horizontalsystems.bankwallet.viewHelpers.HudHelper
import io.horizontalsystems.hdwalletkit.WordList
import kotlinx.android.synthetic.main.activity_restore_wallet.*

class RestoreWalletActivity : BaseActivity() {

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

        viewModel.navigateToSetSyncModeLiveEvent.observe(this, Observer { words ->
            words?.let {
                SyncModeModule.start(this, it)
            }
        })

        val autocompleteAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, WordList.getWords())

        recyclerInputs.isNestedScrollingEnabled = false
        recyclerInputs.layoutManager = GridLayoutManager(this, 2)
        recyclerInputs.adapter = WordsInputAdapter(object : InputTextViewHolder.WordsChangedListener {
            override fun set(position: Int, value: String) {
                if (isUsingNativeKeyboard()) {
                    words[position] = value
                }
            }

            override fun done() {
                viewModel.delegate.restoreDidClick(words)
            }

        }, autocompleteAdapter)
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

}
