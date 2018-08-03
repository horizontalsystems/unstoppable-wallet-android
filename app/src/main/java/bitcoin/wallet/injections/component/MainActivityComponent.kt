package bitcoin.wallet.injections.component

import bitcoin.wallet.injections.module.AppModule
import bitcoin.wallet.injections.module.MainActivityModule
import bitcoin.wallet.modules.main.MainActivity
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [AppModule::class, MainActivityModule::class])
interface MainActivityComponent {

    fun inject(mainActivity: MainActivity)

}
