package com.example.anotheriptv.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.anotheriptv.data.local.entity.PlaylistEntity
import com.example.anotheriptv.data.local.entity.WatchHistoryEntity
import kotlinx.coroutines.flow.Flow


@Dao
interface WatchHistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: WatchHistoryEntity ): Long

    @Update
    suspend fun update(history: WatchHistoryEntity)

    @Delete
    suspend fun delete(history: WatchHistoryEntity)

    @Query("DELETE FROM watch_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM watch_history ORDER BY watchedAt DESC")
    fun getAll(): Flow<List<WatchHistoryEntity>>


}
