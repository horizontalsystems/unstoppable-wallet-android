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

    const val TX_EMPTY = "tx_empty"
    const val TX_WAITING_SYNC = "tx_waiting_sync"
    const val TX_FILTER = "tx_filter"
    // The record uid is the transaction hash on most chains
    fun txItem(uid: String) = "tx_item_${uid.take(8)}"

    const val SWAP_TOKEN_IN = "swap_token_in"
    const val SWAP_TOKEN_OUT = "swap_token_out"
    const val SWAP_AMOUNT_IN = "swap_amount_in"
    const val SWAP_AMOUNT_OUT = "swap_amount_out"
    const val SWAP_SWITCH = "swap_switch"
    const val SWAP_NEXT = "swap_next"

    const val SETTINGS_MANAGE_WALLETS = "settings_manage_wallets"
    const val SETTINGS_BLOCKCHAIN = "settings_blockchain"
    const val SETTINGS_SECURITY = "settings_security"
    const val SETTINGS_PRIVACY = "settings_privacy"
    const val SETTINGS_CONTACTS = "settings_contacts"
    const val SETTINGS_APP_SETTINGS = "settings_app_settings"
    const val SETTINGS_BACKUP_MANAGER = "settings_backup_manager"
    const val SETTINGS_ABOUT = "settings_about"
    const val SETTINGS_DEBUG_SIMULATE_FAIL_SWAP = "settings_debug_simulate_fail_swap"
    const val SETTINGS_DEBUG_SWAP_PROVIDER_NAME = "settings_debug_swap_provider_name"

    const val ABOUT_APP_STATUS = "about_app_status"
    const val ABOUT_TERMS = "about_terms"

    const val SECURITY_PASSCODE = "security_passcode"
    const val SECURITY_DISABLE_PASSCODE = "security_disable_passcode"
    const val SECURITY_BIOMETRICS = "security_biometrics"
    const val SECURITY_AUTOLOCK = "security_autolock"

    fun pinKey(digit: Int) = "pin_key_$digit"
    const val PIN_KEY_DELETE = "pin_key_delete"
    const val PIN_TITLE = "pin_title"
    const val PIN_ERROR = "pin_error"
    const val PIN_ATTEMPTS_LEFT = "pin_attempts_left"

    // Index over the list as shown: wallets first, then watch addresses
    fun accountRow(index: Int) = "account_row_$index"
    fun accountOptions(index: Int) = "account_options_$index"
    const val ACCOUNT_MANUAL_BACKUP = "account_manual_backup"
    const val ACCOUNT_LOCAL_BACKUP = "account_local_backup"
    const val ACCOUNT_UNLINK = "account_unlink"

    const val BACKUP_REVEAL = "backup_reveal"
    // Numbered as shown on screen, from 1
    fun backupWord(number: Int) = "backup_word_$number"
    const val BACKUP_VERIFY = "backup_verify"
    fun quizPosition(index: Int) = "quiz_position_$index"
    fun quizOption(word: String) = "quiz_option_$word"

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
