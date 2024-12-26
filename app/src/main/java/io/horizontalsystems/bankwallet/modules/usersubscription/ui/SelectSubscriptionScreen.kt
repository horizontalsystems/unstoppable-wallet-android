package io.horizontalsystems.bankwallet.modules.usersubscription.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.TabRowDefaults
import androidx.compose.material.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.evmfee.ButtonsGroupWithShade
import io.horizontalsystems.bankwallet.modules.usersubscription.BuySubscriptionViewModel
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.RadialBackground
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.body_grey
import io.horizontalsystems.subscriptions.core.Subscription
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectSubscriptionScreen(
    navController: NavController,
    onCloseClick: () -> Unit
) {
    val viewModel = viewModel<BuySubscriptionViewModel> {
        BuySubscriptionViewModel()
    }

    val uiState = viewModel.uiState
    val subscriptions = uiState.subscriptions

    val coroutineScope = rememberCoroutineScope()
    val selectedTabIndex = remember { mutableStateOf(0) }
    val modalBottomSheetState =
        androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var isBottomSheetVisible by remember { mutableStateOf(false) }
    val scrollScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = selectedTabIndex.value) { 2 }

    Scaffold(
        backgroundColor = ComposeAppTheme.colors.tyler,
        topBar = {
            TitleCenteredTopBar(
                title = stringResource(R.string.Premium_Title),
                onCloseClick = onCloseClick
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            RadialBackground()
            Column {
                VSpacer(12.dp)
                body_grey(
                    text = stringResource(R.string.Premium_ChoosePlanForYou),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                VSpacer(24.dp)
                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(12.dp)),
                ) {
                    SubscriptionTabs(
                        subscriptions = subscriptions,
                        selectedTabIndex = selectedTabIndex,
                        onTabSelected = {
                            scrollScope.launch {
                                pagerState.scrollToPage(it)
                            }
                        }
                    )

                    Column(
                        modifier = Modifier
                            .padding(paddingValues)
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        if (subscriptions.isNotEmpty()) {
                            HorizontalPager(
                                state = pagerState,
                                userScrollEnabled = false
                            ) { page ->
                                PlanItems(subscriptions[page].actions)
                            }
                            VSpacer(32.dp)
                        }
                    }

                    ButtonsGroupWithShade {
                        Column(
                            modifier = Modifier.padding(horizontal = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            ButtonPrimaryCustomColor(
                                modifier = Modifier.fillMaxWidth(),
                                title = stringResource(R.string.Premium_TryForFree),
                                brush = yellowGradient,
                                onClick = {
                                    coroutineScope.launch {
                                        isBottomSheetVisible = true
                                        modalBottomSheetState.show()
                                    }
                                },
                            )
                            VSpacer(12.dp)
                            ColoredTextSecondaryButton(
                                title = stringResource(R.string.Premium_Restore),
                                onClick = {
                                    //
                                },
                                color = ComposeAppTheme.colors.leah
                            )
                        }
                    }
                }
            }
            if (isBottomSheetVisible) {
                SubscriptionBottomSheet(
                    modalBottomSheetState = modalBottomSheetState,
                    subscriptions = subscriptions,
                    selectedTabIndex = selectedTabIndex,
                    navController = navController,
                    hideBottomSheet = {
                        coroutineScope.launch {
                            modalBottomSheetState.hide()
                        }
                        isBottomSheetVisible = false
                    }
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun SubscriptionBottomSheet(
    modalBottomSheetState: SheetState,
    subscriptions: List<Subscription>,
    selectedTabIndex: MutableState<Int>,
    navController: NavController,
    hideBottomSheet: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = hideBottomSheet,
        sheetState = modalBottomSheetState,
        containerColor = ComposeAppTheme.colors.transparent
    ) {
        if (subscriptions.isNotEmpty()) {
            SelectSubscriptionBottomSheet(
                subscriptionId = subscriptions[selectedTabIndex.value].id,
                type = PremiumPlanType.entries[selectedTabIndex.value],
                onDismiss = hideBottomSheet,
                onSubscribeClick = { type ->
                    hideBottomSheet()
                    navController.navigate("premium_subscribed_page?type=${type.name}")
                }
            )
        }
    }
}

@Composable
private fun SubscriptionTabs(
    subscriptions: List<Subscription>,
    selectedTabIndex: MutableState<Int>,
    onTabSelected: (Int) -> Unit = {}
) {
    if (subscriptions.isNotEmpty()) {
        TabRow(
            selectedTabIndex = selectedTabIndex.value,
            backgroundColor = ComposeAppTheme.colors.transparent, // Dark background
            contentColor = Color(0xFFEDD716),
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    Modifier
                        .tabIndicatorOffset(tabPositions[selectedTabIndex.value])
                        .height(0.dp), // No indicator line
                    color = Color.Transparent
                )
            }
        ) {
            subscriptions.forEachIndexed { index, tab ->
                Tab(
                    selected = selectedTabIndex.value == index,
                    onClick = {
                        selectedTabIndex.value = index
                        onTabSelected(index)
                    },
                    modifier = Modifier.background(
                        brush =
                        if (selectedTabIndex.value == index) yellowGradient else steelBrush,
                    ),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .height(44.dp)
                            .padding(vertical = 8.dp, horizontal = 16.dp)
                    ) {
                        Icon(
                            painter = painterResource(if (index == 0) R.drawable.prem_star_yellow_16 else R.drawable.prem_crown_yellow_16),
                            contentDescription = null,
                            tint = if (selectedTabIndex.value == index) ComposeAppTheme.colors.dark else ComposeAppTheme.colors.jacob,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = tab.name,
                            color = if (selectedTabIndex.value == index) ComposeAppTheme.colors.dark else ComposeAppTheme.colors.grey,
                            style = ComposeAppTheme.typography.captionSB
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun SelectSubscriptionScreenPreview() {
    ComposeAppTheme {
        val ctx = LocalContext.current
        SelectSubscriptionScreen(navController = NavController(ctx)) {}
    }
}