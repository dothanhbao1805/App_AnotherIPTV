package com.example.anotheriptv.data.local.dao

import androidx.room.*
import com.example.anotheriptv.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<CategoryEntity>)

    @Query("SELECT * FROM categories WHERE playlistId = :playlistId AND contentType = :contentType")
    fun getCategories(playlistId: Long, contentType: String): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE playlistId = :playlistId AND contentType = :contentType")
    suspend fun getCategoriesByPlaylistAndType(
        playlistId: Long,
        contentType: String
    ): List<CategoryEntity>

    @Query("DELETE FROM categories WHERE playlistId = :playlistId")
    suspend fun deleteByPlaylistId(playlistId: Long)
}