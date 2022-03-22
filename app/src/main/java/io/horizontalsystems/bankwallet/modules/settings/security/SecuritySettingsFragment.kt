package io.horizontalsystems.bankwallet.modules.settings.security

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.databinding.FragmentSettingsSecurityBinding
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.getNavigationResult
import io.horizontalsystems.pin.PinInteractionType
import io.horizontalsystems.pin.PinModule
import io.horizontalsystems.views.ListPosition

class SecuritySettingsFragment : BaseFragment() {

    private val viewModel by viewModels<SecuritySettingsViewModel>()

    private var _binding: FragmentSettingsSecurityBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsSecurityBinding.inflate(inflater, container, false)
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

        viewModel.init()

        binding.changePin.setOnSingleClickListener {
            viewModel.delegate.didTapEditPin()
        }

        binding.privacy.setOnSingleClickListener {
            viewModel.delegate.didTapPrivacy()
        }

        binding.biometricAuth.setOnClickListener {
            binding.biometricAuth.switchToggle()
        }

        binding.biometricAuth.setOnCheckedChangeListener {
            viewModel.delegate.didSwitchBiometricEnabled(it)
        }

        binding.enablePin.setOnSingleClickListener {
            binding.enablePin.switchToggle()
        }

        binding.enablePin.setOnCheckedChangeListenerSingle {
            viewModel.delegate.didSwitchPinSet(it)
        }

        //  Handling view model live events

        viewModel.pinSetLiveData.observe(viewLifecycleOwner, Observer { pinEnabled ->
            binding.enablePin.setChecked(pinEnabled)
            binding.enablePin.showAttention(!pinEnabled)
        })

        viewModel.editPinVisibleLiveData.observe(viewLifecycleOwner, Observer { pinEnabled ->
            binding.changePin.isVisible = pinEnabled
            binding.enablePin.setListPosition(if (pinEnabled) ListPosition.First else ListPosition.Single)
            if (pinEnabled) {
                binding.changePin.setListPosition(ListPosition.Last)
            }
        })

        viewModel.biometricSettingsVisibleLiveData.observe(viewLifecycleOwner, Observer { enabled ->
            binding.biometricAuth.isVisible = enabled
            binding.txtBiometricAuthInfo.isVisible = enabled
        })

        viewModel.biometricEnabledLiveData.observe(viewLifecycleOwner, Observer {
            binding.biometricAuth.setChecked(it)
        })

        //  Router

        viewModel.openEditPinLiveEvent.observe(viewLifecycleOwner, Observer {
            findNavController().slideFromRight(
                R.id.securitySettingsFragment_to_pinFragment,
                PinModule.forEditPin()
            )
        })

        viewModel.openSetPinLiveEvent.observe(viewLifecycleOwner, Observer {
            findNavController().slideFromRight(
                R.id.securitySettingsFragment_to_pinFragment,
                PinModule.forSetPin()
            )
        })

        viewModel.openUnlockPinLiveEvent.observe(viewLifecycleOwner, Observer {
            findNavController().slideFromRight(
                R.id.securitySettingsFragment_to_pinFragment,
                PinModule.forUnlock()
            )
        })

        viewModel.openPrivacySettingsLiveEvent.observe(viewLifecycleOwner, Observer {
            findNavController().slideFromRight(
                R.id.securitySettingsFragment_to_privacySettingsFragment
            )
        })

        subscribeFragmentResult()
    }

    private fun subscribeFragmentResult() {
        getNavigationResult(PinModule.requestKey)?.let { bundle ->
            val resultType = bundle.getParcelable<PinInteractionType>(PinModule.requestType)
            val resultCode = bundle.getInt(PinModule.requestResult)

            if (resultType == PinInteractionType.SET_PIN) {
                when (resultCode) {
                    PinModule.RESULT_OK -> viewModel.delegate.didSetPin()
                    PinModule.RESULT_CANCELLED -> viewModel.delegate.didCancelSetPin()
                }
            }

            if (resultType == PinInteractionType.UNLOCK) {
                when (resultCode) {
                    PinModule.RESULT_OK -> viewModel.delegate.didUnlockPinToDisablePin()
                    PinModule.RESULT_CANCELLED -> viewModel.delegate.didCancelUnlockPinToDisablePin()
                }
            }
        }
    }
}
