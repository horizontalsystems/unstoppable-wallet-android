package io.horizontalsystems.bankwallet.modules.createwallet

import android.os.Bundle
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseActivity
import io.horizontalsystems.bankwallet.modules.main.MainModule

class CreateWalletActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_fragment_container)

        supportFragmentManager.beginTransaction().apply {
            replace(R.id.fragmentContainerView, CreateWalletFragment.instance())
            commit()
        }

    }

    fun close(result: Result){
        when(result){
            Result.Success -> {
                MainModule.start(this)
                finishAffinity()
            }
            Result.Cancelation -> {
                finish()
            }
        }
    }

    enum class Result{
        Success, Cancelation
    }

}
