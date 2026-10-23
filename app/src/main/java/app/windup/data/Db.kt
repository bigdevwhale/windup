package app.windup.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Upsert

@Entity(tableName = "pin", indices = [Index("at")])
data class PinRow(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val at: Long,
    val toy: String,
    val ex: String,
    val done: Int,
    val target: Int,
    val gears: Int,
    val springs: Int,
    val over: Boolean,
)

@Entity(tableName = "meta")
data class MetaRow(@PrimaryKey val k: String, val v: String)

@Dao
interface ShelfDao {
    @Query("SELECT * FROM pin ORDER BY at, id") suspend fun pins(): List<PinRow>
    @Query("SELECT * FROM meta") suspend fun meta(): List<MetaRow>

    @Insert suspend fun insert(p: PinRow): Long
    @Upsert suspend fun meta(m: List<MetaRow>)

    @Transaction
    suspend fun pin(p: PinRow, m: List<MetaRow>): Long = insert(p).also { meta(m) }
}

@Database(entities = [PinRow::class, MetaRow::class], version = 1, exportSchema = true)
abstract class ShelfDb : RoomDatabase() {
    abstract fun dao(): ShelfDao

    companion object {
        fun create(c: Context): ShelfDb = Room.databaseBuilder(c, ShelfDb::class.java, "shelf.db").build()
        fun memory(c: Context): ShelfDb = Room.inMemoryDatabaseBuilder(c, ShelfDb::class.java).build()
    }
}
