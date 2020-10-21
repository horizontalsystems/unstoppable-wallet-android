package io.horizontalsystems.bankwallet.modules.settings.security.privacy

import android.os.Bundle
import android.view.*
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI.navigateUp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.extensions.NavDestinationChangeListener
import kotlinx.android.synthetic.main.fragment_settings_privacy.*

class PrivacySettingsInfoFragment : BaseFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_privacy_settings_info, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val navController = findNavController()
        val appBarConfiguration = AppBarConfiguration(navController.graph)

        val navDestinationChangeListener = NavDestinationChangeListener(toolbar, appBarConfiguration, false)
        navController.addOnDestinationChangedListener(navDestinationChangeListener)
        toolbar.setNavigationOnClickListener { navigateUp(navController, appBarConfiguration) }

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

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.settings_privacy_info_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.closeButton -> {
                findNavController().navigateUp()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}
