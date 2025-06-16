package cash.p.terminal.tangem.domain.usecase

import cash.p.terminal.tangem.domain.card.CardConfig
import com.tangem.common.card.Card
import com.tangem.common.card.EllipticCurve

internal class ValidateBackUseCase {

    fun isValidFull(cardDTO: Card): Boolean {
        return validateBackupStatus(cardDTO) && validateCurves(cardDTO)
    }

    fun isValidBackupStatus(cardDTO: Card): Boolean {
        return validateBackupStatus(cardDTO)
    }

    private fun validateCurves(cardDTO: Card): Boolean {
        val config = CardConfig.createConfig(cardDTO)
        // / Since the curve `bls12381_G2_AUG` was added later into first generation of wallets,
        // we cannot determine whether this curve is missing due to an error or because the user
        // did not want to recreate the wallet.
        val expectedCurves = config.mandatoryCurves
            .filterNot { it == EllipticCurve.Bls12381G2Aug }
        val curves = cardDTO.wallets.map { it.curve }
        for (expectedCurve in expectedCurves) {
            val cardCurvesCount = curves.count { it == expectedCurve }

            // missing curve
            if (cardCurvesCount == 0) {
                return false
            }

            // duplicated curve
            if (cardCurvesCount > 1) {
                return false
            }
        }
        return true
    }

    private fun validateBackupStatus(cardDTO: Card): Boolean {
        val backupStatus = cardDTO.backupStatus
        backupStatus
            ?: return true // for card with null backup status, validation should always returns true
        return backupStatus !is Card.BackupStatus.CardLinked
    }
}