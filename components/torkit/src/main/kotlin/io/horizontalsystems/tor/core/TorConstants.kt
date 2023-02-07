package io.horizontalsystems.tor.core


interface TorConstants {

    companion object {
        const val TAG = "TorBinary"
        const val DIRECTORY_TOR_DATA = "data"
        const val TOR_CONTROL_PORT_FILE = "control.txt"
        const val LIB_NAME = "tor"
        const val LIB_SO_NAME = "libtor.so"
        const val SHELL_CMD_PS = "ps"

        //geoip data file asset key
        const val GEOIP_ASSET_KEY = "geoip"
        const val GEOIP6_ASSET_KEY = "geoip6"

        //torrc (tor config file)
        const val TOR_ASSET_KEY = "libtor"
        const val TORRC_ASSET_KEY = "torrc"
        const val COMMON_ASSET_KEY = "common/"
        const val TOR_CONTROL_COOKIE = "control_auth_cookie"
        const val IP_LOCALHOST = "127.0.0.1"
        const val TOR_TRANSPROXY_PORT_DEFAULT = "9040"
        const val TOR_DNS_PORT_DEFAULT = "5400"
        const val HTTP_PROXY_PORT_DEFAULT = "8118" // like Privoxy!
        const val SOCKS_PROXY_PORT_DEFAULT = "9050"

        var FILE_WRITE_BUFFER_SIZE = 1024

        fun getIntValue(value: String):Int {
            try{
                return value.toInt()
            }
            catch (e: Exception){
                return -1
            }
        }
    }
}
