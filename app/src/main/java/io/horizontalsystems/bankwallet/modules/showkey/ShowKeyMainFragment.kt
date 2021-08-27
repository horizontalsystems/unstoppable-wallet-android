package io.horizontalsystems.bankwallet.modules.showkey

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.material.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.navGraphViewModels
import androidx.viewpager2.widget.ViewPager2
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.modules.showkey.ShowKeyModule.ShowKeyTab
import io.horizontalsystems.bankwallet.modules.showkey.tabs.ShowKeyTabsAdapter
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.Grey
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.core.findNavController
import kotlinx.android.synthetic.main.fragment_show_key_main.*
import kotlinx.android.synthetic.main.fragment_show_key_main.tabsCompose
import kotlinx.android.synthetic.main.fragment_show_key_main.toolbar
import kotlinx.android.synthetic.main.fragment_show_key_main.viewPager

class ShowKeyMainFragment : BaseFragment() {
    private val viewModel by navGraphViewModels<ShowKeyViewModel>(R.id.showKeyIntroFragment)
    private val showKeyTabs = listOf(ShowKeyTab.MnemonicPhrase, ShowKeyTab.PrivateKey)

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
            showKeyTabs,
            viewModel.words,
            viewModel.passphrase,
            viewModel.privateKeys,
            childFragmentManager,
            viewLifecycleOwner.lifecycle
        )

        viewPager.isUserInputEnabled = false

        buttonCloseCompose.setViewCompositionStrategy(
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

        setTabs(viewPager)
    }

    private fun setTabs(viewPager: ViewPager2) {
        val tabs = showKeyTabs.map { getString(it.title) }
        tabsCompose.setContent {
            var tabIndex by remember { mutableStateOf(0) }
            ComposeAppTheme {
                TabRow(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp),
                    selectedTabIndex = tabIndex,
                    backgroundColor = ComposeAppTheme.colors.tyler,
                    contentColor = ComposeAppTheme.colors.tyler,
                    indicator = @Composable { tabPositions ->
                        TabRowDefaults.Indicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[tabIndex]),
                            color = ComposeAppTheme.colors.jacob
                        )
                    }
                ) {
                    tabs.forEachIndexed { index, tab ->
                        Tab(
                            selected = tabIndex == index,
                            onClick = {
                                tabIndex = index
                                viewPager.currentItem = index
                            },
                            text = {
                                ProvideTextStyle(
                                    ComposeAppTheme.typography.subhead1.copy(
                                        textAlign = TextAlign.Center
                                    )
                                ) {
                                    Text(
                                        text = tab,
                                        color = if (tabIndex == index) ComposeAppTheme.colors.oz else Grey
                                    )
                                }
                            })
                    }
                }

            }
        }
    }

}
