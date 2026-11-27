package io.horizontalsystems.walletkit.uiv3

import androidx.compose.ui.platform.testTag
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import io.horizontalsystems.walletkit.core.App

/**
 * Test tags UI tests select by. The names are shared with the iOS app's `TestTags`: the same strings.
 * Names are `<screen>_<element>` in snake_case; a suffix is added only where a test has to pick one item
 * of a list. The rules are in `AGENTS.md`, "UI test tags".
 */
object TestTags {
    // The intro this named is gone. The shared flows still select it, so it
    // stays declared until they start from the welcome screen instead.
    const val INTRO_NEXT = "intro_next"

    const val INTRO_CREATE_WALLET = "intro_create_wallet"
    const val INTRO_ADD_EXISTING_WALLET = "intro_add_existing_wallet"
    // One text holding both links, Terms of Use and Privacy Policy
    const val INTRO_AGREEMENT = "intro_agreement"

    // The close button in a bottom sheet's header
    const val SHEET_CLOSE = "sheet_close"

    fun tab(name: String) = "tab_${name.lowercase()}"

    const val ONBOARDING_NEW_WALLET = "onboarding_new_wallet"
    const val ONBOARDING_EXISTING_WALLET = "onboarding_existing_wallet"
    const val ONBOARDING_WATCH_WALLET = "onboarding_watch_wallet"

    // The terms screen these named is gone. The shared flows still select
    // them, so they stay declared until the flows stop accepting terms.
    fun termsCheck(index: Int) = "terms_check_$index"
    const val TERMS_NEXT = "terms_next"

    const val CREATE_TYPE_STANDARD = "create_type_standard"
    const val CREATE_TYPE_PASSKEY = "create_type_passkey"
    const val CREATE_NAME_INPUT = "create_name_input"
    const val CREATE_NAME_RANDOM = "create_name_random"
    const val CREATE_NAME_CLEAR = "create_name_clear"
    const val CREATE_MNEMONIC_KIND = "create_mnemonic_kind"
    const val CREATE_PASSPHRASE_INPUT = "create_passphrase_input"
    const val CREATE_PASSPHRASE_EYE = "create_passphrase_eye"
    const val CREATE_PASSPHRASE_CONFIRM_INPUT = "create_passphrase_confirm_input"
    const val CREATE_PASSPHRASE_CONFIRM_EYE = "create_passphrase_confirm_eye"
    const val CREATE_ADVANCED_OPTIONS = "create_advanced_options"
    const val CREATE_SUBMIT = "create_submit"

    const val IMPORT_RECOVERY_PHRASE = "import_recovery_phrase"
    const val IMPORT_PRIVATE_KEY = "import_private_key"
    const val IMPORT_PASSKEY = "import_passkey"
    const val IMPORT_BACKUP_FILE = "import_backup_file"

    const val RESTORE_NAME_RANDOM = "restore_name_random"
    const val RESTORE_NAME_CLEAR = "restore_name_clear"
    const val RESTORE_KEY_NAME_RANDOM = "restore_key_name_random"
    const val RESTORE_KEY_NAME_CLEAR = "restore_key_name_clear"
    const val RESTORE_PHRASE_INPUT = "restore_phrase_input"
    const val RESTORE_WORDLIST = "restore_wordlist"
    const val RESTORE_NEXT = "restore_next"
    const val RESTORE_ERROR = "restore_error"
    const val RESTORE_ADVANCED_OPTIONS = "restore_advanced_options"
    fun restoreChain(blockchainUid: String) = "restore_chain_$blockchainUid"
    const val RESTORE_SUBMIT = "restore_submit"

    const val WATCH_NAME_RANDOM = "watch_name_random"
    const val WATCH_NAME_CLEAR = "watch_name_clear"
    const val WATCH_INPUT = "watch_input"
    // Done for addresses that need no chain choice, Next for the rest
    const val WATCH_SUBMIT = "watch_submit"
    fun watchChain(blockchainUid: String) = "watch_chain_$blockchainUid"
    const val WATCH_CHAINS_DONE = "watch_chains_done"

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
    // The coin icon carries the sync state: synced, syncing or failed
    fun balanceItemSync(coinCode: String, state: String) = "balance_item_${coinCode}_$state"

    // The period is the server's code for it ("1d", "1w", ... "5y"), or "all"
    fun chartPeriod(period: String) = "chart_period_$period"

    const val TOKEN_BALANCE = "token_balance"
    const val TOKEN_CHART = "token_chart"
    const val TOKEN_RECEIVE = "token_receive"
    const val TOKEN_SEND = "token_send"
    const val TOKEN_SWAP = "token_swap"
    // "View my Address" on the sheet a new, unactivated Tron wallet shows over the token screen
    const val TOKEN_TRON_VIEW_ADDRESS = "token_tron_view_address"

    const val RECEIVE_ADDRESS = "receive_address"
    const val RECEIVE_NETWORK = "receive_network"
    const val RECEIVE_FORMAT = "receive_format"
    const val RECEIVE_ADDRESS_TYPE = "receive_address_type"
    const val RECEIVE_SET_AMOUNT = "receive_set_amount"
    const val RECEIVE_SHARE = "receive_share"
    const val RECEIVE_COPY = "receive_copy"

    // The floating search bar shared by list screens; a screen has at most one
    const val SEARCH_BAR = "search_bar"

    fun manageCoin(coinCode: String, blockchainUid: String) = "manage_coin_${coinCode}_$blockchainUid"

    const val SEND_AMOUNT_INPUT = "send_amount_input"
    const val SEND_SETTINGS_UTXOS = "send_settings_utxos"
    const val SEND_SETTINGS_INPUTS_OUTPUTS = "send_settings_inputs_outputs"
    const val SEND_SETTINGS_TIME_LOCK = "send_settings_time_lock"
    const val SEND_RECIPIENT_ROW = "send_recipient_row"
    const val SEND_NEXT = "send_next"
    const val SEND_ACTION = "send_action"

    // The address entry page is shared by send, swap recipient and other flows
    const val ENTER_ADDRESS_INPUT = "enter_address_input"
    const val ENTER_ADDRESS_SUBMIT = "enter_address_submit"
    const val ENTER_ADDRESS_ERROR = "enter_address_error"

    const val CONFIRM_AMOUNT = "confirm_amount"
    const val CONFIRM_RECIPIENT = "confirm_recipient"
    const val CONFIRM_NETWORK_FEE = "confirm_network_fee"
    const val CONFIRM_SEND = "confirm_send"

    const val TX_EMPTY = "tx_empty"
    const val TX_WAITING_SYNC = "tx_waiting_sync"
    const val TX_FILTER = "tx_filter"
    const val ACTIVITY_TAB_TRANSFER = "activity_tab_transfer"
    const val ACTIVITY_TAB_SWAP = "activity_tab_swap"
    const val ACTIVITY_TAB_PRIVATE_SEND = "activity_tab_private_send"
    const val ACTIVITY_TAB_CROSS_PAY = "activity_tab_cross_pay"
    const val ACTIVITY_EMPTY = "activity_empty"

    // The record uid is the transaction hash on most chains. Bitcoin-family
    // chains use a random uid, so their rows cannot be picked by hash.
    fun txItem(uid: String) = "tx_item_${uid.take(8)}"
    fun txItemTitle(uid: String) = "tx_item_title_${uid.take(8)}"
    fun txItemSubtitle(uid: String) = "tx_item_subtitle_${uid.take(8)}"
    fun txItemValue(uid: String) = "tx_item_value_${uid.take(8)}"

    fun txInfoHash(hash: String) = "tx_info_hash_${hash.take(8)}"
    // state: completed, pending, processing, failed or not_processed
    fun txInfoStatus(state: String) = "tx_info_status_$state"

    const val SWAP_TOKEN_IN = "swap_token_in"
    const val SWAP_TOKEN_OUT = "swap_token_out"
    const val SWAP_AMOUNT_IN = "swap_amount_in"
    const val SWAP_AMOUNT_OUT = "swap_amount_out"
    const val SWAP_SWITCH = "swap_switch"
    const val SWAP_NEXT = "swap_next"
    // Replaces swap_next while the swap cannot go on: no quote, insufficient balance, sync
    const val SWAP_ERROR = "swap_error"
    const val SWAP_PROVIDER = "swap_provider"
    const val SWAP_INFO_MORE = "swap_info_more"
    const val SWAP_INFO_COPY_RAW_DATA = "swap_info_copy_raw_data"
    fun swapCoin(coinCode: String, blockchainUid: String) = "swap_coin_${coinCode}_$blockchainUid"

    const val SETTINGS_MANAGE_WALLETS = "settings_manage_wallets"
    const val SETTINGS_BLOCKCHAIN = "settings_blockchain"
    const val SETTINGS_SECURITY = "settings_security"
    const val SETTINGS_PRIVACY = "settings_privacy"
    const val SETTINGS_CONTACTS = "settings_contacts"
    const val SETTINGS_APP_SETTINGS = "settings_app_settings"
    const val SETTINGS_BACKUP_MANAGER = "settings_backup_manager"
    const val SETTINGS_ABOUT = "settings_about"
    const val SETTINGS_DAPP_CONNECTION = "settings_dapp_connection"
    const val SETTINGS_SUBSCRIPTION = "settings_subscription"
    const val SETTINGS_RATE_US = "settings_rate_us"
    const val SETTINGS_TELL_FRIENDS = "settings_tell_friends"
    const val SETTINGS_FAQ = "settings_faq"
    const val SETTINGS_LABS = "settings_labs"
    const val SETTINGS_SUPPORT = "settings_support"
    const val SETTINGS_TELEGRAM = "settings_telegram"
    const val SETTINGS_TWITTER = "settings_twitter"
    const val SETTINGS_DONATE = "settings_donate"
    const val SETTINGS_DONATE_BANNER = "settings_donate_banner"
    const val SETTINGS_DEBUG_SIMULATE_FAIL_SWAP = "settings_debug_simulate_fail_swap"
    const val SETTINGS_DEBUG_SWAP_PROVIDER_NAME = "settings_debug_swap_provider_name"

    const val PREMIUM_CLOSE = "premium_close"
    // The button under the feature list; it opens the list of plans
    const val PREMIUM_SUBSCRIBE = "premium_subscribe"
    fun premiumPlan(id: String) = "premium_plan_$id"
    // One text holding both links, Terms and Privacy
    const val PREMIUM_PLANS_AGREEMENT = "premium_plans_agreement"
    const val PREMIUM_PLANS_SUBSCRIBE = "premium_plans_subscribe"
    // The page shown after a purchase
    const val PREMIUM_SUBSCRIBED_CLOSE = "premium_subscribed_close"
    const val PREMIUM_SUBSCRIBED_GO_TO_APP = "premium_subscribed_go_to_app"

    const val APPEARANCE_RESET = "appearance_reset"
    const val APPEARANCE_LANGUAGE = "appearance_language"
    const val APPEARANCE_BASE_CURRENCY = "appearance_base_currency"
    // The dropdown buttons that show a setting's value and open its options
    const val APPEARANCE_THEME = "appearance_theme"
    const val APPEARANCE_PRICE_CHANGE = "appearance_price_change"
    const val APPEARANCE_LAUNCH_SCREEN = "appearance_launch_screen"
    const val APPEARANCE_BALANCE_VALUE = "appearance_balance_value"
    // The rows that hold those buttons; tapping a row opens the same options
    const val APPEARANCE_THEME_ROW = "appearance_theme_row"
    const val APPEARANCE_PRICE_CHANGE_ROW = "appearance_price_change_row"
    const val APPEARANCE_LAUNCH_SCREEN_ROW = "appearance_launch_screen_row"
    const val APPEARANCE_BALANCE_VALUE_ROW = "appearance_balance_value_row"
    // The switches; each reports whether it is on
    const val APPEARANCE_HIDE_MARKETS = "appearance_hide_markets"
    const val APPEARANCE_HIDE_BUTTONS = "appearance_hide_buttons"
    const val APPEARANCE_CHART_BUTTON = "appearance_chart_button"
    const val APPEARANCE_AMOUNT_ROUNDING = "appearance_amount_rounding"
    const val APPEARANCE_RECENTLY_SENT = "appearance_recently_sent"
    fun currency(code: String) = "currency_$code"
    // A tile of the app icon picker; the chosen one reports itself as checked
    fun appIcon(id: String) = "app_icon_$id"

    const val INDICATOR_MA_TYPE = "indicator_ma_type"

    const val SHOW_EXTENDED_KEY_PURPOSE = "show_extended_key_purpose"
    const val SHOW_EXTENDED_KEY_BLOCKCHAIN = "show_extended_key_blockchain"
    const val SHOW_EXTENDED_KEY_ACCOUNT = "show_extended_key_account"
    const val SHOW_MONERO_KEY_TYPE = "show_monero_key_type"
    const val TON_CONNECT_NEW_WALLET = "ton_connect_new_wallet"

    const val ABOUT_APP_STATUS = "about_app_status"
    const val ABOUT_TERMS = "about_terms"

    const val SECURITY_PASSCODE = "security_passcode"
    const val SECURITY_BIOMETRICS = "security_biometrics"
    const val SECURITY_BALANCE_AUTO_HIDE = "security_balance_auto_hide"
    const val SECURITY_AUTOLOCK = "security_autolock"
    // The dropdown button inside the row above; it opens the same list
    const val SECURITY_AUTOLOCK_VALUE = "security_autolock_value"
    fun autolockInterval(raw: String) = "autolock_$raw"

    fun pinKey(digit: Int) = "pin_key_$digit"
    const val PIN_KEY_DELETE = "pin_key_delete"
    const val PIN_KEY_BIOMETRIC = "pin_key_biometric"
    const val PIN_RANDOM = "pin_random"
    const val PIN_BACK = "pin_back"
    const val PIN_TITLE = "pin_title"
    const val PIN_DESCRIPTION = "pin_description"
    const val PIN_ERROR = "pin_error"
    const val PIN_ATTEMPTS_LEFT = "pin_attempts_left"
    const val PIN_DISABLED_UNTIL = "pin_disabled_until"

    // Index over the list as shown: wallets first, then watch addresses
    fun accountRow(index: Int) = "account_row_$index"
    fun accountOptions(index: Int) = "account_options_$index"
    const val ACCOUNT_MANUAL_BACKUP = "account_manual_backup"
    const val ACCOUNT_LOCAL_BACKUP = "account_local_backup"
    const val ACCOUNT_UNLINK = "account_unlink"
    const val ACCOUNT_NAME_INPUT = "account_name_input"
    const val ACCOUNT_NAME_CLEAR = "account_name_clear"
    const val ACCOUNT_SAVE = "account_save"
    fun unlinkConfirm(index: Int) = "unlink_confirm_$index"
    const val UNLINK_DELETE = "unlink_delete"

    const val BACKUP_REVEAL = "backup_reveal"
    // Numbered as shown on screen, from 1
    fun backupWord(number: Int) = "backup_word_$number"
    const val BACKUP_VERIFY = "backup_verify"
    fun quizPosition(index: Int) = "quiz_position_$index"
    fun quizOption(word: String) = "quiz_option_$word"

    fun selectorSwitch(index: Int) = "selector_switch_$index"
    const val SELECTOR_DONE = "selector_done"

    // The dropdown button inside a row; it opens the same list as the row
    fun dropdownValue(rowTag: String) = "${rowTag}_value"

    const val MARKET_FILTERS_SET = "market_filters_set"
    const val MARKET_FILTERS_VOLUME = "market_filters_volume"
    const val MARKET_FILTERS_BLOCKCHAINS = "market_filters_blockchains"
    const val MARKET_FILTERS_SECTORS = "market_filters_sectors"
    const val MARKET_FILTERS_PRICE_CHANGE = "market_filters_price_change"
    const val MARKET_FILTERS_PRICE_PERIOD = "market_filters_price_period"
    const val MARKET_FILTERS_TRADING_SIGNALS = "market_filters_trading_signals"
    const val MARKET_FILTERS_PRICE_CLOSE_TO = "market_filters_price_close_to"

    const val TX_FILTER_BLOCKCHAIN = "tx_filter_blockchain"
    const val TX_FILTER_COIN = "tx_filter_coin"
    const val TX_FILTER_CONTACT = "tx_filter_contact"

    const val ADD_TOKEN_BLOCKCHAIN = "add_token_blockchain"
    const val CONTACT_ADDRESS_BLOCKCHAIN = "contact_address_blockchain"
}

// Read once; previews have no App, so they get false
private val exposeTestTagsEnabled by lazy {
    runCatching { App.appConfigProvider.e2eBuild }.getOrDefault(false)
}

/**
 * Exposes [androidx.compose.ui.platform.testTag]s as resource ids, which is how UI tests outside
 * the app see them. Set at the activity root, and again in every dialog and bottom sheet: those
 * open their own windows, which do not inherit it. Only the e2e build exposes them, so other
 * builds keep their accessibility tree unchanged.
 */
@OptIn(ExperimentalComposeUiApi::class)
fun Modifier.exposeTestTags() =
    if (exposeTestTagsEnabled) semantics { testTagsAsResourceId = true } else this
