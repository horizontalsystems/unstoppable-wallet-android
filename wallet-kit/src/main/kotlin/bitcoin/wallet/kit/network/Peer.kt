package bitcoin.wallet.kit.network

class Peer(var ip: String, var score: Int = 0) {

    @Volatile
    var using: Boolean = false

    override fun equals(other: Any?): Boolean {
        if (other is Peer) {
            return ip == other.ip
        }

        return false
    }

    override fun hashCode(): Int {
        return ip.hashCode()
    }
}
