package io.horizontalsystems.bankwallet.modules.settings.security.privacy

import android.os.Bundle
import android.view.*
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.commit
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.views.TopMenuItem
import kotlinx.android.synthetic.main.fragment_privacy_settings_info.*

class PrivacySettingsInfoFragment : BaseFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_privacy_settings_info, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        shadowlessToolbar.bind(
                title = getString(R.string.Welcome_PrivacySettings),
                rightBtnItem = TopMenuItem(text = R.string.Alert_Close, onClick = {
                    parentFragmentManager.popBackStack()
                })
        )
    }

    companion object {
        fun start(activity: FragmentActivity) {
            activity.supportFragmentManager.commit {
                add(R.id.fragmentContainerView, PrivacySettingsInfoFragment())
                addToBackStack(null)
            }
        }
    }
}
