package com.example.data.database

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "user_accounts")
data class UserAccount(
    @PrimaryKey val email: String,
    val passwordKey: String,
    val name: String,
    val username: String,
    val location: String = "Chennai, Tamil Nadu",
    val profilePhotoUri: String = "",
    val isLoggedIn: Boolean = false,
    val biometricEnabled: Boolean = false
)

@Entity(tableName = "ai_creations")
data class VisionAiCreation(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val prompt: String,
    val style: String,
    val cameraAngle: String,
    val resolution: String,
    val fps: Int,
    val duration: Int,
    val type: String, // "VIDEO", "IMAGE", "VOICEOVER", "WRITING"
    val visualUrl: String, // Simulated generated URL (can be a stylish stock abstract image or specific UI-rendered frame)
    val responseText: String = "", // Used for writers
    val generatedDescription: String = "",
    val generatedHashtags: String = "",
    val generatedScript: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val userEmail: String = ""
)

@Dao
interface UserAccountDao {
    @Query("SELECT * FROM user_accounts WHERE email = :email LIMIT 1")
    suspend fun getAccount(email: String): UserAccount?

    @Query("SELECT * FROM user_accounts WHERE isLoggedIn = 1 LIMIT 1")
    fun getLoggedInAccount(): Flow<UserAccount?>

    @Query("SELECT * FROM user_accounts WHERE isLoggedIn = 1 LIMIT 1")
    suspend fun getLoggedInAccountSync(): UserAccount?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveAccount(account: UserAccount)

    @Query("UPDATE user_accounts SET isLoggedIn = 0")
    suspend fun logoutAll()

    @Query("UPDATE user_accounts SET isLoggedIn = 1 WHERE email = :email")
    suspend fun setLogin(email: String)
}

@Dao
interface VisionAiCreationDao {
    @Query("SELECT * FROM ai_creations ORDER BY timestamp DESC")
    fun getAllCreations(): Flow<List<VisionAiCreation>>

    @Query("SELECT * FROM ai_creations WHERE userEmail = :userEmail ORDER BY timestamp DESC")
    fun getCreationsByUser(userEmail: String): Flow<List<VisionAiCreation>>

    @Query("SELECT * FROM ai_creations WHERE type = :type ORDER BY timestamp DESC")
    fun getCreationsByType(type: String): Flow<List<VisionAiCreation>>

    @Query("SELECT * FROM ai_creations WHERE id = :id LIMIT 1")
    suspend fun getCreationById(id: Long): VisionAiCreation?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCreation(creation: VisionAiCreation): Long

    @Query("DELETE FROM ai_creations WHERE id = :id")
    suspend fun deleteCreation(id: Long)

    @Query("DELETE FROM ai_creations")
    suspend fun deleteAll()
}

@Database(entities = [UserAccount::class, VisionAiCreation::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userAccountDao(): UserAccountDao
    abstract fun visionAiCreationDao(): VisionAiCreationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "vision_ai_studio_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
