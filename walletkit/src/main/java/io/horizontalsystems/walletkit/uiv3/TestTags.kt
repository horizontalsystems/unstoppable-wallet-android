package io.horizontalsystems.walletkit.uiv3

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId

/**
 * Test tags the Maestro UI tests in `maestro/` select by (`id:`). Renaming or removing one
 * breaks those tests, so update the flows in the same change. Names are `<screen>_<element>`
 * in snake_case; a suffix is added only where a test has to pick one item of a list.
 */
object TestTags {
    const val INTRO_NEXT = "intro_next"

    fun tab(name: String) = "tab_${name.lowercase()}"

    const val ONBOARDING_NEW_WALLET = "onboarding_new_wallet"
    const val ONBOARDING_EXISTING_WALLET = "onboarding_existing_wallet"
    const val ONBOARDING_WATCH_WALLET = "onboarding_watch_wallet"

    fun termsCheck(index: Int) = "terms_check_$index"
    const val TERMS_NEXT = "terms_next"

    const val CREATE_TYPE_STANDARD = "create_type_standard"
    const val CREATE_TYPE_PASSKEY = "create_type_passkey"
    const val CREATE_NAME_INPUT = "create_name_input"
    const val CREATE_SUBMIT = "create_submit"

    const val IMPORT_RECOVERY_PHRASE = "import_recovery_phrase"
    const val IMPORT_PRIVATE_KEY = "import_private_key"
    const val IMPORT_PASSKEY = "import_passkey"
    const val IMPORT_BACKUP_FILE = "import_backup_file"

    const val RESTORE_PHRASE_INPUT = "restore_phrase_input"
    const val RESTORE_NEXT = "restore_next"
    const val RESTORE_ERROR = "restore_error"
    fun restoreChain(blockchainUid: String) = "restore_chain_$blockchainUid"
    const val RESTORE_SUBMIT = "restore_submit"

    fun selectorSwitch(index: Int) = "selector_switch_$index"
    const val SELECTOR_DONE = "selector_done"
}

/**
 * Exposes [androidx.compose.ui.platform.testTag]s as resource ids, which is how UI tests outside
 * the app see them. Set at the activity root, and again in every dialog and bottom sheet: those
 * open their own windows, which do not inherit it.
 */
@OptIn(ExperimentalComposeUiApi::class)
fun Modifier.exposeTestTags() = semantics { testTagsAsResourceId = true }
