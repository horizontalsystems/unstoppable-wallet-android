package io.horizontalsystems.bankwallet.modules.showkey

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.modules.evmfee.ButtonsGroupWithShade
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.bankwallet.ui.extensions.ConfirmationDialog
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.pin.PinInteractionType
import io.horizontalsystems.pin.PinModule
import kotlinx.coroutines.launch

class ShowKeyFragment : BaseFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )
            setContent {
                ShowKeyIntroScreen(
                    arguments?.getParcelable(ShowKeyModule.ACCOUNT)!!,
                    findNavController(),
                    { key -> showPrivateKeyCopyWarning(key) }
                )
            }
        }
    }

    private fun showPrivateKeyCopyWarning(key: String) {
        ConfirmationDialog.show(
            title = getString(R.string.ShowKey_PrivateKeyCopyWarning_Title),
            subtitle = getString(R.string.ShowKey_PrivateKeyCopyWarning_Subtitle),
            contentText = getString(R.string.ShowKey_PrivateKeyCopyWarning_Text),
            destructiveButtonTitle = getString(R.string.ShowKey_PrivateKeyCopyWarning_Proceed),
            actionButtonTitle = null,
            cancelButtonTitle = null,
            fragmentManager = childFragmentManager,
            listener = object : ConfirmationDialog.Listener {
                override fun onDestructiveButtonClick() {
                    TextHelper.copyText(key)
                    HudHelper.showSuccessMessage(requireView(), R.string.Hud_Text_Copied)
                }
            }
        )
    }
}

@Composable
private fun ShowKeyIntroScreen(
    account: Account,
    navController: NavController,
    showKeyWarning: (String) -> Unit,
    viewModel: ShowKeyViewModel = viewModel(factory = ShowKeyModule.Factory(account))
) {
    SubscribeForFragmentResult(navController, viewModel)

    ComposeAppTheme {
        Column(modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)) {
            AppBar(
                title = TranslatableString.ResString(R.string.ShowKey_Title),
                navigationIcon = {
                    HsIconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_back),
                            contentDescription = "back button",
                            tint = ComposeAppTheme.colors.jacob
                        )
                    }
                }
            )

            when (viewModel.viewState) {
                ShowKeyModule.ViewState.Warning -> {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        TextImportantWarning(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            text = stringResource(R.string.ShowKey_Description)
                        )
                    }
                    ActionButton(R.string.ShowKey_ButtonShow) {
                        if (viewModel.isPinSet) {
                            navController.slideFromRight(R.id.pinFragment, PinModule.forUnlock())
                        } else {
                            viewModel.showKey()
                        }
                    }
                }
                ShowKeyModule.ViewState.Key -> {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Top
                    ) {
                        KeyTabs(viewModel, showKeyWarning)
                    }
                    ActionButton(R.string.ShowKey_ButtonClose) { navController.popBackStack() }
                }
            }
        }
    }
}

@Composable
private fun SubscribeForFragmentResult(
    navController: NavController,
    viewModel: ShowKeyViewModel
) {
    navController.currentBackStackEntry?.savedStateHandle?.remove<Bundle>(PinModule.requestKey)
        ?.let { bundle ->
            val resultType = bundle.getParcelable<PinInteractionType>(PinModule.requestType)
            val resultCode = bundle.getInt(PinModule.requestResult)

            if (resultType == PinInteractionType.UNLOCK) {
                when (resultCode) {
                    PinModule.RESULT_OK -> {
                        viewModel.showKey()
                    }
                    PinModule.RESULT_CANCELLED -> {
                        // on cancel unlock
                    }
                }
            }
        }
}

@OptIn(ExperimentalPagerApi::class)
@Composable
private fun KeyTabs(viewModel: ShowKeyViewModel, showKeyWarning: (String) -> Unit) {
    val tabs = listOf(R.string.ShowKey_TabMnemonicPhrase, R.string.ShowKey_TabPrivateKey)
    val pagerState = rememberPagerState(initialPage = 0)
    val selectedTabIndex = pagerState.currentPage
    val coroutineScope = rememberCoroutineScope()
    val selectedTab = tabs[selectedTabIndex]

    val tabItems = tabs.map {
        TabItem(stringResource(it), it == selectedTab, it)
    }

    Tabs(tabItems) { item ->
        val selectedIndex = tabs.indexOf(item)
        coroutineScope.launch {
            pagerState.scrollToPage(selectedIndex)
        }
    }

    HorizontalPager(
        count = 2,
        state = pagerState,
        userScrollEnabled = false
    ) { index ->
        if (index == 0) {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Spacer(Modifier.height(12.dp))
                SeedPhraseList(viewModel.wordsNumbered)
                PassphraseCell(viewModel.passphrase)
            }
        } else {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Spacer(Modifier.height(12.dp))
                TextImportantWarning(
                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp),
                    text = stringResource(R.string.ShowKey_PrivateKey_Warning)
                )
                Spacer(Modifier.height(12.dp))
                PrivateKey(viewModel, showKeyWarning)
            }
        }
    }
}

@Composable
private fun PassphraseCell(passphrase: String) {
    val localView = LocalView.current
    if (passphrase.isNotBlank()) {
        Spacer(Modifier.height(27.dp))
        CellSingleLineLawrenceSection(
            listOf {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_key_phrase_20),
                        contentDescription = null,
                        tint = ComposeAppTheme.colors.grey
                    )
                    Text(
                        text = stringResource(R.string.ShowKey_Passphrase),
                        color = ComposeAppTheme.colors.grey,
                        style = ComposeAppTheme.typography.subhead2,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(Modifier.weight(1f))
                    ButtonSecondaryDefault(
                        title = passphrase,
                        onClick = {
                            TextHelper.copyText(passphrase)
                            HudHelper.showSuccessMessage(localView, R.string.Hud_Text_Copied)
                        }
                    )
                }
            })
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun PrivateKey(viewModel: ShowKeyViewModel, showKeyWarning: (String) -> Unit) {
    ButtonSecondary(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        buttonColors = SecondaryButtonDefaults.buttonColors(
            backgroundColor = ComposeAppTheme.colors.steel20,
            contentColor = ComposeAppTheme.colors.leah,
        ),
        contentPadding = PaddingValues(vertical = 6.dp, horizontal = 16.dp),
        content = {
            Text(
                viewModel.ethereumPrivateKey,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        },
        onClick = { showKeyWarning(viewModel.ethereumPrivateKey) }
    )
}

@Composable
private fun ActionButton(title: Int, onClick: () -> Unit) {
    ButtonsGroupWithShade {
        ButtonPrimaryYellow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 32.dp),
            title = stringResource(title),
            onClick = onClick,
        )
    }
}

@Composable
private fun SeedPhraseList(wordsNumbered: List<ShowKeyModule.WordNumbered>) {
    val portion = if (wordsNumbered.size == 12) 3 else 4
    val half = wordsNumbered.size / 2

    Row(Modifier.padding(horizontal = 16.dp)) {
        Column(Modifier.weight(1f)) {
            wordsNumbered.take(half).forEach { item ->
                SeedPhrase(item, portion)
            }
        }
        Column(Modifier.weight(1f)) {
            wordsNumbered.takeLast(half).forEach { item ->
                SeedPhrase(item, portion)
            }
        }
    }
}

@Composable
private fun SeedPhrase(item: ShowKeyModule.WordNumbered, portion: Int) {
    Row(Modifier.padding(bottom = 7.dp)) {
        Text(
            modifier = Modifier.width(32.dp),
            text = "${item.number}.",
            color = ComposeAppTheme.colors.grey,
            style = ComposeAppTheme.typography.headline2,
        )
        Text(
            text = item.word,
            color = ComposeAppTheme.colors.leah,
            style = ComposeAppTheme.typography.headline2,
        )
    }
    if (item.number.mod(portion) == 0) {
        Spacer(Modifier.height(28.dp))
    }
}
