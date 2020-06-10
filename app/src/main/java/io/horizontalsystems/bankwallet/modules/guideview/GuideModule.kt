package io.horizontalsystems.bankwallet.modules.guideview

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.entities.Guide
import io.horizontalsystems.core.putParcelableExtra

object GuideModule {
    const val GuideKey = "GuideKey"

    fun start(context: Context, guide: Guide) {
        val intent = Intent(context, GuideActivity::class.java)
        intent.putParcelableExtra(GuideKey, guide)

        context.startActivity(intent)
    }


    class Factory(private val guide: Guide?) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
//            val interactor = GuideInteractor()
            val viewModel = GuideViewModel(guide)

//            interactor.delegate = presenter

            return viewModel as T
        }
    }
}
