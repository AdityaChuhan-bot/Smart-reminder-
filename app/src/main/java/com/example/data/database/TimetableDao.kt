package com.example.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.TimetableClass
import kotlinx.coroutines.flow.Flow

@Dao
interface TimetableDao {

    @Query("SELECT * FROM timetable_classes ORDER BY startHour ASC, startMinute ASC")
    fun getAllClasses(): Flow<List<TimetableClass>>

    @Query("SELECT * FROM timetable_classes WHERE id = :id LIMIT 1")
    fun getClassById(id: Long): Flow<TimetableClass?>

    @Query("SELECT * FROM timetable_classes WHERE id = :id LIMIT 1")
    suspend fun getClassByIdSync(id: Long): TimetableClass?

    @Query("SELECT * FROM timetable_classes WHERE isEnabled = 1")
    suspend fun getActiveClassesSync(): List<TimetableClass>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClass(timetableClass: TimetableClass): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllClasses(classes: List<TimetableClass>)

    @Update
    suspend fun updateClass(timetableClass: TimetableClass)

    @Delete
    suspend fun deleteClass(timetableClass: TimetableClass)

    @Query("DELETE FROM timetable_classes WHERE id = :id")
    suspend fun deleteClassById(id: Long)

    @Query("DELETE FROM timetable_classes")
    suspend fun deleteAllClasses()
}
