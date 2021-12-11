package io.horizontalsystems.bankwallet.modules.walletconnect.request.sendtransaction

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.navGraphViewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.AppLogger
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.ethereum.EthereumFeeViewModel
import io.horizontalsystems.bankwallet.modules.sendevmtransaction.SendEvmTransactionViewModel
import io.horizontalsystems.bankwallet.modules.walletconnect.WalletConnectViewModel
import io.horizontalsystems.bankwallet.modules.walletconnect.request.WalletConnectRequestModule
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryDefault
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.android.synthetic.main.fragment_wallet_connect_request.*

class WalletConnectSendEthereumTransactionRequestFragment : BaseFragment() {
    private val logger = AppLogger("wallet-connect")
    private val baseViewModel by navGraphViewModels<WalletConnectViewModel>(R.id.walletConnectMainFragment)
    private lateinit var viewModel: WalletConnectSendEthereumTransactionRequestViewModel
    private lateinit var sendViewModel: SendEvmTransactionViewModel
    private lateinit var feeViewModel: EthereumFeeViewModel
    private var approveEnabled = true
    private var rejectEnabled = true


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_wallet_connect_request, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val vmFactory = WalletConnectRequestModule.Factory(baseViewModel.sharedSendEthereumTransactionRequest!!, baseViewModel.service)

        viewModel = ViewModelProvider(this, vmFactory).get(WalletConnectSendEthereumTransactionRequestViewModel::class.java)
        sendViewModel = ViewModelProvider(this, vmFactory).get(SendEvmTransactionViewModel::class.java)
        feeViewModel = ViewModelProvider(this, vmFactory).get(EthereumFeeViewModel::class.java)

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            viewModel.reject()
            close()
        }

        sendViewModel.sendEnabledLiveData.observe(viewLifecycleOwner, { enabled ->
            approveEnabled = enabled
            setButtons()
        })

        sendViewModel.sendingLiveData.observe(viewLifecycleOwner, {
            rejectEnabled = false
            setButtons()
        })

        sendViewModel.sendSuccessLiveData.observe(viewLifecycleOwner, { transactionHash ->
            viewModel.approve(transactionHash)
            HudHelper.showSuccessMessage(
                requireActivity().findViewById(android.R.id.content),
                R.string.Hud_Text_Done
            )
            close()
        })

        sendViewModel.sendFailedLiveData.observe(viewLifecycleOwner, {
            HudHelper.showErrorMessage(requireActivity().findViewById(android.R.id.content), it)
        })

        sendEvmTransactionView.init(
            sendViewModel,
            feeViewModel,
            viewLifecycleOwner,
            parentFragmentManager,
            showSpeedInfoListener = {
                findNavController().navigate(
                    R.id.walletConnectRequestFragment_to_feeSpeedInfo,
                    null,
                    navOptions()
                )
            }
        )

        sendViewModel.transactionTitleLiveData.observe(viewLifecycleOwner, {
            toolbar.title = it
        })

        setButtons()
    }

    private fun setButtons() {
        buttonsCompose.setContent {
            ComposeAppTheme {
                Column(modifier = Modifier.width(IntrinsicSize.Max)) {
                    ButtonPrimaryYellow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                        title = getString(R.string.Button_Confirm),
                        onClick = {
                            logger.info("click confirm button")
                            sendViewModel.send(logger)
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
