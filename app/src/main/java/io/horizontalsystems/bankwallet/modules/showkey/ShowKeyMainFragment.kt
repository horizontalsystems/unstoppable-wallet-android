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
import io.horizontalsystems.bankwallet.databinding.FragmentShowKeyMainBinding
import io.horizontalsystems.bankwallet.modules.showkey.ShowKeyModule.ShowKeyTab
import io.horizontalsystems.bankwallet.modules.showkey.tabs.ShowKeyTabsAdapter
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.TabItem
import io.horizontalsystems.bankwallet.ui.compose.components.Tabs
import io.horizontalsystems.core.findNavController

class ShowKeyMainFragment : BaseFragment() {
    private val viewModel by navGraphViewModels<ShowKeyViewModel>(R.id.showKeyIntroFragment)

    private var _binding: FragmentShowKeyMainBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentShowKeyMainBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        allowScreenshot()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        binding.viewPager.adapter = ShowKeyTabsAdapter(
            viewModel.showKeyTabs,
            viewModel.words,
            viewModel.passphrase,
            viewModel.privateKeys,
            childFragmentManager,
            viewLifecycleOwner.lifecycle
        )

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                setTabs(viewModel.showKeyTabs[position])
            }
        })

        binding.buttonCloseCompose.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
        )

        binding.tabsCompose.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
        )

        binding.buttonCloseCompose.setContent {
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
        binding.viewPager.currentItem = viewModel.showKeyTabs.indexOf(item)
        setTabs(item)
    }

    private fun setTabs(selectedTab: ShowKeyTab) {
        val tabItems = viewModel.showKeyTabs.map { showKeyTab ->
            TabItem(getString(showKeyTab.title), showKeyTab == selectedTab, showKeyTab)
        }
        binding.tabsCompose.setContent {
            ComposeAppTheme {
                Tabs(tabItems) { viewModel.onSelectTab(it) }
            }
        }
    }

}
