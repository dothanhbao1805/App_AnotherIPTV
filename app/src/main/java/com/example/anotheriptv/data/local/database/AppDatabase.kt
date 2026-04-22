package com.example.anotheriptv.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.anotheriptv.data.local.dao.CategoryDao
import com.example.anotheriptv.data.local.dao.ChannelDao
import com.example.anotheriptv.data.local.dao.PlaylistDao
import com.example.anotheriptv.data.local.dao.WatchHistoryDao
import com.example.anotheriptv.data.local.entity.CategoryEntity
import com.example.anotheriptv.data.local.entity.ChannelEntity
import com.example.anotheriptv.data.local.entity.PlaylistEntity
import com.example.anotheriptv.data.local.entity.WatchHistoryEntity

@Database(
    entities = [
        PlaylistEntity::class,
        ChannelEntity::class,
        WatchHistoryEntity::class,
        CategoryEntity::class
    ],
    version = 4,
    exportSchema = false
)

abstract class AppDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao
    abstract fun channelDao(): ChannelDao
    abstract fun watchHistoryDao(): WatchHistoryDao
    abstract fun categoryDao(): CategoryDao
}