package com.spoolin.support.fixtures

import java.lang.reflect.Proxy
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Savepoint
import java.sql.Statement
import java.util.ArrayDeque
import javax.sql.DataSource

/** Responses come from an external JDBC boundary; SQL predicates are not reproduced here. */
class JdbcBoundary(
    var failure: Throwable? = null,
) {
    val queries = ArrayDeque<List<Map<String, Any?>>>()
    val updates = ArrayDeque<Int>()
    var commits = 0
    var rollbacks = 0
    var closed = 0
    val source: DataSource =
        proxy(DataSource::class.java) { method, _ ->
            when (method) {
                "getConnection" -> {
                    failure?.let { throw it }
                    connection
                }
                else -> null
            }
        }
    val connection: Connection =
        proxy(Connection::class.java) { method, _ ->
            when (method) {
                "createStatement" -> statement(Statement::class.java)
                "prepareStatement" -> statement(PreparedStatement::class.java)
                "commit" -> {
                    commits++
                    null
                }
                "rollback" -> {
                    rollbacks++
                    null
                }
                "close" -> {
                    closed++
                    null
                }
                "setSavepoint" -> proxy(Savepoint::class.java) { _, _ -> null }
                else -> null
            }
        }

    fun rows(vararg rows: Map<String, Any?>): JdbcBoundary = apply { queries.add(rows.toList()) }

    private fun <T> statement(type: Class<T>): T =
        proxy(type) { method, _ ->
            when (method) {
                "executeUpdate" -> if (updates.isEmpty()) 1 else updates.removeFirst()
                "executeQuery" -> result(if (queries.isEmpty()) emptyList() else queries.removeFirst())
                else -> null
            }
        }

    private fun result(rows: List<Map<String, Any?>>): ResultSet {
        var position = -1
        var last: Any? = null
        return proxy(ResultSet::class.java) { method, args ->
            when (method) {
                "next" -> {
                    position++
                    position < rows.size
                }
                "wasNull" -> last == null
                "getObject", "getString", "getBytes", "getTimestamp",
                "getInt", "getLong", "getDouble", "getBoolean",
                -> {
                    last = rows[position][args!![0].toString()]
                    when (method) {
                        "getInt" -> (last as? Number)?.toInt() ?: 0
                        "getLong" -> (last as? Number)?.toLong() ?: 0L
                        "getDouble" -> (last as? Number)?.toDouble() ?: 0.0
                        "getBoolean" -> last ?: false
                        else -> last
                    }
                }
                else -> null
            }
        }
    }
}

private fun <T> proxy(
    type: Class<T>,
    invoke: (String, Array<out Any?>?) -> Any?,
): T =
    type.cast(
        Proxy.newProxyInstance(type.classLoader, arrayOf(type)) { _, method, args ->
            when (method.name) {
                "toString" -> "Unit JDBC boundary"
                "hashCode" -> 1
                "equals" -> false
                else ->
                    invoke(method.name, args) ?: when (method.returnType) {
                        java.lang.Boolean.TYPE -> false
                        java.lang.Integer.TYPE -> 0
                        java.lang.Long.TYPE -> 0L
                        else -> null
                    }
            }
        },
    )
