package bitcoin.wallet.core.managers

import android.os.Handler
import bitcoin.wallet.core.IUnspentOutputProvider
import bitcoin.wallet.entities.UnspentOutput
import io.reactivex.subjects.PublishSubject

class UnspentOutputProvider : IUnspentOutputProvider {

    override val unspentOutputs: List<UnspentOutput>
        get() {
//            return App.db.unspentOutputDao().all

            return listOf(
                    UnspentOutput(32500000, 0, 0, "", ""),
                    UnspentOutput(16250000, 0, 0, "", "")
            )
//            return NetworkManager.getUnspentOutputs()
//                    .doOnNext {
//                        it.size.log()
//                    }
//                    .blockingFirst()
        }

    override val subject = PublishSubject.create<List<UnspentOutput>>()

    init {
        val handler = Handler()
        handler.postDelayed(Runnable {
            subject.onNext(
                    listOf(
                            UnspentOutput(42500000, 0, 0, "", ""),
                            UnspentOutput(26250000, 0, 0, "", "")
                    )

            )

        }, 3000)

    }
}