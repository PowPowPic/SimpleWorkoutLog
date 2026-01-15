package com.poweder.simpleworkoutlog.data.dao

import androidx.room.*
import com.poweder.simpleworkoutlog.data.entity.ExerciseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercises ORDER BY sortOrder, id")
    fun getAllExercises(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE workoutType = :type ORDER BY sortOrder, id")
    fun getExercisesByType(type: String): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun getExerciseById(id: Long): ExerciseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(exercise: ExerciseEntity): Long

    @Update
    suspend fun update(exercise: ExerciseEntity)

    @Delete
    suspend fun delete(exercise: ExerciseEntity)

    @Query("DELETE FROM exercises WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM exercises")
    suspend fun deleteAll()

    @Query("SELECT MAX(sortOrder) FROM exercises WHERE workoutType = :type")
    suspend fun getMaxSortOrder(type: String): Int?

    // ===== テンプレート管理用（Upsert方式） =====

    /**
     * templateKeyで既存テンプレを検索
     * ※ CASCADEによるデータ消失を防ぐため、deleteTemplates()は使わない
     */
    @Query("SELECT * FROM exercises WHERE templateKey = :templateKey LIMIT 1")
    suspend fun getByTemplateKey(templateKey: String): ExerciseEntity?

    /**
     * テンプレートの存在確認
     */
    @Query("SELECT COUNT(*) FROM exercises WHERE templateKey = :templateKey")
    suspend fun existsByTemplateKey(templateKey: String): Int

    /**
     * テンプレートをUpsert（既存ならsortOrderのみ更新、なければ挿入）
     * ※ idを維持することで外部キー参照を壊さない
     */
    @Transaction
    suspend fun upsertTemplate(exercise: ExerciseEntity) {
        val existing = getByTemplateKey(exercise.templateKey ?: return)
        if (existing != null) {
            // 既存テンプレがある場合：sortOrderだけ更新（idは維持）
            update(existing.copy(
                sortOrder = exercise.sortOrder,
                workoutType = exercise.workoutType
            ))
        } else {
            // 新規テンプレの場合：挿入
            insert(exercise)
        }
    }

    @Query("SELECT COUNT(*) FROM exercises")
    suspend fun countAll(): Int

    @Query("SELECT COUNT(*) FROM exercises WHERE isTemplate = 1")
    suspend fun countTemplates(): Int

    // ===== 危険：CASCADEでデータ消失するため使用禁止 =====
    // @Query("DELETE FROM exercises WHERE isTemplate = 1")
    // suspend fun deleteTemplates()
}
