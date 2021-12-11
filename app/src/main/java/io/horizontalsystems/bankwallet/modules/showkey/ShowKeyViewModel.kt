package io.horizontalsystems.bankwallet.modules.showkey

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.core.SingleLiveEvent

class ShowKeyViewModel(
        private val service: ShowKeyService
) : ViewModel() {
    val openUnlockLiveEvent = SingleLiveEvent<Unit>()
    val showKeyLiveEvent = SingleLiveEvent<Unit>()
    val showKeyTabs = listOf(ShowKeyModule.ShowKeyTab.MnemonicPhrase, ShowKeyModule.ShowKeyTab.PrivateKey)
    val selectedTab = MutableLiveData(ShowKeyModule.ShowKeyTab.MnemonicPhrase)

    val words: List<String>
        get() = service.words

    val passphrase: String
        get() = service.passphrase

    val privateKeys: List<ShowKeyModule.PrivateKey>
        get() = listOf(
            ShowKeyModule.PrivateKey("Ethereum", service.ethereumPrivateKey),
            ShowKeyModule.PrivateKey("Binance Smart Chain", service.binanceSmartChainPrivateKey),
        )

    fun onClickShow() {
        if (service.isPinSet) {
            openUnlockLiveEvent.postValue(Unit)
        } else {
            showKeyLiveEvent.postValue(Unit)
        }
    }

    fun onUnlock() {
        showKeyLiveEvent.postValue(Unit)
    }

    fun onSelectTab(tab: ShowKeyModule.ShowKeyTab) {
        selectedTab.postValue(tab)
    }

}
