package bitcoin.wallet.kit.network

class RegTest : TestNet() {

    override var id: String = ID_REGTEST

    override var port: Int = 18444

    override var dnsSeeds: Array<String> = arrayOf(
            "blocknode01.grouvi.org",
            "blocknode02.grouvi.org",
            "blocknode03.grouvi.org",
            "blocknode04.grouvi.org"
    )

    override var paymentProtocolId: String = PAYMENT_PROTOCOL_ID_REGTEST

}
