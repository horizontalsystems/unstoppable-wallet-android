package io.horizontalsystems.bankwallet.modules.addErc20token

import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.snackbar.SnackbarDuration
import kotlinx.android.synthetic.main.fragment_add_erc20_token.*

class AddErc20TokenFragment : BaseFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_add_erc20_token, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (activity as? AppCompatActivity)?.let {
            it.setSupportActionBar(toolbar)
            it.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        }

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
            coinNameTitle.isVisible = viewItem != null
            coinNameValue.isVisible = viewItem != null

            symbolTitle.isVisible = viewItem != null
            symbolValue.isVisible = viewItem != null

            decimalTitle.isVisible = viewItem != null
            decimalsValue.isVisible = viewItem != null

            viewItem?.let {
                coinNameValue.text = it.coinName
                symbolValue.text = it.symbol
                decimalsValue.text = it.decimal.toString()
            }
        })

        model.showSuccess.observe(this, Observer {
            HudHelper.showSuccessMessage(requireView(), R.string.Hud_Text_Success, SnackbarDuration.LONG)
            Handler().postDelayed({
                findNavController().popBackStack()
            }, 1500)
        })

    }
}
