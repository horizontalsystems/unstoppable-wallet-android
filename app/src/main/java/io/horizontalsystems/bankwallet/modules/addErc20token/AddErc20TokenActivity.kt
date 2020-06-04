package io.horizontalsystems.bankwallet.modules.addErc20token

import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.Observer
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseActivity
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import kotlinx.android.synthetic.main.activity_add_erc20_token.*

class AddErc20TokenActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_erc20_token)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val model: AddErc20TokenViewModel by viewModels()

        btnPaste.setOnClickListener {
            val text =  TextHelper.getCopiedText()
            txtAddressInput.setText(text)
        }

        btnDeleteAddress.setOnClickListener {
            txtAddressInput.setText("")
        }

        txtAddressInput.doOnTextChanged { text, _, _, _ ->
            model.onTextChange(text)
        }

        model.showTrashButtonVisible.observe(this, Observer { visible ->
            btnDeleteAddress.isVisible = visible
        })

        model.showPasteButtonVisible.observe(this, Observer { visible ->
            btnPaste.isVisible = visible
        })

    }

}
