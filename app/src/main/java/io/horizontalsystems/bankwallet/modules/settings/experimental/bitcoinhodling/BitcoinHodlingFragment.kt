package io.horizontalsystems.bankwallet.modules.settings.experimental.bitcoinhodling

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.databinding.FragmentBitcoinHodlingBinding
import io.horizontalsystems.core.findNavController

class BitcoinHodlingFragment : BaseFragment() {

    private val presenter by viewModels<BitcoinHodlingPresenter> { BitcoinHodlingModule.Factory() }

    private var _binding: FragmentBitcoinHodlingBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBitcoinHodlingBinding.inflate(inflater, container, false)
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

        val hodlingView = presenter.view as BitcoinHodlingView

        hodlingView.lockTimeEnabledLiveEvent.observe(viewLifecycleOwner, Observer { enabled ->
            binding.switchLockTime.setChecked(enabled)
        })

        binding.switchLockTime.setOnClickListener {
            binding.switchLockTime.switchToggle()
        }

        binding.switchLockTime.setOnCheckedChangeListener {
            presenter.onSwitchLockTime(it)
        }

        presenter.onLoad()
    }
}
