package cash.p.terminal.core

import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Parcelable
import android.provider.DocumentsContract
import android.widget.ImageView
import androidx.annotation.IdRes
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.Clipboard
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import cash.p.terminal.R
import cash.p.terminal.modules.backuplocal.BackupLocalModule
import cash.p.terminal.modules.backuplocal.fullbackup.BackupFileValidator
import cash.p.terminal.modules.main.MainActivity
import cash.p.terminal.modules.market.topplatforms.Platform
import cash.p.terminal.modules.premium.about.AboutPremiumFragment
import cash.p.terminal.navigation.slideFromBottomForResult
import cash.p.terminal.premium.domain.usecase.CheckPremiumUseCase
import cash.p.terminal.ui_compose.components.ImageSource
import cash.p.terminal.wallet.entities.FullCoin
import cash.p.terminal.wallet.models.CoinCategory
import cash.p.terminal.wallet.models.CoinInvestment
import cash.p.terminal.wallet.models.CoinTreasury
import coil3.load
import io.horizontalsystems.ethereumkit.core.toRawHexString
import io.horizontalsystems.hdwalletkit.Language
import io.horizontalsystems.hodler.LockTimeInterval
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.ParametersDefinition
import org.koin.java.KoinJavaComponent.inject
import java.io.File
import java.math.BigDecimal
import java.util.Locale
import java.util.Optional

import androidx.compose.ui.focus.FocusManager
import kotlinx.coroutines.CoroutineScope

/**
 * Clears focus (dismissing keyboard) and launches [action] after a brief delay.
 * Prevents a Compose bug where .imePadding() gets stuck with stale IME insets
 * when the keyboard is dismissed during fragment navigation.
 */
fun CoroutineScope.launchAfterClearingFocus(
    focusManager: FocusManager,
    action: () -> Unit
) {
    focusManager.clearFocus()
    launch {
        delay(100)
        action()
    }
}

fun String.orHide(hidden: Boolean, hideValue: String = "*****"): String =
    if (hidden) hideValue else this

val <T> Optional<T>.orNull: T?
    get() = when {
        isPresent -> get()
        else -> null
    }

val Platform.iconUrl: String
    get() = "https://p.cash/storage/blockchains/$uid.png"

val String.coinIconUrl: String
    get() = "https://cdn.blocksdecoded.com/coin-icons/32px/$this@3x.png"

val String.fiatIconUrl: String
    get() = "https://cdn.blocksdecoded.com/fiat-icons/$this@3x.png"

val CoinCategory.imageUrl: String
    get() = "https://cdn.blocksdecoded.com/category-icons/$uid@3x.png"

val CoinInvestment.Fund.logoUrl: String
    get() = "https://cdn.blocksdecoded.com/fund-icons/$uid@3x.png"

val CoinTreasury.logoUrl: String
    get() = "https://cdn.blocksdecoded.com/treasury-icons/$fundUid@3x.png"

fun List<FullCoin>.sortedByFilter(filter: String): List<FullCoin> {
    val baseComparator = compareBy<FullCoin> {
        it.coin.priority ?: Int.MAX_VALUE
    }.thenBy {
        it.coin.marketCapRank ?: Int.MAX_VALUE
    }.thenBy {
        it.coin.name.lowercase(Locale.ENGLISH)
    }
    val comparator = if (filter.isNotBlank()) {
        val lowercasedFilter = filter.lowercase()
        compareByDescending<FullCoin> {
            it.coin.code.lowercase() == lowercasedFilter
        }.thenByDescending {
            it.coin.code.lowercase().startsWith(lowercasedFilter)
        }.thenByDescending {
            it.coin.name.lowercase().startsWith(lowercasedFilter)
        }.thenComparing(baseComparator)
    } else {
        baseComparator
    }

    return sortedWith(comparator)
}

val Language.displayNameStringRes: Int
    get() = when (this) {
        Language.English -> R.string.Language_English
        Language.Japanese -> R.string.Language_Japanese
        Language.Korean -> R.string.Language_Korean
        Language.Spanish -> R.string.Language_Spanish
        Language.SimplifiedChinese -> R.string.Language_SimplifiedChinese
        Language.TraditionalChinese -> R.string.Language_TraditionalChinese
        Language.French -> R.string.Language_French
        Language.Italian -> R.string.Language_Italian
        Language.Czech -> R.string.Language_Czech
        Language.Portuguese -> R.string.Language_Portuguese
    }

// ImageView

fun ImageView.setRemoteImage(url: String, placeholder: Int? = R.drawable.ic_placeholder) {
    load(url) {
        if (placeholder != null) {
            error(placeholder)
        }
    }
}

fun ImageView.setImage(imageSource: ImageSource) {
    when (imageSource) {
        is ImageSource.Local -> setImageResource(imageSource.resId)
        is ImageSource.Remote -> setRemoteImage(imageSource.url, imageSource.placeholder)
    }
}

// String

fun String.hexToByteArray(): ByteArray {
    return ByteArray(this.length / 2) {
        this.substring(it * 2, it * 2 + 2).toInt(16).toByte()
    }
}

// ByteArray

fun ByteArray.toRawHexString(): String {
    return this.joinToString(separator = "") {
        it.toInt().and(0xff).toString(16).padStart(2, '0')
    }
}

fun ByteArray?.to0xHexString(): String {
    val rawHex = this?.toRawHexString() ?: return ""
    return "0x$rawHex"
}

// Intent & Parcelable Enum
fun Intent.putParcelableExtra(key: String, value: Parcelable) {
    putExtra(key, value)
}

fun LockTimeInterval?.stringResId(): Int {
    return when (this) {
        LockTimeInterval.hour -> R.string.Send_LockTime_Hour
        LockTimeInterval.month -> R.string.Send_LockTime_Month
        LockTimeInterval.halfYear -> R.string.Send_LockTime_HalfYear
        LockTimeInterval.year -> R.string.Send_LockTime_Year
        null -> R.string.Send_LockTime_Off
    }
}

//Compose Animated Navigation

fun NavGraphBuilder.composablePage(
    route: String,
    arguments: List<NamedNavArgument> = emptyList(),
    content: @Composable AnimatedVisibilityScope.(NavBackStackEntry) -> Unit,
) {
    composable(
        route = route,
        arguments = arguments,
        enterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(300)
            )
        },
        popExitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(300)
            )
        },
        popEnterTransition = { null },
        content = content
    )
}

fun NavGraphBuilder.composablePopup(
    route: String,
    content: @Composable AnimatedVisibilityScope.(NavBackStackEntry) -> Unit
) {
    composable(
        route,
        enterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Up,
                animationSpec = tween(250)
            )
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(250)) +
                    slideOutOfContainer(
                        AnimatedContentTransitionScope.SlideDirection.Down,
                        animationSpec = tween(250)
                    )
        },
        content = content
    )
}

inline fun <reified T : Any> NavGraphBuilder.composablePage(
    noinline content: @Composable AnimatedVisibilityScope.(NavBackStackEntry) -> Unit,
) {
    composable<T>(
        enterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(300)
            )
        },
        popExitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(300)
            )
        },
        popEnterTransition = { null },
        content = content
    )
}

inline fun <reified T : Any> NavGraphBuilder.composablePopup(
    noinline content: @Composable AnimatedVisibilityScope.(NavBackStackEntry) -> Unit
) {
    composable<T>(
        enterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Up,
                animationSpec = tween(250)
            )
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(250)) +
                    slideOutOfContainer(
                        AnimatedContentTransitionScope.SlideDirection.Down,
                        animationSpec = tween(250)
                    )
        },
        content = content
    )
}

suspend fun <T> retryWhen(
    times: Int,
    predicate: suspend (cause: Throwable) -> Boolean,
    block: suspend () -> T
): T {
    repeat(times - 1) {
        try {
            return block()
        } catch (e: Throwable) {
            if (!predicate(e)) {
                throw e
            }
        }
        delay(1000)
    }
    return block()
}

fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * Safely opens an InputStream from a content URI, handling virtual documents
 * (e.g., files from Google Drive that don't exist locally).
 *
 * Virtual documents cannot be opened with openInputStream() directly and require
 * openTypedAssetFileDescriptor() to stream the content.
 */
fun ContentResolver.openInputStreamSafe(uri: Uri): java.io.InputStream? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isVirtualDocument(uri)) {
        openTypedAssetFileDescriptor(uri, "*/*", null)
            ?.createInputStream()
    } else {
        openInputStream(uri)
    }
}

private fun ContentResolver.isVirtualDocument(uri: Uri): Boolean {
    if (!DocumentsContract.isDocumentUri(App.instance, uri)) return false

    return try {
        val cursor = query(
            uri,
            arrayOf(DocumentsContract.Document.COLUMN_FLAGS),
            null, null, null
        )
        cursor?.use {
            if (it.moveToFirst()) {
                val flags = it.getInt(0)
                flags and DocumentsContract.Document.FLAG_VIRTUAL_DOCUMENT != 0
            } else false
        } ?: false
    } catch (_: Exception) {
        false
    }
}

fun String?.extractBigDecimal(): BigDecimal? =
    if (this == null) {
        null
    } else {
        "\\d+\\.\\d+".toRegex().find(this)?.value?.toBigDecimal()
    }

inline fun <reified T> getKoinInstance(): T {
    return object : KoinComponent {
        val value: T by inject()
    }.value
}

inline fun <reified T> getKoinInstance(noinline parameters: ParametersDefinition): T {
    return object : KoinComponent {
        val value: T by inject(parameters = parameters)
    }.value
}

inline fun <reified T : Enum<T>> Enum.Companion.valueOrDefault(index: Int, default: T): T {
    return enumValues<T>().getOrNull(index) ?: default
}

fun String.splitToAddresses(): List<String> {
    return split(",")
        .map { it.split("\n") }.flatten()
        .map { it.replace(Regex(":\\d+"), "") }
        .map { it.trim() }
        .filter { it.isNotEmpty() }
}

/**
 * Normalizes a private key byte array to exactly [size] bytes.
 * BigInteger.toByteArray() may return fewer bytes (leading zeros stripped)
 * or more bytes (sign byte prepended). This ensures a fixed-size output.
 */
fun ByteArray.toFixedSize(size: Int): ByteArray = when {
    this.size > size -> copyOfRange(this.size - size, this.size)
    this.size < size -> ByteArray(size - this.size) + this
    else -> this
}

inline fun <T> tryOrNull(block: () -> T): T? {
    return try {
        block()
    } catch (_: Throwable) {
        null
    }
}

fun writeBackupToTempFile(data: ByteArray): String {
    val file = File.createTempFile("backup_", ".tmp", App.instance.cacheDir)
    file.writeBytes(data)
    return file.absolutePath
}

fun validateAndSaveBackup(bytes: ByteArray): String {
    val validator = BackupFileValidator()
    if (BackupLocalModule.BackupV4Binary.isBinaryFormat(bytes)) {
        validator.validateBinary(bytes)
    } else {
        validator.validate(String(bytes, Charsets.UTF_8))
    }
    return writeBackupToTempFile(bytes)
}

fun Context.hasNFC(): Boolean {
    val pm = getSystemService(Context.NFC_SERVICE) as android.nfc.NfcManager
    return pm.defaultAdapter?.isEnabled == true
}

fun NavController.premiumAction(block: () -> Unit) {
    val checkPremiumUseCase: CheckPremiumUseCase by inject(CheckPremiumUseCase::class.java)
    if (checkPremiumUseCase.getPremiumType().isPremium()) {
        block.invoke()
    } else {
        slideFromBottomForResult<AboutPremiumFragment.Result>(
            R.id.aboutPremiumFragment,
            AboutPremiumFragment.CloseOnPremiumInput()
        ) {
            val backStackEntry = currentBackStackEntry
            if (backStackEntry != null) {
                backStackEntry.lifecycleScope.launch {
                    val job = this
                    val observer = object : DefaultLifecycleObserver {
                        override fun onResume(owner: LifecycleOwner) {
                            super.onResume(owner)
                            backStackEntry.lifecycle.removeObserver(this)
                            job.cancel()

                            if (backStackEntry.lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED)) {
                                block.invoke()
                            }
                        }

                        override fun onDestroy(owner: LifecycleOwner) {
                            super.onDestroy(owner)
                            backStackEntry.lifecycle.removeObserver(this)
                            job.cancel()
                        }
                    }

                    if (backStackEntry.lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED)) {
                        backStackEntry.lifecycle.addObserver(observer)
                    } else {
                        job.cancel()
                    }
                }
            } else {
                block.invoke()
            }
        }
    }
}

@Composable
inline fun <reified VM : ViewModel> rememberViewModelFromGraph(
    navController: NavController,
    @IdRes destinationId: Int,
    factory: ViewModelProvider.Factory? = null,
): VM? {
    val viewModelStoreOwner = remember(navController.currentBackStackEntry) {
        tryOrNull {
            navController.getBackStackEntry(destinationId)
        }
    } ?: return null

    return viewModel<VM>(
        viewModelStoreOwner = viewModelStoreOwner,
        factory = factory
    )
}

suspend fun Clipboard.getText(): String? {
    val clip = this.getClipEntry()?.clipData
    return if (clip != null && clip.itemCount > 0) {
        val item = clip.getItemAt(0)
        item.text.toString().takeIf { it.isNotBlank() }
    } else {
        null
    }
}

/**
 * Restarts the app by launching MainActivity with clear flags.
 * Use this as a fallback when navigation state is corrupted after process death.
 */
fun Activity.restartMain() {
    val intent = Intent(this, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }
    startActivity(intent)
}
