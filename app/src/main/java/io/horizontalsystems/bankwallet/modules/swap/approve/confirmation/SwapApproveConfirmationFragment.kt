package io.horizontalsystems.bankwallet.modules.swap.approve.confirmation

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.navigation.navGraphViewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.AppLogger
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.ethereum.EthereumFeeViewModel
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.databinding.FragmentConfirmationApproveSwapBinding
import io.horizontalsystems.bankwallet.modules.sendevm.SendEvmData
import io.horizontalsystems.bankwallet.modules.sendevm.SendEvmModule
import io.horizontalsystems.bankwallet.modules.sendevm.SendEvmModule.additionalInfoKey
import io.horizontalsystems.bankwallet.modules.sendevm.SendEvmModule.transactionDataKey
import io.horizontalsystems.bankwallet.modules.sendevmtransaction.SendEvmTransactionViewModel
import io.horizontalsystems.bankwallet.modules.swap.approve.SwapApproveModule
import io.horizontalsystems.bankwallet.modules.swap.approve.SwapApproveViewModel
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.core.setNavigationResult
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.horizontalsystems.snackbar.CustomSnackbar
import io.horizontalsystems.snackbar.SnackbarDuration

class SwapApproveConfirmationFragment : BaseFragment() {
    private val logger = AppLogger("swap-approve")
    private val mainViewModel by navGraphViewModels<SwapApproveViewModel>(R.id.swapApproveFragment)
    private val vmFactory by lazy {
        SwapApproveConfirmationModule.Factory(
            SendEvmData(transactionData, additionalItems),
            mainViewModel.dex.blockchain
        )
    }
    private val sendViewModel by viewModels<SendEvmTransactionViewModel> { vmFactory }
    private val feeViewModel by viewModels<EthereumFeeViewModel> { vmFactory }
    private val transactionData: TransactionData
        get() {
            val transactionDataParcelable =
                arguments?.getParcelable<SendEvmModule.TransactionDataParcelable>(transactionDataKey)!!
            return TransactionData(
                Address(transactionDataParcelable.toAddress),
                transactionDataParcelable.value,
                transactionDataParcelable.input
            )
        }
    private val additionalItems: SendEvmData.AdditionalInfo?
        get() = arguments?.getParcelable(additionalInfoKey)

    private var snackbarInProcess: CustomSnackbar? = null

    private var _binding: FragmentConfirmationApproveSwapBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentConfirmationApproveSwapBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        snackbarInProcess?.dismiss()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menuClose -> {
                    findNavController().popBackStack(R.id.swapApproveFragment, true)
                    true
                }
                else -> false
            }
        }
        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        sendViewModel.sendEnabledLiveData.observe(viewLifecycleOwner, { enabled ->
            setButton(enabled)
        })

        sendViewModel.sendingLiveData.observe(viewLifecycleOwner, {
            snackbarInProcess = HudHelper.showInProcessMessage(
                requireView(),
                R.string.Swap_Approving,
                SnackbarDuration.INDEFINITE
            )
        })

        sendViewModel.sendSuccessLiveData.observe(viewLifecycleOwner, {
            HudHelper.showSuccessMessage(
                requireActivity().findViewById(android.R.id.content),
                R.string.Hud_Text_Done
            )
            Handler(Looper.getMainLooper()).postDelayed({
                setNavigationResult(
                    SwapApproveModule.requestKey,
                    bundleOf(SwapApproveModule.resultKey to true)
                )
                findNavController().popBackStack(R.id.swapApproveFragment, false)
            }, 1200)
        })

        sendViewModel.sendFailedLiveData.observe(viewLifecycleOwner, {
            HudHelper.showErrorMessage(requireActivity().findViewById(android.R.id.content), it)

            findNavController().popBackStack()
        })

        binding.sendEvmTransactionView.init(
            sendViewModel,
            feeViewModel,
            viewLifecycleOwner,
            parentFragmentManager,
            showSpeedInfoListener = {
                findNavController().slideFromRight(
                    R.id.swapApproveConfirmationFragment_to_feeSpeedInfo
                )
            }
        )

        binding.buttonApproveCompose.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
        )

        setButton()
    }

    private fun setButton(enabled: Boolean = false) {
        binding.buttonApproveCompose.setContent {
            ComposeAppTheme {
                ButtonPrimaryYellow(
                    modifier = Modifier.padding(
                        start = 16.dp,
                        top = 24.dp,
                        end = 16.dp,
                        bottom = 24.dp
                    ),
                    title = getString(R.string.Swap_Approve),
                    onClick = {
                        logger.info("click approve button")
                        sendViewModel.send(logger)
                    },
                    enabled = enabled
                )
            }
        }
    }

}
