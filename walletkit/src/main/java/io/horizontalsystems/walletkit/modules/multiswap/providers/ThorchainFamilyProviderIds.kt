package io.horizontalsystems.walletkit.modules.multiswap.providers

// Provider ids for the thorchain-family swap providers; the provider objects live in the
// optional walletkit-chain-thorchain module, so core code matches them by id.
const val THORCHAIN_PROVIDER_ID = "thorchain"
const val MAYA_PROVIDER_ID = "mayachain"

// EVM swap providers (objects live in the optional walletkit-chain-evm module).
const val ONEINCH_PROVIDER_ID = "oneinch"
const val UNISWAP_V3_PROVIDER_ID = "uniswap_v3"
const val PANCAKE_V3_PROVIDER_ID = "pancake_v3"
