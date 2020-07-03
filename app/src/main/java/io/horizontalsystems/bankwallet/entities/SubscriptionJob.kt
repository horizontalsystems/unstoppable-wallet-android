package io.horizontalsystems.bankwallet.entities

import androidx.room.Entity

@Entity(primaryKeys = ["coinCode", "stateType"])
class SubscriptionJob(
        val coinCode: String,
        val topicName: String,
        val stateType: StateType,
        val jobType: JobType
) {
    enum class StateType(val value: String) {
        Change("change"),
        Trend("trend");

        companion object {
            fun valueOf(value: String?): StateType? {
                return values().find { it.value == value }
            }
        }
    }

    enum class JobType(val value: String) {
        Subscribe("subscribe"),
        Unsubscribe("unsubscribe");

        companion object {
            fun valueOf(value: String?): JobType? {
                return values().find { it.value == value }
            }
        }
    }
}
