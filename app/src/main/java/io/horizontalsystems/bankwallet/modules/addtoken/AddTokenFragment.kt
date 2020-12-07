package io.horizontalsystems.bankwallet.modules.addtoken

import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.entities.ApiError
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.ethereumkit.core.AddressValidator
import io.horizontalsystems.snackbar.SnackbarDuration
import kotlinx.android.synthetic.main.fragment_add_token.*

class AddTokenFragment : BaseFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_add_token, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        val tokenType = arguments?.getParcelable<TokenType>(TOKEN_TYPE_KEY) ?: run {
            findNavController().popBackStack()
            return
        }

        val model: AddTokenViewModel by viewModels { AddTokenModule.Factory(tokenType) }

        toolbar.title = getString(model.titleTextRes)
        txtAddressInput.hint = getString(model.hintTextRes)

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

    private fun observeViewModel(model: AddTokenViewModel) {
        model.loadingLiveData.observe(viewLifecycleOwner, Observer { visible ->
            progressLoading.isVisible = visible
        })

        model.showTrashButton.observe(viewLifecycleOwner, Observer { visible ->
            btnDeleteAddress.isVisible = visible
        })

        model.showPasteButton.observe(viewLifecycleOwner, Observer { visible ->
            btnPaste.isVisible = visible
        })

        model.showSuccess.observe(viewLifecycleOwner, Observer {
            HudHelper.showSuccessMessage(requireView(), R.string.Hud_Text_Success, SnackbarDuration.LONG)
            Handler().postDelayed({
                findNavController().popBackStack()
            }, 1500)
        })

        model.viewItemLiveData.observe(viewLifecycleOwner, Observer {
            setCoinDetails(it)
        })

        model.showErrorLiveData.observe(viewLifecycleOwner, Observer { error ->
            error?.let {
                txtAddressError.text = getString(getErrorText(it))
            }
            txtAddressError.isVisible = error != null
        })

        model.showWarningLiveData.observe(viewLifecycleOwner, Observer { visible ->
            warningText.isVisible = visible
        })

        model.showAddButton.observe(viewLifecycleOwner, Observer { visible ->
            btnAddToken.isVisible = visible
        })

    }

    private fun getErrorText(error: Throwable): Int {
        return when (error) {
            is AddressValidator.InvalidAddressLength,
            is AddressValidator.InvalidAddressHex,
            is AddressValidator.InvalidAddressChecksum -> R.string.AddToken_InvalidAddressError
            is ApiError.ContractNotFound -> R.string.AddErc20Token_ContractNotFound
            is ApiError.TokenNotFound -> R.string.AddBep2Token_TokenNotFound
            is ApiError.ApiLimitExceeded -> R.string.AddToken_ApiLimitExceeded
            else -> R.string.Error
        }
    }

    private fun setCoinDetails(viewItem: AddTokenModule.ViewItem?) {
        coinNameTitle.isVisible = viewItem != null
        coinNameValue.isVisible = viewItem != null

        symbolTitle.isVisible = viewItem != null
        symbolValue.isVisible = viewItem != null

        decimalTitle.isVisible = viewItem != null
        decimalsValue.isVisible = viewItem != null

        coinNameValue.text = viewItem?.coinName ?: ""
        symbolValue.text = viewItem?.symbol ?: ""
        decimalsValue.text = viewItem?.decimal?.toString() ?: ""
    }

    companion object {
        const val TOKEN_TYPE_KEY = "token_type_key"
    }
}
