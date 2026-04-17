package cash.p.terminal.domain.usecase

import cash.p.terminal.modules.contacts.ContactsRepository

class DeleteAllContactsUseCase(
    private val contactsRepository: ContactsRepository
) {
    operator fun invoke() {
        contactsRepository.clear(writeSynchronously = true)
    }
}
