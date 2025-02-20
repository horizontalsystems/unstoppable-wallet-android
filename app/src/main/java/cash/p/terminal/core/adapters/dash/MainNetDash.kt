package cash.p.terminal.core.adapters.dash

import io.horizontalsystems.bitcoincore.network.Network

class MainNetDash : Network() {

    override val protocolVersion = 70228
    override val noBloomVersion = 70201

    override var port: Int = 9999

    override var magic: Long = 0xbd6b0cbf
    override var bip32HeaderPub: Int =
        0x0488B21E   // The 4 byte header that serializes in base58 to "xpub".
    override var bip32HeaderPriv: Int =
        0x0488ADE4  // The 4 byte header that serializes in base58 to "xprv"
    override var addressVersion: Int = 76
    override var addressSegwitHrp: String = "bc"
    override var addressScriptVersion: Int = 16
    override var coinType: Int = 5
    override val blockchairChainId: String = "dash"

    override val maxBlockSize = 1_000_000
    override val dustRelayTxFee =
        1000 // https://github.com/dashpay/dash/blob/master/src/policy/policy.h#L36

    override var dnsSeeds = listOf(
        "dnsseed.dash.org",
        "dnsseed.dashdot.io",
        "dnsseed.masternode.io"
    )
}
