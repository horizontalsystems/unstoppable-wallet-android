package cash.p.terminal.tangem.domain.task

import com.tangem.common.CompletionResult
import com.tangem.common.card.Card
import com.tangem.common.core.CardSession

interface ProductCommandProcessor<T> {
    fun proceed(card: Card, session: CardSession, callback: (result: CompletionResult<T>) -> Unit)
}
