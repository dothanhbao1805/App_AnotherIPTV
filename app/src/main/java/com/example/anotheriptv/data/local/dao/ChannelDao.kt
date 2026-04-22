package com.example.anotheriptv.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.anotheriptv.data.local.entity.ChannelEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChannelDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(channel: ChannelEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(channels: List<ChannelEntity>)

    @Update
    suspend fun update(channel: ChannelEntity)

    @Delete
    suspend fun delete(channel: ChannelEntity)

    @Query("DELETE FROM channels WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM channels WHERE playlistId = :playlistId")
    suspend fun deleteByPlaylistId(playlistId: Long)

    @Query("DELETE FROM channels")
    suspend fun deleteAll()

    // ── Single channel ──

    @Query("SELECT * FROM channels WHERE id = :id")
    suspend fun getById(id: Long): ChannelEntity?

    // ── By playlist ──

    @Query("SELECT * FROM channels WHERE playlistId = :playlistId ORDER BY name ASC")
    fun getByPlaylistId(playlistId: Long): Flow<List<ChannelEntity>>

    @Query("""
        SELECT * FROM channels 
        WHERE playlistId = :playlistId AND category = :category 
        ORDER BY name ASC
    """)
    fun getByCategory(playlistId: Long, category: String): Flow<List<ChannelEntity>>

    @Query("""
        SELECT DISTINCT category FROM channels 
        WHERE playlistId = :playlistId 
        ORDER BY category ASC
    """)
    fun getCategoriesByPlaylistId(playlistId: Long): Flow<List<String>>

    // ── Favorites ──

    @Query("SELECT * FROM channels WHERE isFavorite = 1 ORDER BY name ASC")
    fun getFavorites(): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE playlistId = :playlistId AND isFavorite = 1 ORDER BY name ASC")
    fun getFavoritesByPlaylistId(playlistId: Long): Flow<List<ChannelEntity>>

    @Query("UPDATE channels SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: Long, isFavorite: Boolean)

    // ── Search ──

    @Query("""
        SELECT * FROM channels 
        WHERE playlistId = :playlistId AND name LIKE '%' || :query || '%' 
        ORDER BY name ASC
    """)
    fun search(playlistId: Long, query: String): Flow<List<ChannelEntity>>

    // ── Utils ──

    @Query("SELECT COUNT(*) FROM channels WHERE playlistId = :playlistId")
    suspend fun countByPlaylistId(playlistId: Long): Int

    // ── Xstream ──

    // Lấy tất cả theo contentType
    @Query("SELECT * FROM channels WHERE playlistId = :playlistId AND contentType = :contentType")
    fun getChannels(playlistId: Long, contentType: String): Flow<List<ChannelEntity>>

    // Lấy theo contentType + categoryId
    @Query("SELECT * FROM channels WHERE playlistId = :playlistId AND contentType = :contentType AND categoryId = :categoryId")
    fun getChannelsByCategory(playlistId: Long, contentType: String, categoryId: String): Flow<List<ChannelEntity>>

    // Tìm kiếm theo tên
    @Query("SELECT * FROM channels WHERE playlistId = :playlistId AND contentType = :contentType AND name LIKE '%' || :query || '%'")
    fun searchChannels(playlistId: Long, contentType: String, query: String): Flow<List<ChannelEntity>>

    @Query("""
        SELECT * FROM channels 
        WHERE playlistId = :playlistId 
        AND contentType = :contentType 
        AND categoryId = :categoryId
        LIMIT 10
    """)

    suspend fun getChannelsByCategoryLimit10(
        playlistId: Long,
        contentType: String,
        categoryId: String
    ): List<ChannelEntity>

}