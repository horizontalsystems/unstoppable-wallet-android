package io.horizontalsystems.walletkit.modules.welcome

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.horizontalsystems.designkit.components.ButtonVariant
import io.horizontalsystems.designkit.components.HSButton
import io.horizontalsystems.designkit.theme.ComposeAppTheme
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.stats.StatEvent
import io.horizontalsystems.walletkit.core.stats.StatPage
import io.horizontalsystems.walletkit.core.stats.stat
import io.horizontalsystems.walletkit.modules.createaccount.CreateAccountSheet
import io.horizontalsystems.walletkit.modules.importwallet.ImportWalletSheet
import io.horizontalsystems.walletkit.modules.manageaccounts.ManageAccountsModule
import io.horizontalsystems.walletkit.modules.nav3.EntryPage
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.ui.helpers.LinkHelper
import io.horizontalsystems.walletkit.uiv3.TestTags
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The first screen of a new install. It stays the app's entry until the
 * first wallet is created or imported, so it offers nothing but those two.
 */
@Composable
fun WelcomeScreen(navigation: HSNavigation) {
    // The welcome screen is the entry page's content, and the entry page shows
    // the main screen once a wallet exists
    val afterWalletAdded = ManageAccountsModule.Input(EntryPage::class, false)

    val context = LocalContext.current

    WelcomeContent(
        onCreate = {
            navigation.slideFromBottom(CreateAccountSheet(afterWalletAdded))
            stat(page = StatPage.Welcome, event = StatEvent.Open(StatPage.NewWallet))
        },
        onImport = {
            navigation.slideFromBottom(ImportWalletSheet(afterWalletAdded))
            stat(page = StatPage.Welcome, event = StatEvent.Open(StatPage.ImportWallet))
        },
        onTerms = {
            LinkHelper.openLinkInAppBrowser(context, App.appConfigProvider.termsOfUseLink)
            stat(page = StatPage.Welcome, event = StatEvent.Open(StatPage.Terms))
        },
        onPrivacy = {
            LinkHelper.openLinkInAppBrowser(context, App.appConfigProvider.privacyPolicyLink)
            stat(page = StatPage.Welcome, event = StatEvent.Open(StatPage.Privacy))
        },
    )
}

@Composable
private fun WelcomeContent(
    onCreate: () -> Unit,
    onImport: () -> Unit,
    onTerms: () -> Unit,
    onPrivacy: () -> Unit,
) {
    // With system animations turned off the screen is shown as it ends up
    val context = LocalContext.current
    val still = LocalInspectionMode.current || remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }

    // The screen is left for a page and returned to, and must not play its
    // entrance again each time
    var entrancePlayed by rememberSaveable { mutableStateOf(false) }
    val settled = remember { still || entrancePlayed }

    // The screen opens on the splash screen's picture: the same mark, at the
    // same size, in the centre of the same background. So the logo starts out
    // shown, and the splash gives way to it unnoticed.
    val logoAlpha = remember { Animatable(1f) }
    val logoScale = remember { Animatable(1f) }
    val logoRise = remember { Animatable(0f) }
    var shownLines by remember { mutableIntStateOf(if (settled) Int.MAX_VALUE else 0) }
    var tailShown by remember { mutableStateOf(settled) }
    var secondButtonShown by remember { mutableStateOf(settled) }
    var consentShown by remember { mutableStateOf(settled) }

    val titleLines = stringResource(R.string.Welcome_Title).lines()

    if (!settled) {
        LaunchedEffect(Unit) {
            var now = 0f
            suspend fun waitUntil(time: Float) {
                delay(((time - now) * 1000).toLong())
                now = time
            }

            waitUntil(WelcomeTimeline.LOGO_OUT)
            launch { logoAlpha.animateTo(0f, tween(450, easing = LeaveEasing)) }
            launch { logoScale.animateTo(0.92f, tween(450, easing = LeaveEasing)) }
            launch { logoRise.animateTo(14f, tween(450, easing = LeaveEasing)) }

            titleLines.indices.forEach { line ->
                waitUntil(WelcomeTimeline.FIRST_LINE + WelcomeTimeline.LINE_STEP * line)
                shownLines = line + 1
            }

            waitUntil(WelcomeTimeline.TAIL)
            tailShown = true
            entrancePlayed = true

            waitUntil(WelcomeTimeline.SECOND_BUTTON)
            secondButtonShown = true

            waitUntil(WelcomeTimeline.CONSENT)
            consentShown = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ComposeAppTheme.colors.tyler)
    ) {
        WelcomeGlow(
            animate = !still,
            startAt = if (settled) GLOW_SETTLED else 0f,
        )

        if (!settled) {
            Image(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(LogoSize)
                    .graphicsLayer {
                        alpha = logoAlpha.value
                        scaleX = logoScale.value
                        scaleY = logoScale.value
                        translationY = -logoRise.value.dp.toPx()
                    },
                painter = painterResource(R.drawable.ic_splash_logo),
                contentDescription = null,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            // The text sits low on the screen, and scrolls when the screen is too
            // short for it at the user's font size
            BoxWithConstraints(Modifier.weight(1f)) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .heightIn(min = maxHeight)
                        .padding(horizontal = 32.dp)
                ) {
                    Spacer(Modifier.weight(7f))
                    titleLines.forEachIndexed { index, line ->
                        Reveal(shown = index < shownLines) {
                            Text(
                                text = line,
                                color = ComposeAppTheme.colors.leah,
                                // Tighter than the font's own line height, as the design sets it
                                style = ComposeAppTheme.typography.title2М.copy(lineHeight = 45.sp),
                            )
                        }
                    }
                    Reveal(shown = tailShown) {
                        Text(
                            modifier = Modifier.padding(top = 24.dp),
                            text = stringResource(R.string.Welcome_Subtitle),
                            color = ComposeAppTheme.colors.leah,
                            style = ComposeAppTheme.typography.title3,
                        )
                    }
                    Reveal(shown = tailShown) {
                        Text(
                            modifier = Modifier.padding(vertical = 16.dp),
                            text = stringResource(R.string.Welcome_Description),
                            color = ComposeAppTheme.colors.leah.copy(alpha = 0.5f),
                            style = ComposeAppTheme.typography.bodyR,
                        )
                    }
                    Spacer(Modifier.weight(1f))
                }
            }

            Box {
                val controlsAlpha by animateFloatAsState(
                    targetValue = if (tailShown) 1f else 0f,
                    animationSpec = tween(500),
                    label = "controls",
                )
                Column(Modifier.graphicsLayer { alpha = controlsAlpha }) {
                    Column(
                        modifier = Modifier.padding(start = 24.dp, top = 24.dp, end = 24.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Reveal(shown = tailShown, rise = 40.dp, blur = 0.dp) {
                            HSButton(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag(TestTags.INTRO_CREATE_WALLET),
                                variant = ButtonVariant.Primary,
                                title = stringResource(R.string.Welcome_CreateNewWallet),
                                onClick = onCreate,
                            )
                        }
                        Reveal(shown = secondButtonShown, rise = 40.dp, blur = 0.dp) {
                            HSButton(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag(TestTags.INTRO_ADD_EXISTING_WALLET),
                                variant = ButtonVariant.Secondary,
                                title = stringResource(R.string.Welcome_AddExistingWallet),
                                onClick = onImport,
                            )
                        }
                    }

                    Reveal(shown = consentShown, rise = 0.dp, blur = 0.dp) {
                        Text(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 32.dp, vertical = 8.dp)
                                .testTag(TestTags.INTRO_AGREEMENT),
                            text = consentText(onTerms, onPrivacy),
                            color = ComposeAppTheme.colors.leah,
                            style = ComposeAppTheme.typography.caption,
                            textAlign = TextAlign.Center,
                        )
                    }
                }

                if (!consentShown) {
                    // Hiding the buttons leaves them tappable, so this takes the
                    // taps. It stays until the consent line appears, since a tap
                    // on a button agrees to what that line says.
                    Box(
                        Modifier
                            .matchParentSize()
                            .pointerInput(Unit) {}
                    )
                }
            }
        }
    }
}

/**
 * Brings its content in: it fades in while rising by [rise] and coming into
 * focus from a blur of [blur].
 */
@Composable
private fun Reveal(
    shown: Boolean,
    rise: Dp = 28.dp,
    blur: Dp = 8.dp,
    content: @Composable () -> Unit,
) {
    val fade by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = tween(700, easing = RevealEasing),
        label = "fade",
    )
    val lift by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = tween(900, easing = RevealEasing),
        label = "rise",
    )
    val focus = if (blur > 0.dp && fade < 1f) {
        Modifier.blur(blur * (1f - fade), BlurredEdgeTreatment.Unbounded)
    } else {
        Modifier
    }
    // Until it starts to appear there is nothing for a screen reader to read
    val semantics = if (shown) Modifier else Modifier.clearAndSetSemantics { }

    Box(
        Modifier
            .graphicsLayer {
                alpha = fade
                translationY = (1f - lift) * rise.toPx()
            }
            .then(focus)
            .then(semantics)
    ) {
        content()
    }
}

/** The consent sentence with the two document names as links. */
@Composable
private fun consentText(onTerms: () -> Unit, onPrivacy: () -> Unit): AnnotatedString {
    val terms = stringResource(R.string.Welcome_TermsOfUse)
    val privacy = stringResource(R.string.Welcome_PrivacyPolicy)
    val sentence = stringResource(R.string.Welcome_Consent, terms, privacy)
    val linkStyle = TextLinkStyles(
        style = SpanStyle(
            fontWeight = ComposeAppTheme.typography.captionSB.fontWeight,
            textDecoration = TextDecoration.Underline,
        )
    )

    // A translation is free to put the two names in either order
    val links = listOf(terms to onTerms, privacy to onPrivacy)
        .map { (name, onClick) -> Triple(sentence.indexOf(name), name, onClick) }
        .filter { (start, _, _) -> start >= 0 }
        .sortedBy { (start, _, _) -> start }

    return buildAnnotatedString {
        var position = 0
        links.forEach { (start, name, onClick) ->
            if (start < position) return@forEach
            append(sentence.substring(position, start))
            withLink(LinkAnnotation.Clickable(tag = name, styles = linkStyle) { onClick() }) {
                append(name)
            }
            position = start + name.length
        }
        append(sentence.substring(position))
    }
}

// A fast start with a long soft landing, for what comes in
private val RevealEasing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)
private val LeaveEasing = CubicBezierEasing(0.4f, 0f, 1f, 1f)

/** When each part of the welcome screen appears, in seconds from its start. */
private object WelcomeTimeline {
    // The logo has no time to appear at: the screen opens with it in place,
    // where the splash screen left it
    const val LOGO_OUT = 2.10f
    const val FIRST_LINE = 2.45f
    const val LINE_STEP = 0.18f
    const val TAIL = 3.25f
    const val SECOND_BUTTON = 3.33f
    const val CONSENT = 3.55f
}

// The size the splash screen draws its icon at
private val LogoSize = 288.dp

@Preview
@Composable
private fun Preview_WelcomeScreen() {
    ComposeAppTheme {
        WelcomeContent(onCreate = {}, onImport = {}, onTerms = {}, onPrivacy = {})
    }
}
