package io.horizontalsystems.bankwallet.modules.addtoken

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.utils.ModuleField
import io.horizontalsystems.bankwallet.modules.qrscanner.QRScannerActivity
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper
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

        addressInputView.setEditable(false)
        addressInputView.setHint(getString(model.hintTextRes))

        btnAddToken.setOnClickListener {
            model.onAddClick()
        }

        addressInputView.onTextChange {
            model.onTextChange(it)
        }

        addressInputView.onPasteText {
            model.onTextChange(it)
            addressInputView.setText(it)
        }

        val qrScannerResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.getStringExtra(ModuleField.SCAN_ADDRESS)?.let {
                    model.onTextChange(it)
                    addressInputView.setText(it)
                }
            }
        }

        addressInputView.onButtonQrScanClick {
            val intent = QRScannerActivity.getIntentForFragment(this)
            qrScannerResultLauncher.launch(intent)
        }

        observeViewModel(model)
    }

    private fun observeViewModel(model: AddTokenViewModel) {
        model.loadingLiveData.observe(viewLifecycleOwner, Observer { visible ->
            progressLoading.isVisible = visible
        })

        model.showSuccess.observe(viewLifecycleOwner, Observer {
            HudHelper.showSuccessMessage(requireView(), R.string.Hud_Text_Success, SnackbarDuration.LONG)
            Handler(Looper.getMainLooper()).postDelayed({
                findNavController().popBackStack()
            }, 1500)
        })

        model.viewItemLiveData.observe(viewLifecycleOwner, Observer {
            setCoinDetails(it)
        })

        model.cautionLiveData.observe(viewLifecycleOwner, Observer { caution ->
            addressInputView.setError(caution)
        })

        model.showAddButton.observe(viewLifecycleOwner, Observer { visible ->
            btnAddToken.isVisible = visible
        })
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
