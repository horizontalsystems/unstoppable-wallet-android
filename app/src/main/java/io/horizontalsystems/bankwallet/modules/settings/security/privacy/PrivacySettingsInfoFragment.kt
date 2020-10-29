package io.horizontalsystems.bankwallet.modules.settings.security.privacy

import android.os.Bundle
import android.view.*
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import kotlinx.android.synthetic.main.fragment_settings_privacy.toolbar

class PrivacySettingsInfoFragment : BaseFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_privacy_settings_info, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar.inflateMenu(R.menu.settings_privacy_info_menu)
        toolbar.setOnMenuItemClickListener { menuItem ->
            if (menuItem.itemId == R.id.closeButton){
                findNavController().navigateUp()
                true
            } else {
                super.onOptionsItemSelected(menuItem)
            }
        }
    }

}
