package io.horizontalsystems.bankwallet.modules.walletconnect.request.sendtransaction

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.navigation.navGraphViewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.AppLogger
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.modules.evmfee.EvmFeeCellViewModel
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.databinding.FragmentWalletConnectRequestBinding
import io.horizontalsystems.bankwallet.modules.sendevmtransaction.SendEvmTransactionViewModel
import io.horizontalsystems.bankwallet.modules.evmfee.SendEvmFeeSettingsFragment
import io.horizontalsystems.bankwallet.modules.walletconnect.WalletConnectViewModel
import io.horizontalsystems.bankwallet.modules.walletconnect.request.WalletConnectRequestModule
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryDefault
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper

class WalletConnectSendEthereumTransactionRequestFragment : BaseFragment() {
    private val logger = AppLogger("wallet-connect")
    private val baseViewModel by navGraphViewModels<WalletConnectViewModel>(R.id.walletConnectMainFragment)
    val vmFactory by lazy {
        WalletConnectRequestModule.Factory(
            baseViewModel.sharedSendEthereumTransactionRequest!!, baseViewModel.service
        )
    }
    private val viewModel by viewModels<WalletConnectSendEthereumTransactionRequestViewModel> { vmFactory }
    private val sendEvmTransactionViewModel by viewModels<SendEvmTransactionViewModel> { vmFactory }
    private val feeViewModel by navGraphViewModels<EvmFeeCellViewModel>(R.id.walletConnectSendEthereumTransactionRequestFragment) { vmFactory }
    private var approveEnabled = true
    private var rejectEnabled = true

    private var _binding: FragmentWalletConnectRequestBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWalletConnectRequestBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            viewModel.reject()
            close()
        }

        sendEvmTransactionViewModel.sendEnabledLiveData.observe(viewLifecycleOwner) { enabled ->
            approveEnabled = enabled
            setButtons()
        }

        sendEvmTransactionViewModel.sendingLiveData.observe(viewLifecycleOwner) {
            rejectEnabled = false
            setButtons()
        }

        sendEvmTransactionViewModel.sendSuccessLiveData.observe(viewLifecycleOwner) { transactionHash ->
            viewModel.approve(transactionHash)
            HudHelper.showSuccessMessage(
                requireActivity().findViewById(android.R.id.content),
                R.string.Hud_Text_Done
            )
            close()
        }

        sendEvmTransactionViewModel.sendFailedLiveData.observe(viewLifecycleOwner) {
            HudHelper.showErrorMessage(requireActivity().findViewById(android.R.id.content), it)
        }

        binding.sendEvmTransactionView.init(
            sendEvmTransactionViewModel,
            feeViewModel,
            viewLifecycleOwner,
            onClickEditFee = {
                findNavController().slideFromRight(
                    resId = R.id.sendEvmFeeSettingsFragment,
                    args = SendEvmFeeSettingsFragment.prepareParams(R.id.walletConnectSendEthereumTransactionRequestFragment)
                )
            }
        )

        sendEvmTransactionViewModel.transactionTitleLiveData.observe(viewLifecycleOwner) {
            binding.toolbar.title = it
        }

        setButtons()
    }

    private fun setButtons() {
        binding.buttonsCompose.setContent {
            ComposeAppTheme {
                Column(modifier = Modifier.width(IntrinsicSize.Max)) {
                    ButtonPrimaryYellow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                        title = getString(R.string.Button_Confirm),
                        onClick = {
                            logger.info("click confirm button")
                            sendEvmTransactionViewModel.send(logger)
                        },
                        enabled = approveEnabled
                    )
                    ButtonPrimaryDefault(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
                        title = getString(R.string.Button_Reject),
                        onClick = {
                            viewModel.reject()
                            close()
                        },
                        enabled = rejectEnabled
                    )
                }
            }
        }
    }

    private fun close() {
        baseViewModel.sharedSendEthereumTransactionRequest = null
        findNavController().popBackStack()
    }

}
