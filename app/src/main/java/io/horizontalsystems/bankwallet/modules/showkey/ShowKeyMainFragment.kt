package io.horizontalsystems.bankwallet.modules.showkey

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.navGraphViewModels
import com.google.android.material.tabs.TabLayoutMediator
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.modules.showkey.ShowKeyModule.ShowKeyTab
import io.horizontalsystems.bankwallet.modules.showkey.tabs.ShowKeyTabsAdapter
import io.horizontalsystems.core.findNavController
import kotlinx.android.synthetic.main.fragment_show_key_main.*

class ShowKeyMainFragment : BaseFragment() {
    private val viewModel by navGraphViewModels<ShowKeyViewModel>(R.id.showKeyIntroFragment)
    private val showKeyTabs = listOf(ShowKeyTab.MnemonicPhrase, ShowKeyTab.PrivateKey)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_show_key_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        viewPager.adapter = ShowKeyTabsAdapter(showKeyTabs, viewModel.words, viewModel.salt, viewModel.privateKeys, childFragmentManager, viewLifecycleOwner.lifecycle)

        tabLayout.setSelectedTabIndicator(null)
        tabLayout.tabRippleColor = null

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.setCustomView(R.layout.view_show_key_tab)
            tab.setText(showKeyTabs[position].title)
        }.attach()

        buttonClose.setOnClickListener {
            findNavController().popBackStack(R.id.showKeyIntroFragment, true)
        }
    }

}
