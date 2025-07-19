package cash.p.terminal.core.di

import android.app.Application
import android.content.Context
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import org.junit.Test
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.verify.verify
import org.mockito.Mockito.mock

class KoinGraphTest : KoinTest {

    @Test
    fun verifyKoinGraph() {
        val testOverrides = module {
            single<Context> { mock() }
            single<Application> { mock() }
            single<HttpClientEngine> { OkHttp.create() }
        }

        val fullModule = module {
            includes(testOverrides, appModule)
        }

        fullModule.verify(extraTypes = listOf(Application::class, Context::class, HttpClientEngine::class))
    }
}
