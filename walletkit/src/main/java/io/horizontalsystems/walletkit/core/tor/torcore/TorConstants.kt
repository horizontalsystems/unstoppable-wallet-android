package io.horizontalsystems.walletkit.core.tor.torcore


interface TorConstants {

    companion object {
        const val DIRECTORY_TOR_DATA = "data"
        const val TOR_CONTROL_PORT_FILE = "control.txt"
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

        // Nothing listens on port 1 and an unprivileged app cannot bind it, so pointing the
        // proxy there refuses every connection until Tor reports the ports it opened
        const val BLOCKED_PROXY_PORT = "1"

        var FILE_WRITE_BUFFER_SIZE = 1024
    }
}
