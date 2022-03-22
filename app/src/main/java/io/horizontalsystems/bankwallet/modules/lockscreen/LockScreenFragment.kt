package io.horizontalsystems.bankwallet.modules.lockscreen

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentResultListener
import androidx.fragment.app.commit
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.databinding.FragmentLockscreenBinding
import io.horizontalsystems.pin.PinFragment
import io.horizontalsystems.pin.PinInteractionType
import io.horizontalsystems.pin.PinModule

class LockScreenFragment : BaseFragment(), FragmentResultListener {

    private var _binding: FragmentLockscreenBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLockscreenBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val pinFragment = PinFragment().apply {
            arguments = Bundle(1).apply {
                putBoolean(PinFragment.ATTACHED_TO_LOCKSCREEN, true)
            }
        }

        childFragmentManager.commit {
            replace(R.id.fragmentContainerView, pinFragment)
        }

        childFragmentManager.setFragmentResultListener(PinModule.requestKey, this, this)
    }

    //  FragmentResultListener

    override fun onFragmentResult(requestKey: String, bundle: Bundle) {
        val resultType = bundle.getParcelable<PinInteractionType>(PinModule.requestType)
        if (resultType == PinInteractionType.UNLOCK) {
            when (bundle.getInt(PinModule.requestResult)) {
                PinModule.RESULT_OK -> activity?.setResult(PinModule.RESULT_OK)
                PinModule.RESULT_CANCELLED -> activity?.setResult(PinModule.RESULT_CANCELLED)
            }

            activity?.finish()
        }
    }
}
