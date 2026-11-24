package com.multmaster.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [AttemptEntity::class, MistakeEntity::class, TableProgressEntity::class,
                SessionEntity::class, FavoriteEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun attemptDao(): AttemptDao
    abstract fun mistakeDao(): MistakeDao
    abstract fun tableProgressDao(): TableProgressDao
    abstract fun sessionDao(): SessionDao
    abstract fun favoriteDao(): FavoriteDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun get(context: Context): AppDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "multmaster.db")
                .fallbackToDestructiveMigration()
                .build().also { INSTANCE = it }
        }
    }
}