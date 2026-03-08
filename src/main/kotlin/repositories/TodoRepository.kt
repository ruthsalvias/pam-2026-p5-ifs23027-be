package org.delcom.repositories

import org.delcom.dao.TodoDAO
import org.delcom.entities.Todo
import org.delcom.helpers.suspendTransaction
import org.delcom.helpers.todoDAOToModel
import org.delcom.tables.TodoTable
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.lowerCase
import java.util.*

class TodoRepository : ITodoRepository {
    override suspend fun getAll(userId: String, search: String, page: Int, perPage: Int, isComplete: Boolean?, urgency: Int?): List<Todo> = suspendTransaction {
        var query = TodoDAO.find {
            TodoTable.userId eq UUID.fromString(userId)
        }

        // Filter berdasarkan search (cari di title atau description)
        if (search.isNotBlank()) {
            query = TodoDAO.find {
                (TodoTable.userId eq UUID.fromString(userId)) and
                ((TodoTable.title.lowerCase() like "%${search.lowercase()}%") or
                 (TodoTable.description.lowerCase() like "%${search.lowercase()}%"))
            }
        }

        // Filter berdasarkan status completion (isComplete)
        if (isComplete != null) {
            query = TodoDAO.find {
                (TodoTable.userId eq UUID.fromString(userId)) and
                (TodoTable.isDone eq isComplete)
            }
            
            // Terapkan search filter jika ada
            if (search.isNotBlank()) {
                query = query.filter {
                    it.title.lowercase().contains(search.lowercase()) ||
                    it.description.lowercase().contains(search.lowercase())
                }.asFlow().toList().let { filteredTodos ->
                    // Convert back to DAO to apply ordering
                    filteredTodos.asSequence()
                }
            }
        }

        // Filter berdasarkan urgency
        if (urgency != null && urgency in 1..3) {
            query = TodoDAO.find {
                (TodoTable.userId eq UUID.fromString(userId)) and
                (TodoTable.urgency eq urgency)
            }
        }

        // Terapkan ordering
        val offset = (page - 1) * perPage
        
        query.orderBy(TodoTable.createdAt to SortOrder.DESC)
            .limit(perPage, offset.toLong())
            .map(::todoDAOToModel)
    }

    override suspend fun getHomeStats(userId: String): Map<String, Long> = suspendTransaction {
        val total = TodoDAO.find { TodoTable.userId eq UUID.fromString(userId) }.count()
        val completed = TodoDAO.find { (TodoTable.userId eq UUID.fromString(userId)) and (TodoTable.isDone eq true) }.count()
        val active = total - completed

        mapOf("total" to total, "complete" to completed, "active" to active)
    }

    override suspend fun getById(todoId: String): Todo? = suspendTransaction {
        TodoDAO
            .find {
                (TodoTable.id eq UUID.fromString(todoId))
            }
            .limit(1)
            .map(::todoDAOToModel)
            .firstOrNull()
    }

    override suspend fun create(todo: Todo): String = suspendTransaction {
        val todoDAO = TodoDAO.new {
            userId = UUID.fromString(todo.userId)
            title = todo.title
            description = todo.description
            cover = todo.cover
            isDone = todo.isDone
            urgency = todo.urgency
            createdAt = todo.createdAt
            updatedAt = todo.updatedAt
        }

        todoDAO.id.value.toString()
    }

    override suspend fun update(userId: String, todoId: String, newTodo: Todo): Boolean = suspendTransaction {
        val todoDAO = TodoDAO
            .find {
                (TodoTable.id eq UUID.fromString(todoId)) and
                        (TodoTable.userId eq UUID.fromString(userId))
            }
            .limit(1)
            .firstOrNull()

        if (todoDAO != null) {
            todoDAO.title = newTodo.title
            todoDAO.description = newTodo.description
            todoDAO.cover = newTodo.cover
            todoDAO.isDone = newTodo.isDone
            todoDAO.urgency = newTodo.urgency
            todoDAO.updatedAt = newTodo.updatedAt
            true
        } else {
            false
        }
    }

    override suspend fun delete(userId: String, todoId: String): Boolean = suspendTransaction {
        val rowsDeleted = TodoTable.deleteWhere {
            (TodoTable.id eq UUID.fromString(todoId)) and
                    (TodoTable.userId eq UUID.fromString(userId))
        }
        rowsDeleted >= 1
    }

}