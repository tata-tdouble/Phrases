package org.hyperskill.phrases

import android.content.Context
import android.util.Log
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch


@Entity(tableName = "phrases")
data class Phrase(
    @ColumnInfo(name = "phrase") var phrase: String,
    @PrimaryKey(autoGenerate = true) var id: Int = 0
)

@Database(entities = [Phrase::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun getPhrasesDao(): PhraseDao
}

@Dao
interface PhraseDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(vararg phrase: Phrase)

    @Delete
    fun delete(phrase: Phrase)

    @Query("DELETE FROM phrases")
    fun deleteAll()

    @Query("SELECT * FROM phrases")
    fun getAll(): Flow<List<Phrase>>

    @Query("SELECT * FROM phrases WHERE id = :id LIMIT 1")
    fun getPhrase(id: Int): Phrase

    @Query("SELECT * FROM phrases ORDER BY id DESC LIMIT 1")
    fun getMostRecentPhrase(): Phrase?

}