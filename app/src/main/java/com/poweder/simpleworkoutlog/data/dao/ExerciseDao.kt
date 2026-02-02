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
    
    /**
     * 複数の種目を一括更新（並び替え用）
     */
    @Update
    suspend fun updateAll(exercises: List<ExerciseEntity>)

    @Delete
    suspend fun delete(exercise: ExerciseEntity)

    @Query("DELETE FROM exercises WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM exercises")
    suspend fun deleteAll()

    @Query("SELECT MAX(sortOrder) FROM exercises WHERE workoutType = :type")
    suspend fun getMaxSortOrder(type: String): Int?
    
    /**
     * 指定IDの種目のsortOrderを更新
     */
    @Query("UPDATE exercises SET sortOrder = :sortOrder WHERE id = :id")
    suspend fun updateSortOrder(id: Long, sortOrder: Int)

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
     * テンプレートをUpsert（なければ挿入、あれば何もしない）
     * 
     * ★重要：既存テンプレートのsortOrderは変更しない
     * ユーザーが並び替えたsortOrderを維持するため、既存テンプレートがある場合は
     * 何も更新せずスキップする
     */
    @Transaction
    suspend fun upsertTemplate(exercise: ExerciseEntity) {
        val templateKey = exercise.templateKey ?: return
        val existing = getByTemplateKey(templateKey)
        if (existing == null) {
            // 新規テンプレの場合のみ挿入
            insert(exercise)
        }
        // 既存テンプレがある場合は何もしない（sortOrderを上書きしない）
    }

    @Query("SELECT COUNT(*) FROM exercises")
    suspend fun countAll(): Int

    @Query("SELECT COUNT(*) FROM exercises WHERE isTemplate = 1")
    suspend fun countTemplates(): Int

    // ===== 危険：CASCADEでデータ消失するため使用禁止 =====
    // @Query("DELETE FROM exercises WHERE isTemplate = 1")
    // suspend fun deleteTemplates()
}
