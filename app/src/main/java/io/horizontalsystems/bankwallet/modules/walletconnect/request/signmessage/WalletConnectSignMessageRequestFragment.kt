package io.horizontalsystems.bankwallet.modules.walletconnect.request.signmessage

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.navGraphViewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.databinding.FragmentWalletConnectSignMessageRequestBinding
import io.horizontalsystems.bankwallet.modules.walletconnect.WalletConnectViewModel
import io.horizontalsystems.bankwallet.modules.walletconnect.request.WalletConnectRequestModule.TYPED_MESSAGE
import io.horizontalsystems.bankwallet.modules.walletconnect.request.signmessage.WalletConnectSignMessageRequestService.SignMessage
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryDefault
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.views.ListPosition

class WalletConnectSignMessageRequestFragment : BaseFragment() {

    private var _binding: FragmentWalletConnectSignMessageRequestBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding =
            FragmentWalletConnectSignMessageRequestBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val baseViewModel by navGraphViewModels<WalletConnectViewModel>(R.id.walletConnectMainFragment)
        val vmFactory = WalletConnectSignMessageRequestModule.Factory(
            baseViewModel.sharedSignMessageRequest!!,
            baseViewModel.service
        )
        val viewModel by viewModels<WalletConnectSignMessageRequestViewModel> { vmFactory }

        when (val message = viewModel.message) {
            is SignMessage.Message,
            is SignMessage.PersonalMessage -> {
                binding.messageHint.text = message.data
                binding.typedMessageViews.isVisible = false
            }
            is SignMessage.TypedMessage -> {
                binding.messageHint.text =
                    getString(R.string.WalletConnect_SignMessageRequest_SignMessageHint)
                binding.domain.bind(
                    title = getString(R.string.WalletConnect_SignMessageRequest_Domain),
                    value = message.domain,
                    listPosition = ListPosition.First
                )
                binding.showMessageButton.apply {
                    bind(
                        title = getString(R.string.WalletConnect_SignMessageRequest_ShowMessageTitle),
                        icon = R.drawable.ic_arrow_right,
                        listPosition = ListPosition.Last
                    )
                    setOnClickListener {
                        findNavController().navigate(
                            R.id.walletConnectSignMessageRequestFragment_to_walletConnectDisplayTypedMessageFragment,
                            bundleOf(TYPED_MESSAGE to formatJson(message.data)),
                            navOptions()
                        )
                    }
                }
                binding.typedMessageViews.isVisible = true
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            viewModel.reject()
        }

        viewModel.closeLiveEvent.observe(viewLifecycleOwner, {
            baseViewModel.sharedSignMessageRequest = null
            findNavController().popBackStack()
        })

        binding.buttonsCompose.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
        )

        binding.buttonsCompose.setContent {
            ComposeAppTheme {
                Column(modifier = Modifier.width(IntrinsicSize.Max)) {
                    ButtonPrimaryYellow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                        title = getString(R.string.WalletConnect_SignMessageRequest_ButtonSign),
                        onClick = {
                            viewModel.sign()
                        }
                    )
                    ButtonPrimaryDefault(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
                        title = getString(R.string.Button_Reject),
                        onClick = {
                            viewModel.reject()
                        }
                    )
                }
            }
        }
    }

    private fun formatJson(text: String): String {
        val json = StringBuilder()
        var indentString = ""
        for (element in text) {
            when (element) {
                '{', '[' -> {
                    json.append("\n$indentString$element\n")
                    indentString += "\t"
                    json.append(indentString)
                }
                '}', ']' -> {
                    indentString = indentString.replaceFirst("\t".toRegex(), "")
                    json.append("\n$indentString$element")
                }
                ',' -> json.append("$element\n$indentString")
                else -> json.append(element)
            }
        }
        return json.toString()
    }

}
