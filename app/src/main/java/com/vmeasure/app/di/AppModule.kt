package com.vmeasure.app.di

import android.content.Context
import com.vmeasure.app.data.db.AppDatabase
import com.vmeasure.app.data.db.dao.AppConfigDao
import com.vmeasure.app.data.db.dao.DeletedUserIdDao
import com.vmeasure.app.data.db.dao.SectionDao
import com.vmeasure.app.data.db.dao.UserDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        AppDatabase.getInstance(context)

    @Provides
    fun provideUserDao(db: AppDatabase): UserDao = db.userDao()

    @Provides
    fun provideSectionDao(db: AppDatabase): SectionDao = db.sectionDao()

    @Provides
    fun provideDeletedUserIdDao(db: AppDatabase): DeletedUserIdDao = db.deletedUserIdDao()

    @Provides
    fun provideAppConfigDao(db: AppDatabase): AppConfigDao = db.appConfigDao()

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
}