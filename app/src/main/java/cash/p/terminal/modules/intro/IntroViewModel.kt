package cash.p.terminal.modules.intro

import androidx.lifecycle.ViewModel
import cash.p.terminal.R
import cash.p.terminal.core.ILocalStorage

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
