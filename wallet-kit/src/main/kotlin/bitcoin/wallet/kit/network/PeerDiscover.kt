package bitcoin.wallet.kit.network

import bitcoin.walllet.kit.common.BitcoinException
import org.slf4j.LoggerFactory
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.*


object PeerDiscover {

    private val log = LoggerFactory.getLogger(PeerDiscover::class.java)

    // https://en.bitcoin.it/wiki/Satoshi_Client_Node_Discovery#DNS_Addresses
    private val DNS_SEEDS = arrayOf(
            "bitseed.xf2.org",
            "dnsseed.bluematt.me",
            "seed.bitcoin.sipa.be",
            "dnsseed.bitcoin.dashjr.org",
            "seed.bitcoinstats.com"
    )

    /**
     * Lookup bitcoin peers by DNS seed.
     *
     * @return InetAddress[] contains 1~N peers.
     * @throws BitcoinException If lookup failed.
     */
    fun lookup(): Array<String> {
        log.info("Lookup peers from DNS seed...")

        val ips: MutableList<String> = ArrayList()

        DNS_SEEDS.forEach { host ->
            try {
                val addresses = InetAddress.getAllByName(host)
                for (address in addresses) {
                    if (address is InetAddress) {
                        ips.add(address.hostAddress)
                    }
                }
            } catch (e: UnknownHostException) {
                log.warn("Cannot look up host: $host")
            }
        }

        if (ips.isEmpty()) {
            throw BitcoinException("Cannot lookup pears from all DNS seeds.")
        }

        log.info(ips.size.toString() + " peers found.")
        return ips.toTypedArray()
    }
}
