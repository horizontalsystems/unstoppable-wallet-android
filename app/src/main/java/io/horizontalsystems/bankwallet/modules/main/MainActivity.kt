package io.horizontalsystems.bankwallet.modules.main

import android.os.Bundle
import androidx.activity.viewModels
import androidx.navigation.fragment.NavHostFragment
import com.walletconnect.walletconnectv2.client.WalletConnectClient
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseActivity
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.modules.walletconnect.request.sendtransaction.v2.WC2SendEthereumTransactionRequestFragment
import io.horizontalsystems.bankwallet.modules.walletconnect.request.signmessage.v2.WC2SignMessageRequestFragment
import io.horizontalsystems.bankwallet.modules.walletconnect.version2.WC2SendEthereumTransactionRequest
import io.horizontalsystems.bankwallet.modules.walletconnect.version2.WC2SignMessageRequest

class MainActivity : BaseActivity() {

    val viewModel by viewModels<MainActivityViewModel>(){
        MainModule.FactoryForActivityViewModel()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        val navHost = supportFragmentManager.findFragmentById(R.id.fragmentContainerView) as NavHostFragment

        navHost.navController.setGraph(R.navigation.main_graph, intent.extras)
        navHost.navController.addOnDestinationChangedListener(this)

        viewModel.openWalletConnectRequestLiveEvent.observe(this) { wcRequest ->
            when (wcRequest) {
                is WC2SignMessageRequest -> {
                    navHost.navController.slideFromBottom(
                        R.id.wc2SignMessageRequestFragment,
                        WC2SignMessageRequestFragment.prepareParams(wcRequest.id)
                    )
                }
                is WC2SendEthereumTransactionRequest -> {
                    navHost.navController.slideFromBottom(
                        R.id.wc2SendEthereumTransactionRequestFragment,
                        WC2SendEthereumTransactionRequestFragment.prepareParams(wcRequest.id)
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        WalletConnectClient.shutdown()
    }
}
