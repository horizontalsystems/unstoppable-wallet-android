package io.horizontalsystems.bankwallet.modules.welcome

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import io.horizontalsystems.bankwallet.BuildConfig
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.bankwallet.modules.createwallet.CreateWalletModule
import io.horizontalsystems.bankwallet.modules.restore.RestoreModule
import io.horizontalsystems.bankwallet.modules.settings.security.privacy.PrivacySettingsModule
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
            activity?.let { activity -> RestoreModule.start(activity, false) }
        })

        viewModel.openCreateWalletModule.observe(viewLifecycleOwner, Observer {
            activity?.let { activity -> CreateWalletModule.start(activity, false) }
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
            activity?.let { activity -> PrivacySettingsModule.start(activity) }
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
    }

    companion object {
        fun instance(): WelcomeFragment {
            return WelcomeFragment()
        }
    }

}
