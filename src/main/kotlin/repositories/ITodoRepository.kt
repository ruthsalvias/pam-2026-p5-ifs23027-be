package org.delcom.repositories

import org.delcom.entities.Todo

interface  ITodoRepository {
    suspend fun getAll(userId: String, search: String, page: Int, perPage: Int, isComplete: Boolean?, urgency: Int?): List<Todo>
    // Fungsi baru untuk statistik Home
    suspend fun getHomeStats(userId: String): Map<String, Long>
    suspend fun getById(todoId: String): Todo?
    suspend fun create(todo: Todo): String
    suspend fun update(userId: String, todoId: String, newTodo: Todo): Boolean
    suspend fun delete(userId: String, todoId: String) : Boolean
}