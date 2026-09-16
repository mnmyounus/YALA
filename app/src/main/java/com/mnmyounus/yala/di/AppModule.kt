package com.mnmyounus.yala.di

import android.content.Context
import androidx.room.Room
import com.mnmyounus.yala.data.local.IntruderDao
import com.mnmyounus.yala.data.local.SecurePrefs
import com.mnmyounus.yala.data.local.YalaDatabase
import com.mnmyounus.yala.data.repository.IntruderRepositoryImpl
import com.mnmyounus.yala.data.repository.LockRepositoryImpl
import com.mnmyounus.yala.domain.repository.IntruderRepository
import com.mnmyounus.yala.domain.repository.LockRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton
    fun securePrefs(@ApplicationContext ctx: Context) = SecurePrefs(ctx)

    @Provides @Singleton
    fun database(@ApplicationContext ctx: Context): YalaDatabase =
        Room.databaseBuilder(ctx, YalaDatabase::class.java, "yala.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun intruderDao(db: YalaDatabase): IntruderDao = db.intruderDao()

    @Provides @Singleton
    fun lockRepository(@ApplicationContext ctx: Context, prefs: SecurePrefs): LockRepository =
        LockRepositoryImpl(ctx, prefs)

    @Provides @Singleton
    fun intruderRepositoryImpl(@ApplicationContext ctx: Context, dao: IntruderDao) =
        IntruderRepositoryImpl(ctx, dao)

    @Provides @Singleton
    fun intruderRepository(impl: IntruderRepositoryImpl): IntruderRepository = impl
}
