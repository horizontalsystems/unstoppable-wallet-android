package com.quantum.wallet.bankwallet.modules.intro

import androidx.lifecycle.ViewModel
import com.quantum.wallet.bankwallet.R
import com.quantum.wallet.bankwallet.core.ILocalStorage

class IntroViewModel(
        private val localStorage: ILocalStorage
): ViewModel() {

    val slides = listOf(
        IntroModule.IntroSliderData(
            R.string.Intro_Wallet_Screen2Title,
            R.string.Intro_Wallet_Screen2Description,
            R.drawable.ic_independence_light,
            R.drawable.ic_independence
        ),
        IntroModule.IntroSliderData(
            R.string.Intro_Wallet_Screen3Title,
            R.string.Intro_Wallet_Screen3Description,
            R.drawable.ic_knowledge_light,
            R.drawable.ic_knowledge
        ),
        IntroModule.IntroSliderData(
            R.string.Intro_Wallet_Screen4Title,
            R.string.Intro_Wallet_Screen4Description,
            R.drawable.ic_privacy_light,
            R.drawable.ic_privacy
        ),
    )

    fun onStartClicked() {
        localStorage.mainShowedOnce = true
    }

}
