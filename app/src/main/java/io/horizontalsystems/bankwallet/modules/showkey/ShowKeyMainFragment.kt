package io.horizontalsystems.bankwallet.modules.showkey

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.navigation.navGraphViewModels
import androidx.viewpager2.widget.ViewPager2
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.modules.showkey.ShowKeyModule.ShowKeyTab
import io.horizontalsystems.bankwallet.modules.showkey.tabs.ShowKeyTabsAdapter
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.TabItem
import io.horizontalsystems.bankwallet.ui.compose.components.Tabs
import io.horizontalsystems.core.findNavController
import kotlinx.android.synthetic.main.fragment_show_key_main.*

class ShowKeyMainFragment : BaseFragment() {
    private val viewModel by navGraphViewModels<ShowKeyViewModel>(R.id.showKeyIntroFragment)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
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

        viewPager.adapter = ShowKeyTabsAdapter(
            viewModel.showKeyTabs,
            viewModel.words,
            viewModel.passphrase,
            viewModel.privateKeys,
            childFragmentManager,
            viewLifecycleOwner.lifecycle
        )

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback(){
            override fun onPageSelected(position: Int) {
                setTabs(viewModel.showKeyTabs[position])
            }
        })

        buttonCloseCompose.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
        )

        tabsCompose.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
        )

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

        viewModel.selectedTab.observe(viewLifecycleOwner, { tab ->
            selectTab(tab)
        })

    }

    private fun selectTab(item: ShowKeyTab) {
        viewPager.currentItem = viewModel.showKeyTabs.indexOf(item)
        setTabs(item)
    }

    private fun setTabs(selectedTab: ShowKeyTab) {
        val tabItems = viewModel.showKeyTabs.map { showKeyTab ->
             TabItem(getString(showKeyTab.title), showKeyTab == selectedTab, showKeyTab)
        }
        tabsCompose.setContent {
            ComposeAppTheme {
                Tabs(tabItems) { viewModel.onSelectTab(it) }
            }
        }
    }

}
