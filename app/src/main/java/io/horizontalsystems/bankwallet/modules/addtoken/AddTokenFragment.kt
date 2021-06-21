package io.horizontalsystems.bankwallet.modules.addtoken

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.utils.ModuleField
import io.horizontalsystems.bankwallet.modules.qrscanner.QRScannerActivity
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.snackbar.SnackbarDuration
import io.horizontalsystems.views.ListPosition
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

        val model: AddTokenViewModel by viewModels { AddTokenModule.Factory() }

        addressInputView.setEditable(false)
        addressInputView.setHint("ERC20 / BEP20 / BEP2")

        btnAddToken.isEnabled = false
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

        setCoinDetails(null)
        observeViewModel(model)
    }

    private fun observeViewModel(model: AddTokenViewModel) {
        model.loadingLiveData.observe(viewLifecycleOwner, Observer { visible ->
            addressInputView.setSpinner(visible)
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
            btnAddToken.isEnabled = visible
        })
    }

    private fun setCoinDetails(viewItem: AddTokenModule.ViewItem?) {
        val dots = getString(R.string.AddToken_Dots)
        coinTypeView.bind(getString(R.string.AddToken_CoinType), viewItem?.coinType ?: dots, listPosition = ListPosition.First)
        coinNameView.bind(getString(R.string.AddToken_CoinName), viewItem?.coinName ?: dots, listPosition = ListPosition.Middle)
        coinSymbolView.bind(getString(R.string.AddToken_Symbol), viewItem?.symbol ?: dots, listPosition = ListPosition.Middle)
        coinDecimalsView.bind(getString(R.string.AddToken_Decimals), viewItem?.decimal?.toString() ?: dots, listPosition = ListPosition.Last)
    }

}
