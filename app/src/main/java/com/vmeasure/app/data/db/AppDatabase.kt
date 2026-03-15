package com.vmeasure.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.vmeasure.app.data.db.dao.AppConfigDao
import com.vmeasure.app.data.db.dao.DeletedUserIdDao
import com.vmeasure.app.data.db.dao.SectionDao
import com.vmeasure.app.data.db.dao.UserDao
import com.vmeasure.app.data.db.entity.AppConfigEntity
import com.vmeasure.app.data.db.entity.DeletedUserIdEntity
import com.vmeasure.app.data.db.entity.SectionEntity
import com.vmeasure.app.data.db.entity.UserEntity

@Database(
    entities = [
        UserEntity::class,
        SectionEntity::class,
        DeletedUserIdEntity::class,
        AppConfigEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun sectionDao(): SectionDao
    abstract fun deletedUserIdDao(): DeletedUserIdDao
    abstract fun appConfigDao(): AppConfigDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "vmeasure.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}