package io.horizontalsystems.bankwallet.modules.backup.words

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import io.horizontalsystems.bankwallet.core.IRandomProvider
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object BackupWordsInteractorTest : Spek({
    val words = arrayOf("word1", "word2", "word3")
    val random = mock<IRandomProvider>()

    val delegate by memoized {
        mock<BackupWordsModule.IInteractorDelegate>()
    }

    val interactor by memoized {
        BackupWordsInteractor(random, words).apply { this.delegate = delegate }
    }

    group("#validate") {

        describe("when empty words") {
            val confirmationWords = hashMapOf(1 to "word1", 1 to "")

            it("fails validation") {
                interactor.validate(confirmationWords)
                verify(delegate).onValidateFailure()
            }
        }

        describe("when invalid words") {
            context("invalid word") {
                val confirmationWords = hashMapOf(1 to "renault", 2 to "word2")

                it("fails validation") {
                    interactor.validate(confirmationWords)
                    verify(delegate).onValidateFailure()
                }
            }

            context("invalid order") {
                val confirmationWords = hashMapOf(1 to "word2", 2 to "word1")

                it("fails validation") {
                    interactor.validate(confirmationWords)
                    verify(delegate).onValidateFailure()
                }
            }
        }

        describe("when valid words") {
            val confirmationWords = hashMapOf(1 to "word1", 2 to "word2")

            it("succeeds validation") {
                interactor.validate(confirmationWords)
                verify(delegate).onValidateSuccess()
            }
        }

    }
})
