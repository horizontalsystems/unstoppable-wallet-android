package io.horizontalsystems.bankwallet.modules.swap.settings.uniswap

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
import io.horizontalsystems.bankwallet.databinding.FragmentSwapSettingsUniswapBinding
import io.horizontalsystems.bankwallet.modules.address.HSAddressInput
import io.horizontalsystems.bankwallet.modules.swap.settings.RecipientAddressViewModel
import io.horizontalsystems.bankwallet.modules.swap.settings.SwapDeadlineViewModel
import io.horizontalsystems.bankwallet.modules.swap.settings.SwapSettingsBaseFragment
import io.horizontalsystems.bankwallet.modules.swap.settings.SwapSlippageViewModel
import io.horizontalsystems.bankwallet.modules.swap.settings.uniswap.UniswapSettingsViewModel.ActionState
import io.horizontalsystems.bankwallet.modules.swap.uniswap.UniswapModule
import io.horizontalsystems.bankwallet.modules.swap.uniswap.UniswapViewModel
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.Header
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper

class UniswapSettingsFragment : SwapSettingsBaseFragment() {
    private val uniswapViewModel by navGraphViewModels<UniswapViewModel>(R.id.swapFragment) {
        UniswapModule.Factory(dex)
    }

    private val vmFactory by lazy {
        UniswapSettingsModule.Factory(uniswapViewModel.tradeService)
    }
    private val uniswapSettingsViewModel by viewModels<UniswapSettingsViewModel> { vmFactory }
    private val deadlineViewModel by viewModels<SwapDeadlineViewModel> { vmFactory }
    private val recipientAddressViewModel by viewModels<RecipientAddressViewModel> { vmFactory }
    private val slippageViewModel by viewModels<SwapSlippageViewModel> { vmFactory }

    private var _binding: FragmentSwapSettingsUniswapBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSwapSettingsUniswapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        uniswapSettingsViewModel.actionStateLiveData.observe(viewLifecycleOwner) { actionState ->
            when (actionState) {
                is ActionState.Enabled -> {
                    setButton(getString(R.string.SwapSettings_Apply), true)
                }
                is ActionState.Disabled -> {
                    setButton(actionState.title, false)
                }
            }
        }

        binding.slippageInputView.setViewModel(slippageViewModel, viewLifecycleOwner)
        binding.deadlineInputView.setViewModel(deadlineViewModel, viewLifecycleOwner)

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
                        if (uniswapSettingsViewModel.onDoneClick()) {
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
