package io.horizontalsystems.bankwallet.modules.addErc20token

import android.os.Bundle
import android.os.Handler
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.Observer
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseActivity
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.android.synthetic.main.activity_add_erc20_token.*

class AddErc20TokenActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_erc20_token)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val model: AddErc20TokenViewModel by viewModels { AddErc20TokenModule.Factory() }

        btnPaste.setOnClickListener {
            val text = TextHelper.getCopiedText()
            txtAddressInput.setText(text)
        }

        btnDeleteAddress.setOnClickListener {
            txtAddressInput.setText("")
        }

        btnAddToken.setOnClickListener {
            model.onAddClick()
        }

        txtAddressInput.doOnTextChanged { text, _, _, _ ->
            model.onTextChange(text)
        }

        observeViewModel(model)
    }

    private fun observeViewModel(model: AddErc20TokenViewModel) {
        model.showTrashButton.observe(this, Observer { visible ->
            btnDeleteAddress.isVisible = visible
        })

        model.showPasteButton.observe(this, Observer { visible ->
            btnPaste.isVisible = visible
        })

        model.showProgressbar.observe(this, Observer { visible ->
            progressLoading.isVisible = visible
        })

        model.showAddButton.observe(this, Observer { visible ->
            btnAddToken.isVisible = visible
        })

        model.showExistingCoinWarning.observe(this, Observer { visible ->
            warningText.isVisible = visible
        })

        model.showInvalidAddressError.observe(this, Observer { visible ->
            txtAddressError.isVisible = visible
        })

        model.coinLiveData.observe(this, Observer { viewItem ->
            coinNameLayout.isVisible = viewItem != null
            symbolLayout.isVisible = viewItem != null
            decimalsLayout.isVisible = viewItem != null

            viewItem?.let {
                coinNameValue.text = it.coinName
                symbolValue.text = it.symbol
                decimalsValue.text = it.decimal.toString()
            }
        })

        model.showSuccess.observe(this, Observer {
            HudHelper.showSuccessMessage(findViewById(android.R.id.content), R.string.Hud_Text_Success, HudHelper.SnackbarDuration.LONG)
            Handler().postDelayed({
                finish()
            }, 1500)
        })

    }

}
