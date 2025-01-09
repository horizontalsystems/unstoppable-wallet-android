package io.horizontalsystems.bankwallet.modules.usersubscription.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Scaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.evmfee.ButtonsGroupWithShade
import io.horizontalsystems.bankwallet.modules.usersubscription.BuySubscriptionModel.bigDescriptionStringRes
import io.horizontalsystems.bankwallet.modules.usersubscription.BuySubscriptionModel.iconRes
import io.horizontalsystems.bankwallet.modules.usersubscription.BuySubscriptionModel.titleStringRes
import io.horizontalsystems.bankwallet.modules.usersubscription.BuySubscriptionViewModel
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.RadialBackground
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.body_grey
import io.horizontalsystems.bankwallet.ui.compose.components.headline2_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.subscriptions.core.IPaidAction
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
    val plansModalBottomSheetState =
        androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val infoModalBottomSheetState =
        androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var isPlanSelectBottomSheetVisible by remember { mutableStateOf(false) }
    var isInfoBottomSheetVisible by remember { mutableStateOf(false) }
    val scrollScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = selectedTabIndex.value) { 2 }
    var infoBottomSheetAction: IPaidAction? = null

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
                        .padding(horizontal = 16.dp),
                ) {
                    SubscriptionTabs(
                        subscriptions = subscriptions,
                        selectedTabIndex = selectedTabIndex,
                        modifier = Modifier.clip(RoundedCornerShape(12.dp, 12.dp, 0.dp, 0.dp)),
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
                                userScrollEnabled = false,
                                modifier = Modifier.clip(RoundedCornerShape(0.dp, 0.dp, 12.dp, 12.dp))
                            ) { page ->
                                PlanItems(
                                    items = subscriptions[page].actions,
                                    onItemClick = { action ->
                                        infoBottomSheetAction = action
                                        coroutineScope.launch {
                                            isInfoBottomSheetVisible = true
                                            infoModalBottomSheetState.show()
                                        }
                                    }
                                )
                            }
                            VSpacer(32.dp)
                            headline2_leah(
                                text = stringResource(R.string.Premium_HighlyRatedSecurity),
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 32.dp)
                            )
                            VSpacer(24.dp)
                            Image(
                                painter = painterResource(id = R.drawable.security_rate_image),
                                contentDescription = null,
                                modifier = Modifier
                                    .height(112.dp)
                                    .fillMaxWidth()
                            )
                            subhead2_grey(
                                text = stringResource(R.string.Premium_ApprovedBy),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 32.dp, vertical = 16.dp),
                                textAlign = TextAlign.Center
                            )
                            Row(
                                modifier = Modifier.align(Alignment.CenterHorizontally),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.bitcoin_logo),
                                    contentDescription = null,
                                    tint = ComposeAppTheme.colors.leah
                                )
                                Icon(
                                    painter = painterResource(id = R.drawable.wallet_scrutiny_logo),
                                    contentDescription = null,
                                    tint = ComposeAppTheme.colors.leah
                                )
                                Icon(
                                    painter = painterResource(id = R.drawable.certik_logo),
                                    contentDescription = null,
                                    tint = ComposeAppTheme.colors.leah
                                )
                            }
                            VSpacer(24.dp)
                            subhead2_grey(
                                text = stringResource(R.string.Premium_WhatUsersSay),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 32.dp),
                                textAlign = TextAlign.Center
                            )
                            VSpacer(16.dp)
                            ReviewSliderBlock()
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
                                        isPlanSelectBottomSheetVisible = true
                                        plansModalBottomSheetState.show()
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
            if (isPlanSelectBottomSheetVisible) {
                val view = LocalView.current
                SubscriptionBottomSheet(
                    modalBottomSheetState = plansModalBottomSheetState,
                    subscriptions = subscriptions,
                    selectedTabIndex = selectedTabIndex,
                    navController = navController,
                    hideBottomSheet = {
                        coroutineScope.launch {
                            plansModalBottomSheetState.hide()
                        }
                        isPlanSelectBottomSheetVisible = false
                    },
                    onError = {
                        coroutineScope.launch {
                            plansModalBottomSheetState.hide()
                        }
                        isPlanSelectBottomSheetVisible = false
                        HudHelper.showErrorMessage(view, it.message ?: "Error")
                    }
                )
            }
            if (isInfoBottomSheetVisible) {
                infoBottomSheetAction?.let {
                    InfoBottomSheet(
                        icon = it.iconRes,
                        title = stringResource(it.titleStringRes),
                        description = stringResource(it.bigDescriptionStringRes),
                        bottomSheetState = infoModalBottomSheetState,
                        hideBottomSheet = {
                            coroutineScope.launch {
                                infoModalBottomSheetState.hide()
                            }
                            infoBottomSheetAction = null
                            isInfoBottomSheetVisible = false
                        }
                    )
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