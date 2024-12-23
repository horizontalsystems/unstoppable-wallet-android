package io.horizontalsystems.bankwallet.modules.premium

import androidx.annotation.StringRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.TweenSpec
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Scaffold
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.TabRowDefaults
import androidx.compose.material.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material.Text
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.evmfee.ButtonsGroupWithShade
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.Steel20
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryTransparent
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.HSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.RadialBackground
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.body_grey
import io.horizontalsystems.bankwallet.ui.compose.components.caption_grey
import io.horizontalsystems.bankwallet.ui.compose.components.headline1_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subhead1_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_jacob
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_remus
import io.horizontalsystems.bankwallet.ui.extensions.BottomSheetHeader
import kotlinx.coroutines.launch

enum class PremiumPlanType(@StringRes val titleResId: Int) {
    ProPlan(R.string.Premium_PlanPro),
    VipPlan(R.string.Premium_PlanVip);
}

val yellowGradient = Brush.horizontalGradient(
    colors = listOf(
        Color(0xFFFFD000),
        Color(0xFFFFA800),
    )
)

private val steelBrush = Brush.horizontalGradient(
    colors = listOf(Steel20, Steel20)
)

@Composable
fun SelectPremiumPlanScreen(
    navController: NavController,
    onCloseClick: () -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    val selectedTabIndex = remember { mutableStateOf(0) }
    val animationSpec = remember {
        Animatable(0f)
            .run {
                TweenSpec<Float>(durationMillis = 300)
            }
    }
    val modalBottomSheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        skipHalfExpanded = true,
        animationSpec = animationSpec
    )
    ModalBottomSheetLayout(
        sheetState = modalBottomSheetState,
        sheetBackgroundColor = ComposeAppTheme.colors.transparent,
        sheetContent = {
            SelectSubscriptionBottomSheet(
                type = PremiumPlanType.entries[selectedTabIndex.value],
                onDismiss = {
                    coroutineScope.launch {
                        modalBottomSheetState.hide()
                    }
                },
                onSubscribeClick = { type ->
                    coroutineScope.launch {
                        modalBottomSheetState.hide()
                        navController.navigate("premium_subscribed_page?type=${type.name}")
                    }
                }
            )
        },
    ) {
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
                    Column(
                        modifier = Modifier
                            .padding(paddingValues)
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    )
                    {
                        VSpacer(12.dp)
                        body_grey(
                            text = stringResource(R.string.Premium_ChoosePlanForYou),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                        VSpacer(24.dp)
                        PlanTabs(
                            selectedTabIndex = selectedTabIndex.value,
                            onTabChange = { index ->
                                selectedTabIndex.value = index
                            }
                        )
                        VSpacer(32.dp)
                    }

                    ButtonsGroupWithShade {
                        Column(
                            modifier= Modifier.padding(horizontal = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            ButtonPrimaryCustomColor(
                                modifier = Modifier.fillMaxWidth(),
                                title = stringResource(R.string.Premium_TryForFree),
                                brush = yellowGradient,
                                onClick = {
                                    coroutineScope.launch {
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
        }
    }
}

@Composable
fun SelectSubscriptionBottomSheet(
    type: PremiumPlanType,
    onDismiss: () -> Unit,
    onSubscribeClick: (PremiumPlanType) -> Unit
) {
    val selectedItem = remember { mutableIntStateOf(0) }
    BottomSheetHeader(
        iconPainter = painterResource(R.drawable.prem_star_yellow_16),
        iconTint = ColorFilter.tint(ComposeAppTheme.colors.jacob),
        title = stringResource(R.string.Premium_PlanPro),
        onCloseClick = onDismiss
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                SubscriptionOption(
                    title = stringResource(R.string.Premium_Annually),
                    price = "US$99 / year",
                    note = "($8.33 / month)",
                    isSelected = selectedItem.value == 0,
                    badgeText = "SAVE 35%",
                    onClick = {
                        selectedItem.value = 0
                    }
                )
                SubscriptionOption(
                    title = stringResource(R.string.Premium_Monthly),
                    price = "US$15 / month",
                    note = "",
                    isSelected = selectedItem.value == 1,
                    badgeText = null,
                    onClick = {
                        selectedItem.value = 1
                    }
                )
            }

            val bottomText = highlightText(
                text = stringResource(R.string.Premium_EnjoyFirst7DaysFree_Description),
                highlightPart = stringResource(R.string.Premium_EnjoyFirst7DaysFree),
                color = ComposeAppTheme.colors.remus
            )
            VSpacer(12.dp)
            Text(
                text = bottomText,
                color = ComposeAppTheme.colors.grey,
                style = ComposeAppTheme.typography.subhead2,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 12.dp)
            )
            VSpacer(24.dp)
            ButtonPrimaryYellow(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(R.string.Premium_Get7DaysFree),
                onClick = {
                    onSubscribeClick(type)
                }
            )
            VSpacer(12.dp)
            ButtonPrimaryTransparent(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                title = stringResource(R.string.Premium_AddPromoCode),
                onClick = {
                    //TODO
                }
            )
            VSpacer(36.dp)
        }
    }
}

@Composable
fun SubscriptionOption(
    title: String,
    price: String,
    note: String,
    isSelected: Boolean,
    badgeText: String?,
    onClick: () -> Unit
) {
    val borderColor =
        if (isSelected) ComposeAppTheme.colors.jacob else ComposeAppTheme.colors.steel20

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, shape = RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Title and badge
            Row(verticalAlignment = Alignment.CenterVertically) {
                headline1_leah(title)
                if (badgeText != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .background(
                                ComposeAppTheme.colors.remus,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = badgeText,
                            color = ComposeAppTheme.colors.claude,
                            style = ComposeAppTheme.typography.microSB,
                        )
                    }
                }
            }

            Row() {
                subhead2_jacob(price)
                if (note.isNotEmpty()) {
                    HSpacer(4.dp)
                    subhead2_remus(note)
                }
            }
        }
    }
}

@Composable
private fun PlanTabs(
    selectedTabIndex: Int,
    onTabChange: (Int) -> Unit
) {
    val tabs = PremiumPlanType.entries.toTypedArray()

    val pagerState = rememberPagerState(initialPage = selectedTabIndex) { tabs.size }
    val scrollScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp)),
    ) {
        TabRow(
            selectedTabIndex = selectedTabIndex,
            backgroundColor = ComposeAppTheme.colors.transparent, // Dark background
            contentColor = Color(0xFFEDD716),
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    Modifier
                        .tabIndicatorOffset(tabPositions[selectedTabIndex])
                        .height(0.dp), // No indicator line
                    color = Color.Transparent
                )
            }
        ) {
            tabs.forEachIndexed { index, tab ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = {
                        onTabChange(index)
                        scrollScope.launch {
                            pagerState.scrollToPage(index)
                        }
                    },
                    modifier = Modifier.background(
                        brush =
                        if (selectedTabIndex == index) yellowGradient else steelBrush,
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
                            tint = if (selectedTabIndex == index) ComposeAppTheme.colors.dark else ComposeAppTheme.colors.jacob,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(tab.titleResId),
                            color = if (selectedTabIndex == index) ComposeAppTheme.colors.dark else ComposeAppTheme.colors.grey,
                            style = ComposeAppTheme.typography.captionSB
                        )
                    }
                }
            }
        }

        HorizontalPager(
            state = pagerState,
            userScrollEnabled = false
        ) { page ->
            when (tabs[page]) {
                PremiumPlanType.ProPlan -> ProPlanItems()
                PremiumPlanType.VipPlan -> VipPlanItems()
            }
        }
    }
}

@Composable
private fun ProPlanItems() {
    Column {
        FeatureItem(
            icon = R.drawable.prem_search_discovery_24,
            title = R.string.Premium_UpgradeFeature_3,
            subtitle = R.string.Premium_UpgradeFeature_Description_3
        )
        Divider(color = ComposeAppTheme.colors.steel20)
        FeatureItem(
            icon = R.drawable.prem_ring_24,
            title = R.string.Premium_UpgradeFeature_4,
            subtitle = R.string.Premium_UpgradeFeature_Description_4
        )
        Divider(color = ComposeAppTheme.colors.steel20)
        FeatureItem(
            icon = R.drawable.prem_percent_24,
            title = R.string.Premium_UpgradeFeature_5,
            subtitle = R.string.Premium_UpgradeFeature_Description_5
        )
        Divider(color = ComposeAppTheme.colors.steel20)
        FeatureItem(
            icon = R.drawable.prem_outgoingraw_24,
            title = R.string.Premium_UpgradeFeature_6,
            subtitle = R.string.Premium_UpgradeFeature_Description_6
        )
        Divider(color = ComposeAppTheme.colors.steel20)
        FeatureItem(
            icon = R.drawable.prem_switch_wallet_24,
            title = R.string.Premium_UpgradeFeature_7,
            subtitle = R.string.Premium_UpgradeFeature_Description_7
        )
        Divider(color = ComposeAppTheme.colors.steel20)
        FeatureItem(
            icon = R.drawable.prem_shield_24,
            title = R.string.Premium_UpgradeFeature_8,
            subtitle = R.string.Premium_UpgradeFeature_Description_8
        )
        Divider(color = ComposeAppTheme.colors.steel20)
        FeatureItem(
            icon = R.drawable.prem_fraud_24,
            title = R.string.Premium_UpgradeFeature_9,
            subtitle = R.string.Premium_UpgradeFeature_Description_9
        )
    }
}

@Composable
private fun VipPlanItems() {
    Column {
        FeatureItem(
            icon = R.drawable.prem_vip_support_24,
            title = R.string.Premium_UpgradeFeature_1,
            subtitle = R.string.Premium_UpgradeFeature_Description_1,
            tint = ComposeAppTheme.colors.jacob
        )
        Divider(color = ComposeAppTheme.colors.steel20)
        FeatureItem(
            icon = R.drawable.prem_chat_support_24,
            title = R.string.Premium_UpgradeFeature_2,
            subtitle = R.string.Premium_UpgradeFeature_Description_2,
            tint = ComposeAppTheme.colors.jacob
        )
        Divider(color = ComposeAppTheme.colors.steel20)
        FeatureItem(
            icon = R.drawable.prem_search_discovery_24,
            title = R.string.Premium_UpgradeFeature_3,
            subtitle = R.string.Premium_UpgradeFeature_Description_3
        )
        Divider(color = ComposeAppTheme.colors.steel20)
        FeatureItem(
            icon = R.drawable.prem_ring_24,
            title = R.string.Premium_UpgradeFeature_4,
            subtitle = R.string.Premium_UpgradeFeature_Description_4
        )
        Divider(color = ComposeAppTheme.colors.steel20)
        FeatureItem(
            icon = R.drawable.prem_percent_24,
            title = R.string.Premium_UpgradeFeature_5,
            subtitle = R.string.Premium_UpgradeFeature_Description_5
        )
        Divider(color = ComposeAppTheme.colors.steel20)
        FeatureItem(
            icon = R.drawable.prem_outgoingraw_24,
            title = R.string.Premium_UpgradeFeature_6,
            subtitle = R.string.Premium_UpgradeFeature_Description_6
        )
        Divider(color = ComposeAppTheme.colors.steel20)
        FeatureItem(
            icon = R.drawable.prem_switch_wallet_24,
            title = R.string.Premium_UpgradeFeature_7,
            subtitle = R.string.Premium_UpgradeFeature_Description_7
        )
        Divider(color = ComposeAppTheme.colors.steel20)
        FeatureItem(
            icon = R.drawable.prem_shield_24,
            title = R.string.Premium_UpgradeFeature_8,
            subtitle = R.string.Premium_UpgradeFeature_Description_8
        )
        Divider(color = ComposeAppTheme.colors.steel20)
        FeatureItem(
            icon = R.drawable.prem_fraud_24,
            title = R.string.Premium_UpgradeFeature_9,
            subtitle = R.string.Premium_UpgradeFeature_Description_9
        )
    }
}

@Composable
private fun FeatureItem(
    icon: Int,
    title: Int,
    subtitle: Int,
    tint: Color = ComposeAppTheme.colors.leah
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(ComposeAppTheme.colors.steel10)
            .padding(vertical = 12.dp, horizontal = 16.dp)
    ) {
        Icon(
            painter = painterResource(icon),
            modifier = Modifier.size(24.dp),
            tint = tint,
            contentDescription = null
        )
        HSpacer(24.dp)
        Column {
            subhead1_leah(stringResource(title))
            caption_grey(stringResource(subtitle))
        }
    }
}

@Preview
@Composable
private fun SelectPremiumPlanScreenPreview() {
    ComposeAppTheme {
        val ctx = androidx.compose.ui.platform.LocalContext.current
        SelectPremiumPlanScreen(navController = NavController(ctx))
    }
}