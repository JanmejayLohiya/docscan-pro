package com.docscan.pro.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.docscan.pro.data.local.DocumentDao
import com.docscan.pro.data.local.ScanProDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE documents ADD COLUMN ocr_text TEXT")
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ScanProDatabase =
        Room.databaseBuilder(context, ScanProDatabase::class.java, "scanpro.db")
            .addMigrations(MIGRATION_1_2)
            .build()

    @Provides
    fun provideDocumentDao(db: ScanProDatabase): DocumentDao = db.documentDao()
}
