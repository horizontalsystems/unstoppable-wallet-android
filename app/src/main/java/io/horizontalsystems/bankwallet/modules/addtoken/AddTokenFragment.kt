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
import io.horizontalsystems.bankwallet.databinding.FragmentAddTokenBinding
import io.horizontalsystems.bankwallet.modules.qrscanner.QRScannerActivity
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.extensions.AddressInputView
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.snackbar.SnackbarDuration
import io.horizontalsystems.views.ListPosition

class AddTokenFragment : BaseFragment() {

    private val viewModel: AddTokenViewModel by viewModels { AddTokenModule.Factory() }

    private var _binding: FragmentAddTokenBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddTokenBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        binding.buttonAddTokenCompose.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
        )

        setAddressInputView()
        setCoinDetails(null)
        observeViewModel(viewModel)
        setButton()
    }

    private fun observeViewModel(model: AddTokenViewModel) {
        model.loadingLiveData.observe(viewLifecycleOwner, Observer { visible ->
            binding.addressInputView.setSpinner(visible)
        })

        model.showSuccess.observe(viewLifecycleOwner, Observer {
            HudHelper.showSuccessMessage(
                requireView(),
                R.string.Hud_Text_Done,
                SnackbarDuration.LONG
            )
            Handler(Looper.getMainLooper()).postDelayed({
                findNavController().popBackStack()
            }, 1500)
        })

        model.viewItemLiveData.observe(viewLifecycleOwner, Observer {
            setCoinDetails(it)
        })

        model.cautionLiveData.observe(viewLifecycleOwner, Observer { caution ->
            binding.addressInputView.setError(caution)
        })

        model.buttonEnabledLiveData.observe(viewLifecycleOwner, Observer { enabled ->
            setButton(enabled)
        })
    }

    private fun setAddressInputView() {
        binding.addressInputView.setEditable(false)
        binding.addressInputView.setHint("ERC20 / BEP20 / BEP2")

        val qrScannerResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    result.data?.getStringExtra(ModuleField.SCAN_ADDRESS)?.let {
                        binding.addressInputView.setText(it)
                    }
                }
            }

        binding.addressInputView.setListener(object : AddressInputView.Listener {
            override fun onTextChange(text: String) {
                viewModel.onTextChange(text)
            }

            override fun onQrButtonClick() {
                val intent = QRScannerActivity.getIntentForFragment(this@AddTokenFragment)
                qrScannerResultLauncher.launch(intent)
            }

            override fun onFocusChange(hasFocus: Boolean) {}
        })
    }

    private fun setButton(enabled: Boolean = false) {
        binding.buttonAddTokenCompose.setContent {
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
        binding.coinTypeView.bind(
            getString(R.string.AddToken_CoinTypes),
            viewItem?.coinType ?: dots,
            listPosition = ListPosition.First
        )
        binding.coinNameView.bind(
            getString(R.string.AddToken_CoinName),
            viewItem?.coinName ?: dots,
            listPosition = ListPosition.Middle
        )
        binding.coinSymbolView.bind(
            getString(R.string.AddToken_Symbol),
            viewItem?.symbol ?: dots,
            listPosition = ListPosition.Middle
        )
        binding.coinDecimalsView.bind(
            getString(R.string.AddToken_Decimals),
            viewItem?.decimals?.toString() ?: dots,
            listPosition = ListPosition.Last
        )
    }

}
