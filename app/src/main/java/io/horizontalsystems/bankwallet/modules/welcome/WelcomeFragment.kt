package io.horizontalsystems.bankwallet.modules.welcome

import android.os.Bundle
import android.transition.TransitionInflater
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import io.horizontalsystems.bankwallet.BuildConfig
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.bankwallet.modules.restore.RestoreFragment
import io.horizontalsystems.core.findNavController
import kotlinx.android.synthetic.main.fragment_welcome.*

class WelcomeFragment : BaseFragment() {

    private val viewModel by viewModels<WelcomeViewModel>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_welcome, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.init()

        viewModel.openRestoreModule.observe(viewLifecycleOwner, Observer {
            val arguments = Bundle(3).apply {
                putParcelable(RestoreFragment.PREDEFINED_ACCOUNT_TYPE_KEY, null)
                putBoolean(RestoreFragment.SELECT_COINS_KEY, true)
                putBoolean(RestoreFragment.IN_APP_KEY, false)
            }

            findNavController().navigate(R.id.welcomeFragment_to_restoreFragment, arguments, navOptions())
        })

        viewModel.openCreateWalletModule.observe(viewLifecycleOwner, Observer {
            val arguments = Bundle(2).apply {
                putParcelable("predefinedAccountType", null)
                putBoolean("inApp", false)
            }

            findNavController().navigate(R.id.welcomeFragment_to_welcomeCreateWalletFragment, arguments, navOptions())
        })

        viewModel.appVersionLiveData.observe(viewLifecycleOwner, Observer { appVersion ->
            appVersion?.let {
                var version = it
                if (getString(R.string.is_release) == "false") {
                    version = "$version (${BuildConfig.VERSION_CODE})"
                }
                textVersion.text = getString(R.string.Welcome_Version, version)
            }
        })

        viewModel.openTorPage.observe(viewLifecycleOwner, Observer {
            findNavController().navigate(R.id.welcomeFragment_to_privacySettingsFragment, null, navOptions())
        })

        buttonCreate.setOnSingleClickListener {
            viewModel.delegate.createWalletDidClick()
        }

        buttonRestore.setOnSingleClickListener {
            viewModel.delegate.restoreWalletDidClick()
        }

        privacySettings.setOnSingleClickListener {
            viewModel.delegate.openTorPage()
        }

        sharedElementEnterTransition = TransitionInflater.from(context).inflateTransition(R.transition.shared_image)
    }
}
