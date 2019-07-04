package io.horizontalsystems.bankwallet.modules.backup

import org.junit.Assert
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object BackupWordsValidatorTest : Spek({

    val words = listOf("word1", "word2", "word3")

    group("#isValid") {

        describe("when empty words") {
            val confirmationWords = hashMapOf(1 to "word1", 1 to "")

            it("returns false") {
                Assert.assertFalse(BackupWordsValidator.isValid(confirmationWords, words))
            }
        }

        describe("when invalid words") {
            context("invalid word") {
                val confirmationWords = hashMapOf(1 to "renault", 2 to "word2")

                it("returns false") {
                    Assert.assertFalse(BackupWordsValidator.isValid(confirmationWords, words))
                }
            }

            context("invalid order") {
                val confirmationWords = hashMapOf(1 to "word2", 2 to "word1")

                it("returns false") {
                    Assert.assertFalse(BackupWordsValidator.isValid(confirmationWords, words))
                }
            }
        }

        describe("when valid words") {
            val confirmationWords = hashMapOf(1 to "word1", 2 to "word2")

            it("returns true") {
                Assert.assertTrue(BackupWordsValidator.isValid(confirmationWords, words))
            }
        }

    }
})
