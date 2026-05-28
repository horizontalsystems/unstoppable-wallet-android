package cash.p.terminal.tangem.di

import cash.p.terminal.tangem.domain.sdk.CardSdkProvider
import io.horizontalsystems.core.BackgroundManager
import io.mockk.mockk
import org.junit.Test
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.assertNotNull

class FeatureTangemModuleTest {

    @Test
    fun featureTangemModule_resolvesCardSdkProviderWithoutPinComponentBinding() {
        val koin = koinApplication {
            modules(
                module {
                    single<BackgroundManager> { mockk(relaxed = true) }
                },
                featureTangemModule,
            )
        }.koin

        assertNotNull(koin.get<CardSdkProvider>())
    }
}
