package io.horizontalsystems.bankwallet.modules.addtoken

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.utils.ModuleField
import io.horizontalsystems.bankwallet.modules.qrscanner.QRScannerActivity
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.extensions.AddressInputView
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.snackbar.SnackbarDuration
import io.horizontalsystems.views.ListPosition
import kotlinx.android.synthetic.main.fragment_add_token.*

class AddTokenFragment : BaseFragment() {

    private val viewModel: AddTokenViewModel by viewModels { AddTokenModule.Factory() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_add_token, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        buttonAddTokenCompose.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
        )

        setAddressInputView()
        setCoinDetails(null)
        observeViewModel(viewModel)
        setButton()
    }

    private fun observeViewModel(model: AddTokenViewModel) {
        model.loadingLiveData.observe(viewLifecycleOwner, Observer { visible ->
            addressInputView.setSpinner(visible)
        })

        model.showSuccess.observe(viewLifecycleOwner, Observer {
            HudHelper.showSuccessMessage(requireView(), R.string.Hud_Text_Done, SnackbarDuration.LONG)
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

        model.buttonEnabledLiveData.observe(viewLifecycleOwner, Observer { enabled ->
            setButton(enabled)
        })
    }

    private fun setAddressInputView() {
        addressInputView.setEditable(false)
        addressInputView.setHint("ERC20 / BEP20 / BEP2")

        addressInputView.setListener(object : AddressInputView.Listener {
            override fun onTextChange(text: String) {
                viewModel.onTextChange(text)
            }

            override fun onQrButtonClick() {
                val qrScannerResultLauncher =
                    registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                        if (result.resultCode == Activity.RESULT_OK) {
                            result.data?.getStringExtra(ModuleField.SCAN_ADDRESS)?.let {
                                addressInputView.setText(it)
                            }
                        }
                    }
                val intent = QRScannerActivity.getIntentForFragment(this@AddTokenFragment)
                qrScannerResultLauncher.launch(intent)
            }

            override fun onFocusChange(hasFocus: Boolean) {}
        })
    }

    private fun setButton(enabled: Boolean = false) {
        buttonAddTokenCompose.setContent {
            ComposeAppTheme {
                ButtonPrimaryYellow(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 32.dp),
                    title = getString(R.string.Button_Add),
                    onClick = {
                        viewModel.onAddClick()
                    },
                    enabled = enabled
                )
            }
        }
    }

    private fun setCoinDetails(viewItem: AddTokenModule.ViewItem?) {
        val dots = getString(R.string.AddToken_Dots)
        coinTypeView.bind(getString(R.string.AddToken_CoinTypes), viewItem?.coinType ?: dots, listPosition = ListPosition.First)
        coinNameView.bind(getString(R.string.AddToken_CoinName), viewItem?.coinName ?: dots, listPosition = ListPosition.Middle)
        coinSymbolView.bind(getString(R.string.AddToken_Symbol), viewItem?.symbol ?: dots, listPosition = ListPosition.Middle)
        coinDecimalsView.bind(getString(R.string.AddToken_Decimals), viewItem?.decimals?.toString() ?: dots, listPosition = ListPosition.Last)
    }

}
