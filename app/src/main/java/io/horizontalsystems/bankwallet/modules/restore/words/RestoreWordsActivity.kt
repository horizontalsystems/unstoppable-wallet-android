package io.horizontalsystems.bankwallet.modules.restore.words

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.BaseActivity
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.utils.ModuleField
import io.horizontalsystems.bankwallet.core.utils.Utils
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.viewHelpers.HudHelper
import kotlinx.android.synthetic.main.activity_restore_words.*
import java.util.*

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

        viewModel = ViewModelProvider(this).get(RestoreWordsViewModel::class.java)
        viewModel.init(wordsCount)

        viewModel.errorLiveData.observe(this, Observer {
            HudHelper.showErrorMessage(it)
        })

        viewModel.notifyRestored.observe(this, Observer {
            setResult(RESULT_OK, Intent().apply {
                putExtra(ModuleField.ACCOUNT_TYPE, AccountType.Mnemonic(viewModel.delegate.words, salt = null))
            })
            finish()
        })

        wordsInput.addTextChangedListener(object: TextWatcher {
            override fun afterTextChanged(s: Editable?) {}

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                isUsingNativeKeyboard()
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        //fixes scrolling in EditText when it's inside NestedScrollView
        wordsInput.setOnTouchListener { v, event ->
            if (wordsInput.hasFocus()) {
                v.parent.requestDisallowInterceptTouchEvent(true)
                when (event.action and MotionEvent.ACTION_MASK) {
                    MotionEvent.ACTION_SCROLL -> {
                        v.parent.requestDisallowInterceptTouchEvent(false)
                        return@setOnTouchListener true
                    }
                }
            }
            return@setOnTouchListener false
        }

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.restore_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when(item?.itemId){
            R.id.menuRestore ->  {
                val cleanedString = wordsInput.text?.toString()?.trim()?.toLowerCase(Locale.ENGLISH)?.replace(Regex("(\\s)+"), " ")
                viewModel.delegate.onDone(cleanedString)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
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
