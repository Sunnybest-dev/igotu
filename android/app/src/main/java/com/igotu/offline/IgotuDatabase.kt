package com.igotu.offline

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [OfflineSosEvent::class, OfflineContact::class, OfflineDeliveryLog::class],
    version = 1,
    exportSchema = false
)
abstract class IgotuDatabase : RoomDatabase() {
    abstract fun sosEventDao(): SosEventDao
    abstract fun contactDao(): ContactDao
    abstract fun deliveryLogDao(): DeliveryLogDao
}
