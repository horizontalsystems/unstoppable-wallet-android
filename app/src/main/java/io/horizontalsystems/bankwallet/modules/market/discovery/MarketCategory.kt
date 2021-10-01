package io.horizontalsystems.bankwallet.modules.market.discovery

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import io.horizontalsystems.bankwallet.R

sealed class MarketCategory(
    val id: String = "",
    @StringRes val titleResId: Int,
    @DrawableRes val imageResId: Int,
    @StringRes val descriptionResId: Int
) {
    object TopCoins : MarketCategory(
        "topcoins",
        R.string.Market_Category_TopCoins,
        R.drawable.ic_category_top_coins,
        R.string.Market_Category_TopCoins_Description
    )

    object Blockchains : MarketCategory(
        "blockchain",
        R.string.Market_Category_Blockchains,
        R.drawable.ic_category_blockchains,
        R.string.Market_Category_Blockchains_Description
    )

    object Dexes : MarketCategory(
        "dexes",
        R.string.Market_Category_Dexes,
        R.drawable.ic_category_dexes,
        R.string.Market_Category_Dexes_Description
    )

    object Lending : MarketCategory(
        "lending",
        R.string.Market_Category_Lending,
        R.drawable.ic_category_lending,
        R.string.Market_Category_Lending_Description
    )

    object Privacy : MarketCategory(
        "privacy",
        R.string.Market_Category_Privacy,
        R.drawable.ic_category_privacy,
        R.string.Market_Category_Privacy_Description
    )

    object Scaling : MarketCategory(
        "scaling",
        R.string.Market_Category_Scaling,
        R.drawable.ic_category_scaling,
        R.string.Market_Category_Scaling_Description
    )

    object Oracles : MarketCategory(
        "oracles",
        R.string.Market_Category_Oracles,
        R.drawable.ic_category_oracles,
        R.string.Market_Category_Oracles_Description
    )

    object Prediction : MarketCategory(
        "prediction_markets",
        R.string.Market_Category_Prediction,
        R.drawable.ic_category_prediction,
        R.string.Market_Category_Prediction_Description
    )

    object YieldAggregators : MarketCategory(
        "yield_aggregators",
        R.string.Market_Category_YieldAggregators,
        R.drawable.ic_category_yield_aggregators,
        R.string.Market_Category_YieldAggregators_Description
    )

    object Stablecoins : MarketCategory(
        "fiat_stablecoins",
        R.string.Market_Category_FiatStableCoins,
        R.drawable.ic_category_stable_coins,
        R.string.Market_Category_FiatStableCoins_Description
    )

    object TokenizedBitcoin : MarketCategory(
        "tokenized_bitcoin",
        R.string.Market_Category_TokenizedBitcoin,
        R.drawable.ic_category_tokenized_bitcoin,
        R.string.Market_Category_TokenizedBitcoin_Description
    )

    object ExchangeTokens : MarketCategory(
        "exchange_tokens",
        R.string.Market_Category_ExchangeTokens,
        R.drawable.ic_category_exchange_tokens,
        R.string.Market_Category_ExchangeTokens_Description
    )

    object RiskManagement : MarketCategory(
        "risk_management",
        R.string.Market_Category_RiskManagement,
        R.drawable.ic_category_risk_management,
        R.string.Market_Category_RiskManagement_Description
    )

    object Wallets : MarketCategory(
        "wallets",
        R.string.Market_Category_Wallets,
        R.drawable.ic_category_wallets,
        R.string.Market_Category_Wallets_Description
    )

    object Synthetics : MarketCategory(
        "synthetics",
        R.string.Market_Category_Synthetics,
        R.drawable.ic_category_synthetics,
        R.string.Market_Category_Synthetics_Description
    )

    object IndexFunds : MarketCategory(
        "index_funds",
        R.string.Market_Category_IndexFunds,
        R.drawable.ic_category_index_funds,
        R.string.Market_Category_IndexFunds_Description
    )

    object NFT : MarketCategory(
        "nft",
        R.string.Market_Category_NFT,
        R.drawable.ic_category_nft,
        R.string.Market_Category_NFT_Description
    )

    object FundRaising : MarketCategory(
        "fundraising",
        R.string.Market_Category_Fundraising,
        R.drawable.ic_category_fundraising,
        R.string.Market_Category_Fundraising_Description
    )

    object Gaming : MarketCategory(
        "gaming",
        R.string.Market_Category_Gaming,
        R.drawable.ic_category_gaming,
        R.string.Market_Category_Gaming_Description
    )

    object Infrastructure : MarketCategory(
        "infrastructure",
        R.string.Market_Category_Infrastructure,
        R.drawable.ic_category_infrastructure,
        R.string.Market_Category_Infrastructure_Description
    )

    object Analytics : MarketCategory(
        "analytics",
        R.string.Market_Category_Analytics,
        R.drawable.ic_category_analytics,
        R.string.Market_Category_Analytics_Description
    )

    object Storage : MarketCategory(
        "storage",
        R.string.Market_Category_Storage,
        R.drawable.ic_category_storage,
        R.string.Market_Category_Storage_Description
    )

    object Identity : MarketCategory(
        "identity",
        R.string.Market_Category_Identity,
        R.drawable.ic_category_identity,
        R.string.Market_Category_Identity_Description
    )

    object YieldTokens : MarketCategory(
        "yield_tokens",
        R.string.Market_Category_YieldTokens,
        R.drawable.ic_category_yield_token,
        R.string.Market_Category_YieldTokens_Description
    )

}
