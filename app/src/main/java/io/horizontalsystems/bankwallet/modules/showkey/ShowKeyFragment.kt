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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.BitcoinCashCoinType
import io.horizontalsystems.bankwallet.entities.BtcBlockchain
import io.horizontalsystems.bankwallet.modules.evmfee.ButtonsGroupWithShade
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.bankwallet.ui.extensions.ConfirmationDialog
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.getNavigationResult
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.pin.PinInteractionType
import io.horizontalsystems.pin.PinModule
import kotlinx.coroutines.launch

class ShowKeyFragment : BaseFragment() {

    private val viewModel by viewModels<ShowKeyViewModel> {
        ShowKeyModule.Factory(arguments?.getParcelable(ShowKeyModule.ACCOUNT)!!)
    }

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
                    viewModel,
                    findNavController(),
                    { key -> showPrivateKeyCopyWarning(key) },
                    { subscribeForPinResult() }
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

    private fun subscribeForPinResult() {
        getNavigationResult(PinModule.requestKey) { bundle ->
            val resultType = bundle.getParcelable<PinInteractionType>(PinModule.requestType)
            val resultCode = bundle.getInt(PinModule.requestResult)

            if (resultType == PinInteractionType.UNLOCK && resultCode == PinModule.RESULT_OK) {
                viewModel.showKey()
            }
        }
    }
}

@Composable
private fun ShowKeyIntroScreen(
    viewModel: ShowKeyViewModel,
    navController: NavController,
    showKeyWarning: (String) -> Unit,
    subscribeForPinResult: () -> Unit,
) {

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
                            subscribeForPinResult()
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

@OptIn(ExperimentalPagerApi::class)
@Composable
private fun KeyTabs(viewModel: ShowKeyViewModel, showKeyWarning: (String) -> Unit) {
    val tabs = listOf(
        R.string.ShowKey_TabMnemonicPhrase,
        R.string.ShowKey_TabPrivateKey,
        R.string.ShowKey_TabPublicKeys
    )
    val pagerState = rememberPagerState(initialPage = 0)
    val selectedTabIndex = pagerState.currentPage
    val coroutineScope = rememberCoroutineScope()
    val selectedTab = tabs[selectedTabIndex]

    val tabItems = tabs.map {
        TabItem(stringResource(it), it == selectedTab, it)
    }

    ScrollableTabs(tabItems) { item ->
        val selectedIndex = tabs.indexOf(item)
        coroutineScope.launch {
            pagerState.scrollToPage(selectedIndex)
        }
    }

    HorizontalPager(
        count = 3,
        state = pagerState,
        userScrollEnabled = false,
        verticalAlignment = Alignment.Top,
    ) { index ->
        when (index) {
            0 -> {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Spacer(Modifier.height(12.dp))
                    SeedPhraseList(viewModel.wordsNumbered)
                    PassphraseCell(viewModel.passphrase)
                }
            }
            1 -> {
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
            else -> {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Spacer(Modifier.height(24.dp))
                    PublicKeys(viewModel)
                }
            }
        }
    }
}

@Composable
private fun PublicKeys(viewModel: ShowKeyViewModel) {
    val localView = LocalView.current
    C1(
        text = BtcBlockchain.Bitcoin.title.uppercase(),
        modifier = Modifier.padding(horizontal = 16.dp)
    )
    Spacer(Modifier.height(13.dp))
    CellSingleLineLawrenceSection(
        listOf(
            {
                KeyCell(stringResource(R.string.ShowKey_Bip44)) {
                    copy(viewModel.bitcoinPublicKeys(AccountType.Derivation.bip44), localView)
                }
            },
            {
                KeyCell(stringResource(R.string.ShowKey_Bip49)) {
                    copy(viewModel.bitcoinPublicKeys(AccountType.Derivation.bip49), localView)
                }
            },
            {
                KeyCell(stringResource(R.string.ShowKey_Bip84)) {
                    copy(viewModel.bitcoinPublicKeys(AccountType.Derivation.bip84), localView)
                }
            },
        )
    )
    Spacer(Modifier.height(36.dp))
    C1(
        text = BtcBlockchain.BitcoinCash.title.uppercase(),
        modifier = Modifier.padding(horizontal = 16.dp)
    )
    Spacer(Modifier.height(13.dp))
    CellSingleLineLawrenceSection(
        listOf(
            {
                KeyCell(stringResource(R.string.CoinSettings_BitcoinCashCoinType_Type0_Title)) {
                    copy(viewModel.bitcoinCashPublicKeys(BitcoinCashCoinType.type0), localView)
                }
            },
            {
                KeyCell(stringResource(R.string.CoinSettings_BitcoinCashCoinType_Type145_Title)) {
                    copy(viewModel.bitcoinCashPublicKeys(BitcoinCashCoinType.type145), localView)
                }
            },
        )
    )
    Spacer(Modifier.height(36.dp))
    C1(
        text = BtcBlockchain.Litecoin.title.uppercase(),
        modifier = Modifier.padding(horizontal = 16.dp)
    )
    Spacer(Modifier.height(13.dp))
    CellSingleLineLawrenceSection(
        listOf(
            {
                KeyCell(stringResource(R.string.ShowKey_Bip44)) {
                    copy(viewModel.litecoinPublicKeys(AccountType.Derivation.bip44), localView)
                }
            },
            {
                KeyCell(stringResource(R.string.ShowKey_Bip49)) {
                    copy(viewModel.litecoinPublicKeys(AccountType.Derivation.bip49), localView)
                }
            },
            {
                KeyCell(stringResource(R.string.ShowKey_Bip84)) {
                    copy(viewModel.litecoinPublicKeys(AccountType.Derivation.bip84), localView)
                }
            },
        )
    )
    Spacer(Modifier.height(36.dp))
    C1(
        text = BtcBlockchain.Dash.title.uppercase(),
        modifier = Modifier.padding(horizontal = 16.dp)
    )
    Spacer(Modifier.height(13.dp))
    CellSingleLineLawrenceSection(
        listOf {
            KeyCell(stringResource(R.string.ShowKey_TabPublicKeys)) {
                copy(viewModel.dashKeys(), localView)
            }
        }
    )
    Spacer(Modifier.height(60.dp))
}

private fun copy(publicKeys: String?, localView: View) {
    if (publicKeys != null) {
        TextHelper.copyText(publicKeys)
        HudHelper.showSuccessMessage(localView, R.string.Hud_Text_Copied)
    } else {
        HudHelper.showErrorMessage(localView, R.string.Error)
    }
}

@Composable
private fun KeyCell(title: String, onCopy: () -> Unit) {
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        B2(
            text = title,
            modifier = Modifier.padding(end = 16.dp)
        )
        Spacer(Modifier.weight(1f))
        ButtonSecondaryCircle(
            icon = R.drawable.ic_copy_20,
            onClick = onCopy
        )
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
                    D1(
                        text = stringResource(R.string.ShowKey_Passphrase),
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
            C2(
                text = viewModel.ethereumPrivateKey,
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
        A1(
            text = "${item.number}.",
            modifier = Modifier.width(32.dp),
        )
        A2(item.word)
    }
    if (item.number.mod(portion) == 0) {
        Spacer(Modifier.height(28.dp))
    }
}

@Preview
@Composable
private fun PublicKeys_Preview() {
    ComposeAppTheme {
        Column {
            C1(
                text = BtcBlockchain.Litecoin.title.uppercase(),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(Modifier.height(13.dp))
            CellSingleLineLawrenceSection(
                listOf(
                    { KeyCell(stringResource(R.string.ShowKey_Bip44)) {} },
                    { KeyCell(stringResource(R.string.ShowKey_Bip49)) {} },
                    { KeyCell(stringResource(R.string.ShowKey_Bip84)) {} },
                )
            )
        }
    }
}
