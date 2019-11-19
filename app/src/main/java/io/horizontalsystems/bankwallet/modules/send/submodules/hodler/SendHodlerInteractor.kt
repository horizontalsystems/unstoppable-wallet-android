package io.horizontalsystems.bankwallet.modules.send.submodules.hodler

import io.horizontalsystems.hodler.LockTimeInterval

class SendHodlerInteractor : SendHodlerModule.IInteractor {

    override fun getLockTimeIntervals(): Array<LockTimeInterval> {
        return LockTimeInterval.values()
    }
}
