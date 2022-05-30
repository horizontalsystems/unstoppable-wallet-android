package io.horizontalsystems.bankwallet.modules.swap.settings.oneinch

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.navigation.navGraphViewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.databinding.FragmentSwapSettings1inchBinding
import io.horizontalsystems.bankwallet.modules.address.HSAddressInput
import io.horizontalsystems.bankwallet.modules.swap.oneinch.OneInchModule
import io.horizontalsystems.bankwallet.modules.swap.oneinch.OneInchSwapViewModel
import io.horizontalsystems.bankwallet.modules.swap.settings.RecipientAddressViewModel
import io.horizontalsystems.bankwallet.modules.swap.settings.SwapSettingsBaseFragment
import io.horizontalsystems.bankwallet.modules.swap.settings.SwapSlippageViewModel
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.Header
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper

class OneInchSettingsFragment : SwapSettingsBaseFragment() {

    private val oneInchViewModel by navGraphViewModels<OneInchSwapViewModel>(R.id.swapFragment) {
        OneInchModule.Factory(dex)
    }

    private val vmFactory by lazy {
        OneInchSwapSettingsModule.Factory(oneInchViewModel.tradeService)
    }
    private val oneInchSettingsViewModel by viewModels<OneInchSettingsViewModel> { vmFactory }
    private val recipientAddressViewModel by viewModels<RecipientAddressViewModel> { vmFactory }
    private val slippageViewModel by viewModels<SwapSlippageViewModel> { vmFactory }

    private var _binding: FragmentSwapSettings1inchBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSwapSettings1inchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        oneInchSettingsViewModel.actionStateLiveData.observe(viewLifecycleOwner) { actionState ->
            when (actionState) {
                is OneInchSettingsViewModel.ActionState.Enabled -> {
                    setButton(getString(R.string.SwapSettings_Apply), true)
                }
                is OneInchSettingsViewModel.ActionState.Disabled -> {
                    setButton(actionState.title, false)
                }
            }
        }

        binding.slippageInputView.setViewModel(slippageViewModel, viewLifecycleOwner)

        binding.buttonApplyCompose.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
        )

        binding.recipientAddressCompose.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
        )

        setRecipientAddressCompose()
    }

    private fun setRecipientAddressCompose() {
        binding.recipientAddressCompose.setContent {
            ComposeAppTheme {
                App.marketKit.platformCoin(dex.blockchain.baseCoinType)?.let { platformCoin ->
                    Column {
                        Spacer(modifier = Modifier.height(12.dp))
                        Header {
                            Text(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                text = stringResource(R.string.SwapSettings_RecipientAddressTitle),
                                style = ComposeAppTheme.typography.subhead1,
                                color = ComposeAppTheme.colors.grey
                            )
                        }

                        HSAddressInput(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            initial = recipientAddressViewModel.initialAddress,
                            coinType = platformCoin.coinType,
                            coinCode = platformCoin.coin.code,
                            onStateChange = {
                                recipientAddressViewModel.setAddressWithError(it?.dataOrNull, it?.errorOrNull)
                            }
                        )

                        Text(
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                            text = stringResource(R.string.SwapSettings_RecipientAddressDescription),
                            style = ComposeAppTheme.typography.subhead2,
                            color = ComposeAppTheme.colors.grey
                        )
                    }
                }
            }
        }
    }

    private fun setButton(title: String, enabled: Boolean = false) {
        binding.buttonApplyCompose.setContent {
            ComposeAppTheme {
                ButtonPrimaryYellow(
                    modifier = Modifier.padding(
                        top = 28.dp,
                        bottom = 24.dp
                    ),
                    title = title,
                    onClick = {
                        if (oneInchSettingsViewModel.onDoneClick()) {
                            findNavController().popBackStack()
                        } else {
                            HudHelper.showErrorMessage(
                                this.requireView(),
                                getString(R.string.default_error_msg)
                            )
                        }
                    },
                    enabled = enabled
                )
            }
        }
    }

}
