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

    const val BALANCE_TOTAL = "balance_total"
    const val BALANCE_SCAN = "balance_scan"
    const val BALANCE_RECEIVE = "balance_receive"
    const val BALANCE_SEND = "balance_send"
    const val BALANCE_SWAP = "balance_swap"
    const val BALANCE_TRANSACTIONS = "balance_transactions"
    const val BALANCE_MANAGE_ACCOUNTS = "balance_manage_accounts"
    const val BALANCE_MANAGE_COINS = "balance_manage_coins"
    const val BALANCE_SYNC_INDICATOR = "balance_sync_indicator"
    fun balanceItem(coinCode: String) = "balance_item_$coinCode"
    fun balanceItemAmount(coinCode: String) = "balance_item_${coinCode}_amount"

    const val TOKEN_BALANCE = "token_balance"
    const val TOKEN_CHART = "token_chart"
    const val TOKEN_RECEIVE = "token_receive"
    const val TOKEN_SEND = "token_send"
    const val TOKEN_SWAP = "token_swap"

    const val RECEIVE_ADDRESS = "receive_address"
    const val RECEIVE_NETWORK = "receive_network"
    const val RECEIVE_FORMAT = "receive_format"
    const val RECEIVE_ADDRESS_TYPE = "receive_address_type"
    const val RECEIVE_SET_AMOUNT = "receive_set_amount"
    const val RECEIVE_SHARE = "receive_share"
    const val RECEIVE_COPY = "receive_copy"

    const val SEND_AMOUNT_INPUT = "send_amount_input"
    const val SEND_RECIPIENT_ROW = "send_recipient_row"
    const val SEND_NEXT = "send_next"
    const val SEND_ACTION = "send_action"

    // The address entry page is shared by send, swap recipient and other flows
    const val ENTER_ADDRESS_INPUT = "enter_address_input"
    const val ENTER_ADDRESS_SUBMIT = "enter_address_submit"

    const val CONFIRM_AMOUNT = "confirm_amount"
    const val CONFIRM_RECIPIENT = "confirm_recipient"
    const val CONFIRM_NETWORK_FEE = "confirm_network_fee"
    const val CONFIRM_SEND = "confirm_send"

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
