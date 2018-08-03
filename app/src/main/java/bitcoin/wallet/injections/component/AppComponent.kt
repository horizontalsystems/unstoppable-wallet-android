package bitcoin.wallet.injections.component

import bitcoin.wallet.core.App
import bitcoin.wallet.injections.module.AppModule
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [AppModule::class])
interface AppComponent {

    fun inject(app: App)

}
