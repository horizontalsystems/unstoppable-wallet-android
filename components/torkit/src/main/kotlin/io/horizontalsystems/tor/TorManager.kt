package io.horizontalsystems.tor

import android.annotation.SuppressLint
import android.content.Context
import io.horizontalsystems.tor.core.TorOperator
import io.reactivex.Single

class TorManager(context: Context, private val listener: Listener) : TorOperator.Listener {

    interface Listener {
        fun statusUpdate(torInfo: Tor.Info)
    }

    companion object {
        lateinit var instance: TorManager
    }

    private var torSettings = Tor.Settings(context)
    private lateinit var torOperator: TorOperator

    init {
        instance = this
    }

    fun start(useBridges: Boolean) {
        torSettings.useBridges = useBridges
        torOperator = TorOperator(torSettings, this)
        torOperator.start()
    }

    fun stop(): Single<Boolean> {
        return torOperator.stop()
    }

    @SuppressLint("CheckResult")
    fun stopNow() {
        torOperator.stop().subscribe({}, {})
    }

    override fun statusUpdate(torInfo: Tor.Info) {
        listener.statusUpdate(torInfo)
    }

    fun newIdentity(): Boolean {
        return torOperator.newIdentity()
    }

    fun getTorInfo(): Tor.Info {
        return torOperator.torInfo
    }
}
