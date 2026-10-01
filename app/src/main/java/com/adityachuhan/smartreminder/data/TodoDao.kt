package com.adityachuhan.smartreminder.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoDao {
    @Query("SELECT * FROM todos ORDER BY isCompleted ASC, priority = 'HIGH' DESC, priority = 'NORMAL' DESC, createdTimestamp DESC")
    fun observeAll(): Flow<List<TodoItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(todo: TodoItem): Long

    @Update
    suspend fun update(todo: TodoItem)

    @Delete
    suspend fun delete(todo: TodoItem)

    @Query("SELECT * FROM todos WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): TodoItem?

    @Query("DELETE FROM todos WHERE isCompleted = 1")
    suspend fun clearCompleted()
}
