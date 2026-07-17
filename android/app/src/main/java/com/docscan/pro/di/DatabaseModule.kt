package com.docscan.pro.di

import android.content.Context
import androidx.room.Room
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

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ScanProDatabase =
        Room.databaseBuilder(context, ScanProDatabase::class.java, "scanpro.db").build()

    @Provides
    fun provideDocumentDao(db: ScanProDatabase): DocumentDao = db.documentDao()
}
