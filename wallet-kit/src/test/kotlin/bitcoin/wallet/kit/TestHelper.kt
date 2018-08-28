package bitcoin.wallet.kit

object TestHelper {

    // https://gist.github.com/fabiomsr/845664a9c7e92bafb6fb0ca70d4e44fd
    fun hexToByteArray(string: String): ByteArray {
        return ByteArray(string.length / 2) {
            string.substring(it * 2, it * 2 + 2).toInt(16).toByte()
        }
    }

    fun byteArrayToHex(byteArray: ByteArray): String {
        return byteArray.joinToString(separator = "") {
            it.toInt().and(0xff).toString(16).padStart(2, '0')
        }
    }
}
