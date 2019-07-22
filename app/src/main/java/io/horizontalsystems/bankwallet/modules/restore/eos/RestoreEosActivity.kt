package io.horizontalsystems.bankwallet.modules.restore.eos

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import io.horizontalsystems.bankwallet.BaseActivity
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.ui.extensions.TopMenuItem
import kotlinx.android.synthetic.main.activity_restore_eos.*
import kotlinx.android.synthetic.main.activity_restore_words.shadowlessToolbar

class RestoreEosActivity : BaseActivity() {

    private lateinit var viewModel: RestoreEosViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_restore_eos)

        shadowlessToolbar.bind(
                title = getString(R.string.Restore_Title),
                leftBtnItem = TopMenuItem(R.drawable.back, this::onBackPressed),
                rightBtnItem = TopMenuItem(R.drawable.checkmark_orange, this::onClickRestore))

        viewModel = ViewModelProviders.of(this).get(RestoreEosViewModel::class.java)
        viewModel.init()

        viewModel.finishLiveEvent.observe(this, Observer {
            it?.let { pair ->
                setResult(RESULT_OK, Intent().apply {
                    putExtra("accountType", AccountType.Eos(pair.first, pair.second))
                })

                finish()
            }
        })
    }

    private fun onClickRestore() {
        val accountName = eosAccount.getText()
        val privateKey = eosActivePrivateKey.getText()

        viewModel.delegate.onClickDone(accountName.trim(), privateKey.trim())
    }
}
