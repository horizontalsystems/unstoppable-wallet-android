package io.horizontalsystems.bankwallet.modules.showkey

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.widget.Toolbar
import androidx.navigation.navGraphViewModels
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.modules.showkey.ShowKeyModule.ShowKeyTab
import io.horizontalsystems.bankwallet.modules.showkey.tabs.ShowKeyTabsAdapter
import io.horizontalsystems.core.findNavController

class ShowKeyMainFragment : BaseFragment() {
    private val viewModel by navGraphViewModels<ShowKeyViewModel>(R.id.showKeyIntroFragment)
    private val showKeyTabs = listOf(ShowKeyTab.MnemonicPhrase, ShowKeyTab.PrivateKey)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        disallowScreenshot()
        return inflater.inflate(R.layout.fragment_show_key_main, container, false)
    }

    override fun onDestroyView() {
        allowScreenshot()
        super.onDestroyView()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<Toolbar>(R.id.toolbar).setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        val viewPager = view.findViewById<ViewPager2>(R.id.viewPager)
        val tabLayout = view.findViewById<TabLayout>(R.id.tabLayout)

        viewPager.adapter = ShowKeyTabsAdapter(showKeyTabs, viewModel.words, viewModel.passphrase, viewModel.privateKeys, childFragmentManager, viewLifecycleOwner.lifecycle)

        tabLayout.setSelectedTabIndicator(null)
        tabLayout.tabRippleColor = null

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.setCustomView(R.layout.view_show_key_tab)
            tab.setText(showKeyTabs[position].title)
        }.attach()

        view.findViewById<Button>(R.id.buttonClose).setOnClickListener {
            findNavController().popBackStack(R.id.showKeyIntroFragment, true)
        }
    }

}
