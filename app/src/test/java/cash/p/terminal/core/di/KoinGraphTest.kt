package cash.p.terminal.core.di

import android.app.Application
import android.content.Context
import androidx.lifecycle.SavedStateHandle
import cash.p.terminal.feature.miniapp.ui.connect.ConnectMiniAppViewModel
import cash.p.terminal.modules.blockchainstatus.BlockchainStatusProvider
import cash.p.terminal.modules.blockchainstatus.BlockchainStatusViewModel
import cash.p.terminal.modules.balance.token.addresspoisoning.AddressPoisoningViewModel
import cash.p.terminal.modules.configuredtoken.ConfiguredTokenInfoViewModel
import cash.p.terminal.modules.multiswap.SwapSelectCoinViewModel
import cash.p.terminal.wallet.Account
import io.horizontalsystems.core.entities.BlockchainType
import cash.p.terminal.modules.createaccount.passphraseterms.PassphraseTermsViewModel
import cash.p.terminal.modules.manageaccount.safetyrules.SafetyRulesModule
import cash.p.terminal.modules.manageaccount.safetyrules.SafetyRulesViewModel
import cash.p.terminal.modules.multiswap.TimerService
import cash.p.terminal.modules.pin.hiddenwallet.HiddenWalletPinPolicy
import cash.p.terminal.modules.settings.advancedsecurity.AdvancedSecurityViewModel
import cash.p.terminal.modules.settings.advancedsecurity.securereset.SecureResetTermsViewModel
import cash.p.terminal.modules.settings.advancedsecurity.terms.HiddenWalletTermsViewModel
import cash.p.terminal.modules.walletconnect.AccountTypeNotSupportedDialog
import cash.p.terminal.modules.walletconnect.AccountTypeNotSupportedViewModel
import cash.p.terminal.wallet.Token
import io.horizontalsystems.core.IPinComponent
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import io.mockk.mockk
import org.junit.Test
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.verify.definition
import org.koin.test.verify.injectedParameters
import org.koin.test.verify.verify

class KoinGraphTest : KoinTest {

    @OptIn(KoinExperimentalAPI::class)
    @Test
    fun verifyKoinGraph() {
        val testOverrides = module {
            single<Context> { mockk() }
            single<Application> { mockk() }
            single<HttpClientEngine> { OkHttp.create() }
        }

        val fullModule = module {
            includes(testOverrides, appModule)
        }

        fullModule.verify(
            extraTypes = listOf(Application::class, Context::class, HttpClientEngine::class, TimerService::class),
            injections = injectedParameters(
                definition<AccountTypeNotSupportedViewModel>(AccountTypeNotSupportedDialog.Input::class),
                definition<HiddenWalletPinPolicy >(IPinComponent::class),
                definition<AdvancedSecurityViewModel>(IPinComponent::class),
                definition<SecureResetTermsViewModel>(Array<String>::class),
                definition<HiddenWalletTermsViewModel>(Array<String>::class),
                definition<PassphraseTermsViewModel>(Array<String>::class),
                definition<ConfiguredTokenInfoViewModel>(Token::class),
                definition<SafetyRulesViewModel>(SafetyRulesModule.SafetyRulesMode::class, List::class),
                definition<ConnectMiniAppViewModel>(SavedStateHandle::class),
                definition<BlockchainStatusViewModel>(BlockchainStatusProvider::class),
                definition<SwapSelectCoinViewModel>(Token::class, Account::class),
                definition<AddressPoisoningViewModel>(String::class, Boolean::class, BlockchainType::class),
            )
        )
    }
}
