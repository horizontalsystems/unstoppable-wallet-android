package io.horizontalsystems.bankwallet.modules.restore.words

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import io.horizontalsystems.bankwallet.BaseActivity
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.putParcelableExtra
import io.horizontalsystems.bankwallet.core.utils.ModuleCode
import io.horizontalsystems.bankwallet.core.utils.ModuleField
import io.horizontalsystems.bankwallet.core.utils.Utils
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.SyncMode
import io.horizontalsystems.bankwallet.modules.restore.options.RestoreOptionsModule
import io.horizontalsystems.bankwallet.viewHelpers.HudHelper
import kotlinx.android.synthetic.main.activity_restore_words.*

class RestoreWordsActivity : BaseActivity() {

    private lateinit var viewModel: RestoreWordsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_restore_words)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val wordsCount = intent.getIntExtra(ModuleField.WORDS_COUNT, 12)

        val accountTypeTitleRes = intent.getIntExtra(ModuleField.ACCOUNT_TYPE_TITLE, 0)
        if (accountTypeTitleRes > 0) {
            description.text = getString(R.string.Restore_Enter_Key_Description_Mnemonic, getString(accountTypeTitleRes), wordsCount.toString())
        }

        viewModel = ViewModelProviders.of(this).get(RestoreWordsViewModel::class.java)
        viewModel.init(wordsCount)

        viewModel.errorLiveData.observe(this, Observer {
            HudHelper.showErrorMessage(it)
        })

        viewModel.notifyRestored.observe(this, Observer {
            setResult(RESULT_OK, Intent().apply {
                putExtra(ModuleField.ACCOUNT_TYPE, AccountType.Mnemonic(viewModel.delegate.words, AccountType.Derivation.bip44, salt = null))
            })
            finish()
        })

        viewModel.startSyncModeModule.observe(this, Observer {
            RestoreOptionsModule.start(this, ModuleCode.RESTORE_OPTIONS)
        })

        wordsInput.addTextChangedListener(object: TextWatcher {
            override fun afterTextChanged(s: Editable?) {}

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                isUsingNativeKeyboard()
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.restore_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when(item?.itemId){
            R.id.menuOk ->  {
                viewModel.delegate.onDone(wordsInput.text?.toString())
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == ModuleCode.RESTORE_OPTIONS && data != null && resultCode == RESULT_OK) {
            val syncMode = data.getParcelableExtra<SyncMode>(ModuleField.SYNCMODE)
            val derivation = data.getParcelableExtra<AccountType.Derivation>(ModuleField.DERIVATION)

            val intent = Intent().apply {
                putExtra(ModuleField.ACCOUNT_TYPE, AccountType.Mnemonic(viewModel.delegate.words, derivation, salt = null))
                putParcelableExtra(ModuleField.SYNCMODE, syncMode)
            }

            setResult(RESULT_OK, intent)
            finish()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    //  Private

    private fun isUsingNativeKeyboard(): Boolean {
        if (Utils.isUsingCustomKeyboard(this)) {
            showCustomKeyboardAlert()
            return false
        }

        return true
    }
}
