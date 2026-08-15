package com.dsp.ping.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [PingEntity::class], version = 1, exportSchema = false)
abstract class PingDatabase : RoomDatabase() {
    abstract fun pingDao(): PingDao

    companion object {
        private const val DATABASE_NAME = "ping-database"

        @Volatile
        private var INSTANCE: PingDatabase? = null

        fun getInstance(context: Context): PingDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): PingDatabase {
            return Room.databaseBuilder(
                context,
                PingDatabase::class.java,
                DATABASE_NAME
            ).build()
        }
    }
}
