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

    @Query("SELECT * FROM categories WHERE playlistId = :playlistId AND contentType = :contentType ORDER BY name ASC")
    suspend fun getByPlaylistIdAndType(playlistId: Long, contentType: String): List<CategoryEntity>

    @Query("UPDATE categories SET isHidden = :isHidden WHERE playlistId = :playlistId AND categoryId = :categoryId AND contentType = :contentType")
    suspend fun updateVisibility(playlistId: Long, categoryId: String, contentType: String, isHidden: Int)

    @Query("SELECT * FROM categories WHERE playlistId = :playlistId AND contentType = :contentType AND isHidden = 0 ORDER BY name ASC")
    suspend fun getVisibleCategoriesByPlaylistAndType(playlistId: Long, contentType: String): List<CategoryEntity>

}