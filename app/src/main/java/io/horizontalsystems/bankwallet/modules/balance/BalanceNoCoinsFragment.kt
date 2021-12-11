package io.horizontalsystems.bankwallet.modules.balance

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.modules.manageaccounts.ManageAccountsModule
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonSecondaryDefault
import io.horizontalsystems.core.findNavController
import kotlinx.android.synthetic.main.fragment_no_coins.*

class BalanceNoCoinsFragment(private val accountName: String?) : BaseFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_no_coins, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbarTitle.text = accountName ?: getString(R.string.Balance_Title)
        toolbarTitle.setOnClickListener {
            ManageAccountsModule.start(this, R.id.manageAccountsFragment, navOptionsFromBottom(), ManageAccountsModule.Mode.Switcher)
        }

        addCoinsButtonCompose.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
        )

        addCoinsButtonCompose.setContent {
            ComposeAppTheme {
                ButtonSecondaryDefault(
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp),
                    title = getString(R.string.Balance_AddCoins),
                    onClick = {
                        findNavController().navigate(R.id.manageWalletsFragment, null, navOptions())
                    }
                )
            }
        }
    }

}
