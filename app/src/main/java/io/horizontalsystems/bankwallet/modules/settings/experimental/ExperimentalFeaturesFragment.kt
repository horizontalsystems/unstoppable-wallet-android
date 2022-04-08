package io.horizontalsystems.bankwallet.modules.settings.experimental

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.databinding.FragmentExperimentalFeaturesBinding
import io.horizontalsystems.core.findNavController

class ExperimentalFeaturesFragment : BaseFragment() {

    private val presenter by viewModels<ExperimentalFeaturesPresenter> { ExperimentalFeaturesModule.Factory() }

    private var _binding: FragmentExperimentalFeaturesBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExperimentalFeaturesBinding.inflate(inflater, container, false)
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

        val router = presenter.router as ExperimentalFeaturesRouter

        router.showBitcoinHodlingLiveEvent.observe(viewLifecycleOwner, Observer {
            findNavController().slideFromRight(
                R.id.bitcoinHodlingFragment
            )
        })

        binding.bitcoinHodling.setOnSingleClickListener {
            presenter.didTapBitcoinHodling()
        }
    }
}
