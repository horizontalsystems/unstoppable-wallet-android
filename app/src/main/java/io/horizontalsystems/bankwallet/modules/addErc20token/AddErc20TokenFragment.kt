package io.horizontalsystems.bankwallet.modules.addErc20token

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
import io.horizontalsystems.bankwallet.core.providers.Erc20ContractInfoProvider
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.snackbar.SnackbarDuration
import kotlinx.android.synthetic.main.fragment_add_erc20_token.*

class AddErc20TokenFragment : BaseFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_add_erc20_token, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setNavigationToolbar(toolbar, findNavController())

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

        model.resultLiveData.observe(viewLifecycleOwner, Observer { result ->
            when (result) {
                AddErc20TokenModule.State.Empty -> {
                    progressLoading.isVisible = false
                    warningText.isVisible = false
                    btnAddToken.isVisible = false
                    txtAddressError.isVisible = false

                    setCoinDetails(null)
                }
                AddErc20TokenModule.State.Loading -> {
                    progressLoading.isVisible = true
                }
                is AddErc20TokenModule.State.ExistingCoin -> {
                    progressLoading.isVisible = false
                    txtAddressError.isVisible = false
                    warningText.isVisible = true

                    setCoinDetails(result.viewItem)
                }
                is AddErc20TokenModule.State.Success -> {
                    progressLoading.isVisible = false
                    txtAddressError.isVisible = false
                    btnAddToken.isVisible = true

                    setCoinDetails(result.viewItem)
                }
                is AddErc20TokenModule.State.Failed -> {
                    txtAddressError.text = getString(getErrorText(result.error))
                    progressLoading.isVisible = false
                    txtAddressError.isVisible = true
                    btnAddToken.isVisible = false

                    setCoinDetails(null)
                }
            }
        })

    }

    private fun getErrorText(error: Throwable): Int {
        return when (error) {
            is Erc20ContractInfoProvider.ApiError.ContractDoesNotExist,
            is AddErc20TokenModule.InvalidAddress -> R.string.AddErc20Token_InvalidAddressError
            is Erc20ContractInfoProvider.ApiError.ApiLimitExceeded -> R.string.AddErc20Token_ApiLimitExceeded
            else -> R.string.Error
        }
    }

    private fun setCoinDetails(viewItem: AddErc20TokenModule.ViewItem?) {
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
}
