package io.horizontalsystems.bankwallet.modules.restore

import android.os.Bundle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.NavGraph
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseActivity
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.PredefinedAccountType
import io.horizontalsystems.bankwallet.modules.main.MainModule
import io.horizontalsystems.bankwallet.modules.restore.restoreselectcoins.RestoreSelectCoinsFragment
import io.horizontalsystems.bankwallet.modules.restore.words.RestoreWordsFragment


class RestoreActivity : BaseActivity() {

    private lateinit var viewModel: RestoreViewModel
    private var inApp = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_restore)

        val predefinedAccountType: PredefinedAccountType? = intent.getParcelableExtra(PREDEFINED_ACCOUNT_TYPE_KEY)
        inApp = intent.getBooleanExtra(IN_APP_KEY, false)
        val selectCoins = intent.getBooleanExtra(SELECT_COINS_KEY, false)

        viewModel = ViewModelProvider(this, RestoreModule.Factory(selectCoins, predefinedAccountType))
                .get(RestoreViewModel::class.java)

        setStartDestination()

        observe()
    }

    override fun onBackPressed() {
        if(findNavController(R.id.fragmentContainerView).popBackStack().not()) {
            closeActivity()
        }
    }

    fun onPredefinedTypeSelect(predefinedAccountType: PredefinedAccountType){
        viewModel.onSelect(predefinedAccountType)
    }

    fun onRestore(accountType: AccountType){
        viewModel.onEnter(accountType)
    }

    fun onCoinsEnabled(enabledCoins: List<Coin>) {
        viewModel.onSelect(enabledCoins)
    }

    private fun setStartDestination() {
        val navController: NavController = findNavController(R.id.fragmentContainerView)
        val navGraph: NavGraph = navController.navInflater.inflate(R.navigation.restore_graph)

        val arguments = Bundle()
        when (val screen = viewModel.initialScreen) {
            RestoreViewModel.Screen.SelectPredefinedAccountType -> {
                navGraph.startDestination = R.id.predefinedAccountTypesFragment
            }
            is RestoreViewModel.Screen.RestoreAccountType -> {
                when (val type = screen.predefinedAccountType) {
                    PredefinedAccountType.Standard -> {
                        navGraph.startDestination = R.id.restoreWordsFragment
                        arguments.apply {
                            putInt(RestoreWordsFragment.wordsCountKey, 12)
                            putInt(RestoreWordsFragment.titleKey, type.title)
                        }
                    }
                    PredefinedAccountType.Binance -> {
                        navGraph.startDestination = R.id.restoreWordsFragment
                        arguments.apply {
                            putInt(RestoreWordsFragment.wordsCountKey, 24)
                            putInt(RestoreWordsFragment.titleKey, type.title)
                        }
                    }
                    PredefinedAccountType.Eos -> {
                        navGraph.startDestination = R.id.restoreEosFragment
                    }
                }
            }
        }

        navController.setGraph(navGraph, arguments)
    }

    private fun observe() {
        viewModel.openScreenLiveEvent.observe(this, Observer {
            openScreen(it)
        })

        viewModel.finishLiveEvent.observe(this, Observer {
            closeWithSuccess()
        })
    }

    private fun closeWithSuccess() {
        if (inApp) {
            closeActivity()
        } else {
            MainModule.start(this)
            finishAffinity()
        }
    }

    private fun closeActivity() {
        finish()
        overridePendingTransition(0, R.anim.to_right)
    }

    private fun openScreen(screen: RestoreViewModel.Screen) {
        when (screen) {
            is RestoreViewModel.Screen.RestoreAccountType -> {
                when (screen.predefinedAccountType) {
                    PredefinedAccountType.Standard,
                    PredefinedAccountType.Binance -> {
                        val wordsCount = if (screen.predefinedAccountType == PredefinedAccountType.Standard) 12 else 24

                        val arguments = Bundle(2).apply {
                            putInt(RestoreWordsFragment.wordsCountKey, wordsCount)
                            putInt(RestoreWordsFragment.titleKey, screen.predefinedAccountType.title)
                        }
                        findNavController(R.id.fragmentContainerView).navigate(R.id.restoreWordsFragment, arguments, navOptions())
                    }
                    PredefinedAccountType.Eos -> {
                        findNavController(R.id.fragmentContainerView).navigate(R.id.restoreEosFragment, null, navOptions())
                    }
                }
            }
            is RestoreViewModel.Screen.SelectCoins -> {
                val arguments = Bundle(1).apply {
                    putParcelable(RestoreSelectCoinsFragment.PREDEFINED_ACCOUNT_TYPE_KEY, screen.predefinedAccountType)
                }
                findNavController(R.id.fragmentContainerView).navigate(R.id.restoreSelectCoinsFragment, arguments, navOptions())
            }
            else -> { }
        }
    }

    private fun navOptions(): NavOptions {
        return NavOptions.Builder()
                .setEnterAnim(R.anim.from_right)
                .setExitAnim(R.anim.to_left)
                .setPopEnterAnim(R.anim.from_left)
                .setPopExitAnim(R.anim.to_right)
                .build()
    }


    companion object {
        const val PREDEFINED_ACCOUNT_TYPE_KEY = "predefined_account_type_key"
        const val SELECT_COINS_KEY = "select_coins_key"
        const val IN_APP_KEY = "in_app_key"
    }
}
