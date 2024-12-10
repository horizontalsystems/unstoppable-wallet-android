package io.horizontalsystems.bankwallet.core.stats

import io.horizontalsystems.bankwallet.core.bitcoinCashCoinType
import io.horizontalsystems.bankwallet.core.derivation
import io.horizontalsystems.bankwallet.entities.BtcRestoreMode
import io.horizontalsystems.marketkit.models.Token

enum class StatPage(val key: String) {
    AboutApp("about_app"),
    Academy("academy"),
    AccountExtendedPrivateKey("account_extended_private_key"),
    AccountExtendedPublicKey("account_extended_public_key"),
    AddEvmSyncSource("add_evm_sync_source"),
    AddToken("add_token"),
    AdvancedSearch("advanced_search"),
    AdvancedSearchResults("advanced_search_results"),
    Appearance("appearance"),
    AppStatus("app_status"),
    BackupManager("backup_manager"),
    BackupPromptAfterCreate("backup_prompt_after_create"),
    BackupRequired("backup_required"),
    Balance("balance"),
    BaseCurrency("base_currency"),
    BirthdayInput("birthday_input"),
    Bip32RootKey("bip32_root_key"),
    BlockchainSettings("blockchain_settings"),
    BlockchainSettingsBtc("blockchain_settings_btc"),
    BlockchainSettingsEvm("blockchain_settings_evm"),
    BlockchainSettingsEvmAdd("blockchain_settings_evm_add"),
    BlockchainSettingsSolana("blockchain_settings_sol"),
    CloudBackup("cloud_backup"),
    FileBackup("file_backup"),
    CoinAnalytics("coin_analytics"),
    CoinAnalyticsCexVolume("coin_analytics_cex_volume"),
    CoinAnalyticsDexVolume("coin_analytics_dex_volume"),
    CoinAnalyticsDexLiquidity("coin_analytics_dex_liquidity"),
    CoinAnalyticsActiveAddresses("coin_analytics_active_addresses"),
    CoinAnalyticsTxCount("coin_analytics_tx_count"),
    CoinAnalyticsTvl("coin_analytics_tvl"),
    CoinManager("coin_manager"),
    CoinMarkets("coin_markets"),
    CoinOverview("coin_overview"),
    CoinPage("coin_page"),
    CoinCategory("coin_category"),
    CoinRankAddress("coin_rank_address"),
    CoinRankCexVolume("coin_rank_cex_volume"),
    CoinRankDexLiquidity("coin_rank_dex_liquidity"),
    CoinRankDexVolume("coin_rank_dex_volume"),
    CoinRankFee("coin_rank_fee"),
    CoinRankHolders("coin_rank_holders"),
    CoinRankRevenue("coin_rank_revenue"),
    CoinRankTxCount("coin_rank_tx_count"),
    Contacts("contacts"),
    ContactAddToExisting("contact_add_to_existing"),
    ContactNew("contact_new"),
    ContactUs("contact_us"),
    Donate("donate"),
    DonateAddressList("donate_address_list"),
    DoubleSpend("double_spend"),
    EvmAddress("evm_address"),
    EvmPrivateKey("evm_private_key"),
    ExportFull("export_full"),
    ExportFullToFiles("export_full_to_files"),
    ExportWalletToFiles("export_wallet_to_files"),
    ExternalBlockExplorer("external_block_explorer"),
    ExternalCoinWebsite("external_coin_website"),
    ExternalCoinWhitePaper("external_coin_white_paper"),
    ExternalCompanyWebsite("external_company_website"),
    ExternalGithub("external_github"),
    ExternalMarketPair("external_market_pair"),
    ExternalNews("external_news"),
    ExternalReddit("external_reddit"),
    ExternalTelegram("external_telegram"),
    ExternalTwitter("external_twitter"),
    ExternalWebsite("external_website"),
    Faq("faq"),
    GlobalMetricsMarketCap("global_metrics_market_cap"),
    GlobalMetricsVolume("global_metrics_volume"),
    GlobalMetricsEtf("global_metrics_etf"),
    GlobalMetricsTvlInDefi("global_metrics_tvl_in_defi"),
    Guide("guide"),
    ImportFull("import_full"),
    ImportFullFromFiles("import_full_from_files"),
    ImportWallet("import_wallet"),
    ImportWalletFromKey("import_wallet_from_key"),
    ImportWalletFromKeyAdvanced("import_wallet_from_key_advanced"),
    ImportWalletFromCloud("import_wallet_from_cloud"),
    ImportWalletFromFiles("import_wallet_from_files"),
    ImportWalletFromExchangeWallet("import_wallet_from_exchange_wallet"),
    ImportWalletNonStandard("import_wallet_non_standard"),
    Indicators("indicators"),
    Info("info"),
    Language("language"),
    Main("main"),
    ManageWallet("manage_wallet"),
    ManageWallets("manage_wallets"),
    ManualBackup("manual_backup"),
    Markets("markets"),
    MarketOverview("market_overview"),
    MarketSearch("market_search"),
    NewWallet("new_wallet"),
    NewWalletAdvanced("new_wallet_advanced"),
    PrivateKeys("private_keys"),
    Privacy("privacy"),
    PublicKeys("public_keys"),
    RateUs("rate_us"),
    Receive("receive"),
    ReceiveTokenList("receive_token_list"),
    RecoveryPhrase("recovery_phrase"),
    Resend("resend"),
    RestoreSelect("restore_select"),
    ScanQrCode("scan_qr_code"),
    Security("security"),
    Send("send"),
    SendTokenList("send_token_list"),
    SendConfirmation("send_confirmation"),
    Settings("settings"),
    Swap("swap"),
    SwapApproveConfirmation("swap_approve_confirmation"),
    SwapConfirmation("swap_confirmation"),
    SwapSettings("swap_settings"),
    SwapProvider("swap_provider"),
    SwitchWallet("switch_wallet"),
    TellFriends("tell_friends"),
    Terms("terms"),
    TopNftCollections("top_nft_collections"),
    TopPlatform("top_platform"),
    TokenPage("token_page"),
    Transactions("transactions"),
    TransactionFilter("transaction_filter"),
    TransactionInfo("transaction_info"),
    UnlinkWallet("unlink_wallet"),
    WalletConnect("wallet_connect"),
    TonConnect("ton_connect"),
    WatchWallet("watch_wallet"),
    WhatsNew("whats_news"),
    Widget("widget"),
}

enum class StatSection(val key: String) {
    AddressFrom("address_from"),
    AddressRecipient("address_recipient"),
    AddressSpender("address_spender"),
    AddressTo("address_to"),
    Input("input"),
    Popular("popular"),
    Recent("recent"),
    SearchResults("search_results"),
    Status("status"),
    TimeLock("time_lock"),
    TopGainers("top_gainers"),
    TopLosers("top_losers"),
    TopPlatforms("top_platforms"),
    Coins("coins"),
    Watchlist("watchlist"),
    News("news"),
    Platforms("platforms"),
    Pairs("pairs")
}

sealed class StatEvent {

    object Send : StatEvent()

    data class AddEvmSource(val chainUid: String) : StatEvent()
    data class DeleteCustomEvmSource(val chainUid: String) : StatEvent()
    data class DisableToken(val token: Token) : StatEvent()
    data class EnableToken(val token: Token) : StatEvent()

    data class ImportWallet(val walletType: String) : StatEvent()
    object ImportFull : StatEvent()

    data class ExportWallet(val walletType: String) : StatEvent()
    object ExportFull : StatEvent()

    data class OpenArticle(val relativeUrl: String): StatEvent()

    data class OpenBlockchainSettingsBtc(val chainUid: String) : StatEvent()
    data class OpenBlockchainSettingsEvm(val chainUid: String) : StatEvent()
    data class OpenBlockchainSettingsEvmAdd(val chainUid: String) : StatEvent()

    data class OpenCategory(val categoryUid: String) : StatEvent()
    data class OpenCoin(val coinUid: String) : StatEvent()
    data class OpenPlatform(val chainUid: String) : StatEvent()
    data class OpenReceive(val token: Token) : StatEvent()
    data class OpenResend(val chainUid: String, val type: StatResendType) : StatEvent()
    data class OpenSend(val token: Token) : StatEvent()
    data class OpenTokenPage(val token: Token?, val assetId: String? = null) : StatEvent()
    data class OpenTokenInfo(val token: Token) : StatEvent()
    data class Open(val page: StatPage) : StatEvent()

    data class HideBalanceButtons(val shown: Boolean): StatEvent()
    data class SelectTheme(val type: String): StatEvent()
    data class SelectLaunchScreen(val type: String): StatEvent()
    data class SelectBalanceConversion(val coinUid: String): StatEvent()
    data class SelectBalanceValue(val type: String): StatEvent()
    data class SelectAppIcon(val iconUid: String): StatEvent()
    data class ShowMarketsTab(val shown: Boolean): StatEvent()
    data class SwitchPriceChangeMode(val changeMode: String): StatEvent()
    data class SwitchLanguage(val language: String): StatEvent()
    data class ShowSignals(val shown: Boolean): StatEvent()
    data class EnableUiStats(val enabled: Boolean): StatEvent()

    data class SwitchBaseCurrency(val code: String) : StatEvent()
    data class SwitchBtcSource(val chainUid: String, val type: BtcRestoreMode) : StatEvent()
    data class SwitchEvmSource(val chainUid: String, val type: String) : StatEvent()
    data class SwitchTab(val tab: StatTab) : StatEvent()
    data class SwitchMarketTop(val marketTop: StatMarketTop) : StatEvent()
    data class SwitchPeriod(val period: StatPeriod) : StatEvent()
    data class SwitchField(val field: StatField) : StatEvent()
    data class SwitchSortType(val sortType: StatSortType) : StatEvent()
    data class SwitchChartPeriod(val period: StatPeriod) : StatEvent()
    data class SwitchTvlChain(val chain: String) : StatEvent()
    data class SwitchFilterType(val type: String) : StatEvent()
    object ToggleSortDirection : StatEvent()
    data class ToggleTvlField(val fieldArg: String) : StatEvent()

    object Refresh : StatEvent()

    object ToggleBalanceHidden : StatEvent()
    object ToggleConversionCoin : StatEvent()


    data class AddToWatchlist(val coinUid: String) : StatEvent()
    data class RemoveFromWatchlist(val coinUid: String) : StatEvent()

    data class ToggleIndicators(val shown: Boolean) : StatEvent()
    object AddToWallet : StatEvent()
    object RemoveFromWallet : StatEvent()

    data class Copy(val entity: StatEntity) : StatEvent()
    data class CopyAddress(val chainUid: String) : StatEvent()

    data class Share(val entity: StatEntity) : StatEvent()

    object SetAmount : StatEvent()
    object RemoveAmount : StatEvent()

    object ToggleHidden : StatEvent()
    object TogglePrice : StatEvent()

    data class SwapSelectTokenIn(val token: Token) : StatEvent()
    data class SwapSelectTokenOut(val token: Token) : StatEvent()
    data class SwapSelectProvider(val uid: String) : StatEvent()
    object SwapSwitchPairs : StatEvent()

    data class Select(val entity: StatEntity) : StatEvent()
    data class Edit(val entity: StatEntity) : StatEvent()
    data class Delete(val entity: StatEntity) : StatEvent()

    data class ScanQr(val entity: StatEntity) : StatEvent()
    data class Paste(val entity: StatEntity) : StatEvent()
    data class Clear(val entity: StatEntity) : StatEvent()

    data class CreateWallet(val walletType: String) : StatEvent()
    data class WatchWallet(val walletType: String) : StatEvent()

    data class Add(val entity: StatEntity) : StatEvent()
    data class AddToken(val token: Token) : StatEvent()

    val name: String
        get() = when (this) {
            is AddEvmSource -> "add_evm_source"
            is DeleteCustomEvmSource -> "delete_custom_evm_source"
            is DisableToken -> "disable_token"
            is EnableToken -> "enable_token"

            is ImportFull -> "import_full"
            is ImportWallet -> "import_wallet"

            is ExportFull -> "export_full"
            is ExportWallet -> "export_wallet"

            is OpenArticle -> "open_article"
            is OpenBlockchainSettingsBtc,
            is OpenBlockchainSettingsEvm,
            is OpenBlockchainSettingsEvmAdd,
            is OpenCategory,
            is OpenCoin,
            is OpenPlatform,
            is OpenReceive,
            is OpenSend,
            is OpenTokenPage,
            is OpenResend,
            is Open -> "open_page"

            is OpenTokenInfo -> "open_token_info"

            is HideBalanceButtons -> "hide_balance_buttons"
            is SelectTheme -> "select_theme"
            is SelectLaunchScreen -> "select_launch_screen"
            is SelectBalanceConversion -> "select_balance_conversion"
            is SelectBalanceValue -> "select_balance_value"
            is SelectAppIcon -> "select_app_icon"
            is ShowMarketsTab -> "show_markets_tab"
            is SwitchPriceChangeMode -> "switch_price_change_mode"
            is SwitchLanguage -> "switch_language"
            is ShowSignals -> "show_signals"
            is EnableUiStats -> "enable_ui_stats"

            is SwapSelectTokenIn -> "swap_select_token_in"
            is SwapSelectTokenOut -> "swap_select_token_out"
            is SwapSelectProvider -> "swap_select_provider"
            is SwapSwitchPairs -> "swap_switch_pairs"

            is Send -> "send"
            is SwitchBaseCurrency -> "switch_base_currency"
            is SwitchBtcSource -> "switch_btc_source"
            is SwitchEvmSource -> "switch_evm_source"
            is SwitchTab -> "switch_tab"
            is SwitchMarketTop -> "switch_market_top"
            is SwitchPeriod -> "switch_period"
            is SwitchField -> "switch_field"
            is SwitchSortType -> "switch_sort_type"
            is SwitchChartPeriod -> "switch_chart_period"
            is SwitchTvlChain -> "switch_tvl_platform"
            is SwitchFilterType -> "switch_filter_type"
            is ToggleSortDirection -> "toggle_sort_direction"
            is ToggleTvlField -> "toggle_tvl_field"
            is Refresh -> "refresh"
            is ToggleBalanceHidden -> "toggle_balance_hidden"
            is ToggleConversionCoin -> "toggle_conversion_coin"
            is TogglePrice -> "toggle_price"

            is AddToWatchlist -> "add_to_watchlist"
            is RemoveFromWatchlist -> "remove_from_watchlist"
            is ToggleIndicators -> "toggle_indicators"
            is AddToWallet -> "add_to_wallet"
            is RemoveFromWallet -> "remove_from_wallet"
            is Copy,
            is CopyAddress -> "copy"

            is Share -> "share"
            is SetAmount -> "set_amount"
            is RemoveAmount -> "remove_amount"
            is ToggleHidden -> "toggle_hidden"
            is Select -> "select"
            is Edit -> "edit"
            is Delete -> "delete"
            is ScanQr -> "scan_qr"
            is Paste -> "paste"
            is Clear -> "clear"
            is CreateWallet -> "create_wallet"
            is WatchWallet -> "watch_wallet"
            is Add -> "add"
            is AddToken -> "add_token"
        }

    val params: Map<StatParam, Any>?
        get() = when (this) {

            is AddEvmSource -> mapOf(
                StatParam.ChainUid to chainUid
            )

            is DeleteCustomEvmSource -> mapOf(
                StatParam.ChainUid to chainUid
            )

            is DisableToken -> tokenParams(token)

            is EnableToken -> tokenParams(token)

            is OpenArticle -> mapOf(
                StatParam.RelativeUrl to relativeUrl
            )

            is OpenBlockchainSettingsBtc -> mapOf(
                StatParam.Page to StatPage.BlockchainSettingsBtc.key,
                StatParam.ChainUid to chainUid
            )

            is OpenBlockchainSettingsEvm -> mapOf(
                StatParam.Page to StatPage.BlockchainSettingsEvm.key,
                StatParam.ChainUid to chainUid
            )

            is OpenBlockchainSettingsEvmAdd -> mapOf(
                StatParam.Page to StatPage.BlockchainSettingsEvmAdd.key,
                StatParam.ChainUid to chainUid
            )

            is OpenCategory -> mapOf(
                StatParam.Page to StatPage.CoinCategory.key,
                StatParam.CategoryUid to categoryUid
            )

            is OpenCoin -> mapOf(
                StatParam.Page to StatPage.CoinPage.key,
                StatParam.CoinUid to coinUid
            )

            is OpenPlatform -> mapOf(
                StatParam.Page to StatPage.TopPlatform.key,
                StatParam.ChainUid to chainUid
            )

            is OpenReceive -> mapOf(StatParam.Page to StatPage.Receive.key) + tokenParams(token)

            is OpenSend -> mapOf(StatParam.Page to StatPage.Send.key) + tokenParams(token)

            is OpenTokenPage -> buildMap {
                put(StatParam.Page, StatPage.TokenPage.key)
                putAll(tokenParams(token))
                assetId?.let { put(StatParam.AssetId, it) }
            }

            is OpenTokenInfo -> tokenParams(token)

            is OpenResend -> mapOf(StatParam.Page to StatPage.Resend.key, StatParam.ChainUid to chainUid, StatParam.Type to type.key)

            is Open -> mapOf(StatParam.Page to page.key)

            //Appearance
            is HideBalanceButtons -> mapOf(StatParam.Shown to shown)
            is SelectTheme -> mapOf(StatParam.Type to type)
            is SelectLaunchScreen -> mapOf(StatParam.Type to type )
            is SelectBalanceConversion -> mapOf(StatParam.CoinUid to coinUid)
            is SelectBalanceValue -> mapOf(StatParam.Type to type)
            is SelectAppIcon -> mapOf(StatParam.IconUid to iconUid)
            is ShowMarketsTab -> mapOf(StatParam.Shown to shown)
            is SwitchPriceChangeMode -> mapOf(StatParam.ChangeMode to changeMode)
            is SwitchLanguage -> mapOf(StatParam.Language to language)
            is ShowSignals -> mapOf(StatParam.Shown to shown)

            is SwapSelectTokenIn -> tokenParams(token)

            is SwapSelectTokenOut -> tokenParams(token)

            is SwapSelectProvider -> mapOf(StatParam.Provider to uid)

            is SwitchBaseCurrency -> mapOf(StatParam.CurrencyCode to code)

            is SwitchBtcSource -> mapOf(StatParam.ChainUid to chainUid, StatParam.Type to type.raw)

            is SwitchEvmSource -> mapOf(StatParam.ChainUid to chainUid, StatParam.Type to type)

            is SwitchTab -> mapOf(StatParam.Tab to tab.key)

            is SwitchMarketTop -> mapOf(StatParam.MarketTop to marketTop.key)

            is SwitchPeriod -> mapOf(StatParam.Period to period.key)

            is SwitchField -> mapOf(StatParam.Field to this.field.key)

            is SwitchSortType -> mapOf(StatParam.Type to sortType.key)

            is SwitchChartPeriod -> mapOf(StatParam.Period to period.key)

            is SwitchTvlChain -> mapOf(StatParam.TvlChain to chain)

            is SwitchFilterType -> mapOf(StatParam.Type to type)

            is EnableUiStats -> mapOf(StatParam.Enabled to enabled)

            is AddToWatchlist -> mapOf(StatParam.CoinUid to coinUid)

            is RemoveFromWatchlist -> mapOf(StatParam.CoinUid to coinUid)

            is ToggleIndicators -> mapOf(StatParam.Shown to shown)

            is ToggleTvlField -> mapOf(StatParam.Field to fieldArg)

            is Copy -> mapOf(StatParam.Entity to entity.key)

            is CopyAddress -> mapOf(StatParam.ChainUid to chainUid)

            is Select -> mapOf(StatParam.Entity to entity.key)

            is Edit -> mapOf(StatParam.Entity to entity.key)

            is Delete -> mapOf(StatParam.Entity to entity.key)

            is ScanQr -> mapOf(StatParam.Entity to entity.key)

            is Paste -> mapOf(StatParam.Entity to entity.key)

            is Clear -> mapOf(StatParam.Entity to entity.key)

            is CreateWallet -> mapOf(StatParam.WalletType to walletType)

            is ExportWallet -> mapOf(StatParam.WalletType to walletType)

            is ImportWallet -> mapOf(StatParam.WalletType to walletType)

            is WatchWallet -> mapOf(StatParam.WalletType to walletType)

            is Add -> mapOf(StatParam.Entity to entity.key)

            is AddToken -> tokenParams(token) + mapOf(StatParam.Entity to StatEntity.Token.key)

            is Share -> mapOf(StatParam.Entity to entity.key)

            else -> null
        }

    private fun tokenParams(token: Token?) = buildMap {
        token?.let {
            put(StatParam.CoinUid, token.coin.uid)
            put(StatParam.ChainUid, token.blockchainType.uid)

            token.type.derivation?.let { put(StatParam.Derivation, it.name.lowercase()) }
            token.type.bitcoinCashCoinType?.let { put(StatParam.BitcoinCashCoinType, it.name.lowercase()) }
        }
    }
}

enum class StatParam(val key: String) {
    AssetId("asset_id"),
    BitcoinCashCoinType("bitcoin_cash_coin_type"),
    CategoryUid("category_uid"),
    ChainUid("chain_uid"),
    ChangeMode("change_mode"),
    CoinUid("coin_uid"),
    CurrencyCode("currency_code"),
    Derivation("derivation"),
    Enabled("enabled"),
    Entity("entity"),
    Field("field"),
    IconUid("icon_uid"),
    Language("language"),
    MarketTop("market_top"),
    Page("page"),
    Period("period"),
    Provider("provider"),
    RelativeUrl("relative_url"),
    Shown("shown"),
    Tab("tab"),
    TvlChain("tvl_chain"),
    Type("type"),
    WalletType("wallet_type")
}

enum class StatTab(val key: String) {
    Markets("markets"),
    Balance("balance"),
    Transactions("transactions"),
    Settings("settings"),
    Overview("overview"),
    News("news"),
    Watchlist("watchlist"),
    Analytics("analytics"),
    All("all"),
    Incoming("incoming"),
    Outgoing("outgoing"),
    Swap("swap"),
    Approve("approve"),
    Coins("coins"),
    Pairs("pairs"),
    Sectors("sectors"),
    Platforms("platforms"),
}

enum class StatSortType(val key: String) {
    Balance("balance"),
    Name("name"),
    PriceChange("price_change"),
    HighestCap("highest_cap"),
    LowestCap("lowest_cap"),
    HighestVolume("highest_volume"),
    LowestVolume("lowest_volume"),
    TopGainers("top_gainers"),
    TopLosers("top_losers"),
    Manual("manual"),
    HighestAssets("highest_assets"),
    LowestAssets("lowest_assets"),
    Inflow("inflow"),
    Outflow("outflow")
}

enum class StatPeriod(val key: String) {
    Day1("1d"),
    Week1("1w"),
    Week2("2w"),
    Month1("1m"),
    Month3("3m"),
    Month6("6m"),
    Year1("1y"),
    Year2("2y"),
    Year5("5y"),
    All("all")
}

enum class StatField(val key: String) {
    MarketCap("market_cap"),
    Volume("volume"),
    Price("price")
}

enum class StatMarketTop(val key: String) {
    Top100("top100"),
    Top200("top200"),
    Top300("top300"),
    Top500("top500"),
}

enum class StatEntity(val key: String) {
    Account("account"),
    Address("address"),
    Blockchain("blockchain"),
    CloudBackup("cloud_backup"),
    ContractAddress("contract_address"),
    Derivation("derivation"),
    EvmAddress("evm_address"),
    EvmPrivateKey("evm_private_key"),
    Key("key"),
    Passphrase("passphrase"),
    ReceiveAddress("receive_address"),
    RecoveryPhrase("recovery_phrase"),
    RawTransaction("raw_transaction"),
    Status("status"),
    Token("token"),
    TransactionId("transaction_id"),
    Wallet("wallet"),
    WalletName("wallet_name")
}

enum class StatResendType(val key: String) {
    SpeedUp("speed_up"),
    Cancel("cancel")
}
