package com.docscan.pro.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [DocumentEntity::class, PageEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class ScanProDatabase : RoomDatabase() {
    abstract fun documentDao(): DocumentDao
}
