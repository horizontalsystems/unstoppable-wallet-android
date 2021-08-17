package io.horizontalsystems.bankwallet.modules.showkey

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.navGraphViewModels
import com.google.android.material.tabs.TabLayoutMediator
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.modules.showkey.ShowKeyModule.ShowKeyTab
import io.horizontalsystems.bankwallet.modules.showkey.tabs.ShowKeyTabsAdapter
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.core.findNavController
import kotlinx.android.synthetic.main.fragment_show_key_main.*
import kotlinx.android.synthetic.main.fragment_show_key_main.toolbar

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

        toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        viewPager.adapter = ShowKeyTabsAdapter(showKeyTabs, viewModel.words, viewModel.passphrase, viewModel.privateKeys, childFragmentManager, viewLifecycleOwner.lifecycle)

        tabLayout.setSelectedTabIndicator(null)
        tabLayout.tabRippleColor = null

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.setCustomView(R.layout.view_show_key_tab)
            tab.setText(showKeyTabs[position].title)
        }.attach()

        buttonCloseCompose.setContent {
            ComposeAppTheme {
                ButtonPrimaryYellow(
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 38.dp),
                    title = getString(R.string.ShowKey_ButtonClose),
                    onClick = {
                        findNavController().popBackStack(R.id.showKeyIntroFragment, true)
                    }
                )
            }
        }
    }

}
