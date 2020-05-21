package io.horizontalsystems.bankwallet.modules.restore.eos

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.zxing.integration.android.IntentIntegrator
import io.horizontalsystems.bankwallet.core.BaseActivity
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.utils.ModuleField
import io.horizontalsystems.bankwallet.core.utils.Utils
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.modules.qrscanner.QRScannerActivity
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.views.MultipleInputEditTextView
import kotlinx.android.synthetic.main.activity_restore_eos.*
import java.util.*

class RestoreEosActivity : BaseActivity(), MultipleInputEditTextView.Listener {

    private lateinit var viewModel: RestoreEosViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_restore_eos)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        viewModel = ViewModelProvider(this).get(RestoreEosViewModel::class.java)
        viewModel.init()

        eosAccount.btnText = getString(R.string.Send_Button_Paste)
        eosActivePrivateKey.btnText = getString(R.string.Send_Button_Paste)

        viewModel.setAccount.observe(this, Observer {
            eosAccount.text = it
        })

        viewModel.setPrivateKey.observe(this, Observer {
            eosActivePrivateKey.text = it
        })

        viewModel.startQRScanner.observe(this, Observer {
            QRScannerActivity.start(this)
        })

        viewModel.finishLiveEvent.observe(this, Observer { pair ->
            setResult(RESULT_OK, Intent().apply {
                putExtra(ModuleField.ACCOUNT_TYPE, AccountType.Eos(pair.first, pair.second))
            })

            finish()
        })

        viewModel.errorLiveEvent.observe(this, Observer { resId ->
            HudHelper.showErrorMessage(findViewById(android.R.id.content), resId)
        })

        eosAccount.setListenerForTextInput(this)
        eosActivePrivateKey.setListenerForTextInput(this)

        bindActions()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.restore_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.menuRestore ->  {
                viewModel.delegate.onClickDone(
                        eosAccount.text.trim().toLowerCase(Locale.ENGLISH),
                        eosActivePrivateKey.text.trim()
                )
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == IntentIntegrator.REQUEST_CODE && resultCode == Activity.RESULT_OK){
            data?.getStringExtra(ModuleField.SCAN_ADDRESS)?.let {
                viewModel.delegate.onQRCodeScan(it)
            }
        }
    }

    override fun beforeTextChanged() {
        if (Utils.isUsingCustomKeyboard(this)) {
            showCustomKeyboardAlert()
        }
    }

    private fun bindActions() {
        eosAccount.bind(onPaste = { viewModel.delegate.onPasteAccount() })
        eosActivePrivateKey.bind(
                onPaste = { viewModel.delegate.onPasteKey() },
                onScan = { viewModel.delegate.onClickScan() }
        )
    }
}
