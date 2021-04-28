package io.horizontalsystems.bankwallet.modules.showkey.tabs

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import io.horizontalsystems.bankwallet.modules.showkey.ShowKeyModule
import io.horizontalsystems.bankwallet.modules.showkey.ShowKeyModule.ShowKeyTab

class ShowKeyTabsAdapter(
        private val showKeyTabs: List<ShowKeyTab>,
        private val words: List<String>,
        private val passphrase: String,
        private val privateKeys: List<ShowKeyModule.PrivateKey>,
        fm: FragmentManager,
        lifecycle: Lifecycle
) : FragmentStateAdapter(fm, lifecycle) {

    override fun getItemCount() = showKeyTabs.size

    override fun createFragment(position: Int): Fragment {
        return when (showKeyTabs[position]) {
            ShowKeyTab.MnemonicPhrase -> ShowWordsTab.getInstance(words, passphrase)
            ShowKeyTab.PrivateKey -> ShowPrivateKeyFragment.getInstance(privateKeys)
        }
    }

}
