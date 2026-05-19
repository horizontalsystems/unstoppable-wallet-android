package cash.p.terminal.tangem.di

import cash.p.terminal.tangem.domain.sdk.CardSdkProvider
import io.horizontalsystems.core.BackgroundManager
import io.horizontalsystems.core.IPinComponent
import io.mockk.mockk
import org.junit.Test
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.assertNotNull

class FeatureTangemModuleTest {

    @Test
    fun featureTangemModule_resolvesCardSdkProvider() {
        val koin = koinApplication {
            modules(
                module {
                    single<BackgroundManager> { mockk(relaxed = true) }
                    single<IPinComponent> { mockk(relaxed = true) }
                },
                featureTangemModule,
            )
        }.koin

        assertNotNull(koin.get<CardSdkProvider>())
    }
}
