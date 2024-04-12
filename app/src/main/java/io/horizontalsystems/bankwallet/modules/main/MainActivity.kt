package io.horizontalsystems.bankwallet.modules.main

import android.os.Bundle
import androidx.activity.viewModels
import androidx.navigation.fragment.NavHostFragment
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseActivity
import io.horizontalsystems.core.hideKeyboard

class MainActivity : BaseActivity() {

    private val viewModel by viewModels<MainActivityViewModel> {
        MainActivityViewModel.Factory()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        val navHost =
            supportFragmentManager.findFragmentById(R.id.fragmentContainerView) as NavHostFragment
        val navController = navHost.navController

        navController.setGraph(R.navigation.main_graph, intent.extras)
        navController.addOnDestinationChangedListener { _, _, _ ->
            currentFocus?.hideKeyboard(this)
        }

        viewModel.navigateToMainLiveData.observe(this) {
            if (it) {
                navController.popBackStack(navController.graph.startDestinationId, false)
                viewModel.onNavigatedToMain()
            }
        }
    }
}
