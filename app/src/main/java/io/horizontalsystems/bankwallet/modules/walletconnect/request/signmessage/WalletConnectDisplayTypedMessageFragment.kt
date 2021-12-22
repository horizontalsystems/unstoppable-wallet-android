package io.horizontalsystems.bankwallet.modules.walletconnect.request.signmessage

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.databinding.FragmentWalletConnectDisplayTypedMessageBinding
import io.horizontalsystems.bankwallet.modules.walletconnect.request.WalletConnectRequestModule
import io.horizontalsystems.core.findNavController

class WalletConnectDisplayTypedMessageFragment : BaseFragment() {

    private var _binding: FragmentWalletConnectDisplayTypedMessageBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding =
            FragmentWalletConnectDisplayTypedMessageBinding.inflate(inflater, container, false)
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

        binding.messageToSign.text = arguments?.getString(WalletConnectRequestModule.TYPED_MESSAGE)
    }

}
