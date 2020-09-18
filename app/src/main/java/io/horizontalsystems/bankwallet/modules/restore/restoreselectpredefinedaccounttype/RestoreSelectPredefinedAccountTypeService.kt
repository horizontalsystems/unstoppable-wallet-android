package io.horizontalsystems.bankwallet.modules.restore.restoreselectpredefinedaccounttype

import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.IPredefinedAccountTypeManager
import io.horizontalsystems.bankwallet.entities.PredefinedAccountType

class RestoreSelectPredefinedAccountTypeService(
        private val predefinedAccountTypeManager: IPredefinedAccountTypeManager
): RestoreSelectPredefinedAccountTypeModule.IService, Clearable {

    override val predefinedAccountTypes: List<PredefinedAccountType>
        get() = predefinedAccountTypeManager.allTypes

    override fun clear() {

    }
}
