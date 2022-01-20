package io.horizontalsystems.bankwallet.modules.transactionInfo.options

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxWidth
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
import io.horizontalsystems.bankwallet.databinding.FragmentTransactionSpeedupCancelBinding
import io.horizontalsystems.bankwallet.modules.sendevmtransaction.SendEvmTransactionViewModel
import io.horizontalsystems.bankwallet.modules.transactionInfo.TransactionInfoOption
import io.horizontalsystems.bankwallet.modules.transactionInfo.TransactionInfoViewModel
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.snackbar.CustomSnackbar
import io.horizontalsystems.snackbar.SnackbarDuration

class TransactionSpeedUpCancelFragment : BaseFragment() {

    private val logger = AppLogger("tx-speedUp-cancel")
    private val transactionInfoViewModel by navGraphViewModels<TransactionInfoViewModel>(R.id.transactionInfoFragment)
    private val optionType by lazy {
        arguments?.getParcelable<TransactionInfoOption.Type>(
            OPTION_TYPE_KEY
        )!!
    }
    private val transactionHash by lazy { arguments?.getString(TRANSACTION_HASH_KEY)!! }

    private val vmFactory by lazy {
        TransactionInfoOptionsModule.Factory(
            optionType,
            transactionHash,
            transactionInfoViewModel.source
        )
    }
    private val speedUpCancelViewModel by viewModels<TransactionSpeedUpCancelViewModel> { vmFactory }
    private val sendEvmTransactionViewModel by viewModels<SendEvmTransactionViewModel> { vmFactory }
    private val feeViewModel by viewModels<EthereumFeeViewModel> { vmFactory }

    private var snackbarInProcess: CustomSnackbar? = null

    private var _binding: FragmentTransactionSpeedupCancelBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTransactionSpeedupCancelBinding.inflate(inflater, container, false)
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
                    findNavController().popBackStack(R.id.transactionInfoFragment, true)
                    true
                }
                else -> false
            }
        }

        binding.toolbar.title = speedUpCancelViewModel.title
        binding.description.text = speedUpCancelViewModel.description

        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        sendEvmTransactionViewModel.sendingLiveData.observe(viewLifecycleOwner, {
            snackbarInProcess = HudHelper.showInProcessMessage(
                requireView(),
                R.string.Send_Sending,
                SnackbarDuration.INDEFINITE
            )
        })

        sendEvmTransactionViewModel.sendSuccessLiveData.observe(
            viewLifecycleOwner,
            { transactionHash ->
                HudHelper.showSuccessMessage(
                    requireActivity().findViewById(android.R.id.content),
                    R.string.Hud_Text_Done
                )
                Handler(Looper.getMainLooper()).postDelayed({
                    findNavController().popBackStack(R.id.transactionInfoFragment, true)
                }, 1200)
            })

        sendEvmTransactionViewModel.sendFailedLiveData.observe(viewLifecycleOwner, {
            HudHelper.showErrorMessage(requireActivity().findViewById(android.R.id.content), it)

            Handler(Looper.getMainLooper()).postDelayed({
                findNavController().popBackStack()
            }, 1200)
        })

        binding.sendEvmTransactionView.init(
            sendEvmTransactionViewModel,
            feeViewModel,
            viewLifecycleOwner,
            parentFragmentManager,
            showSpeedInfoListener = {
                findNavController().slideFromRight(
                    R.id.transactionSpeedUpCancelFragment_to_feeSpeedInfo
                )
            }
        )

        if (speedUpCancelViewModel.isTransactionPending) {
            sendEvmTransactionViewModel.sendEnabledLiveData.observe(viewLifecycleOwner, { enabled ->
                setButton(enabled)
            })
        } else {
            setButton(false)
            HudHelper.showErrorMessage(
                requireActivity().findViewById(android.R.id.content),
                R.string.TransactionInfoOptions_Warning_TransactionInBlock
            )
            Handler(Looper.getMainLooper()).postDelayed({
                findNavController().popBackStack(R.id.transactionInfoFragment, true)
            }, 1500)
        }

        binding.buttonSendCompose.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
        )

        setButton()
    }

    private fun setButton(enabled: Boolean = false) {
        binding.buttonSendCompose.setContent {
            ComposeAppTheme {
                ButtonPrimaryYellow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, top = 24.dp, end = 16.dp),
                    title = speedUpCancelViewModel.buttonTitle,
                    onClick = {
                        logger.info("click send button")
                        sendEvmTransactionViewModel.send(logger)
                    },
                    enabled = enabled
                )
            }
        }
    }

    companion object {
        private const val OPTION_TYPE_KEY = "option_type_key"
        private const val TRANSACTION_HASH_KEY = "transaction_hash_key"

        fun prepareParams(
            optionType: TransactionInfoOption.Type,
            transactionHash: String
        ): Bundle {
            return bundleOf(
                OPTION_TYPE_KEY to optionType,
                TRANSACTION_HASH_KEY to transactionHash
            )
        }
    }

}
