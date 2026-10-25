package com.spoolin.core.features.session

import org.springframework.jdbc.datasource.AbstractDataSource
import java.lang.reflect.Proxy
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet

internal class SessionJdbcFixture(
    results: List<List<Map<String, Any?>>>,
) : AbstractDataSource() {
    private val responses = ArrayDeque(results)

    fun enqueue(vararg rows: Map<String, Any?>) {
        responses.addLast(rows.toList())
    }

    val writes = mutableListOf<Map<Int, Any?>>()
    var executions = 0
    var updateResult = 1
    var updates = 0
    var queries = 0
    var commits = 0
    private var autoCommit = true
    val jdbc: Connection =
        proxy(Connection::class.java) { name, args ->
            when (name) {
                "prepareStatement", "createStatement" -> statement()
                "getAutoCommit" -> autoCommit
                "setAutoCommit" -> {
                    autoCommit = args!![0] as Boolean
                    null
                }
                "commit" -> {
                    commits++
                    null
                }
                "isClosed" -> false
                "close", "rollback" -> null
                else -> null
            }
        }

    override fun getConnection(): Connection = jdbc

    override fun getConnection(
        username: String,
        password: String,
    ): Connection = jdbc

    private fun statement(): PreparedStatement {
        val parameters = mutableMapOf<Int, Any?>()
        return proxy(PreparedStatement::class.java) { name, args ->
            when (name) {
                "execute" -> { executions++; true }
                "executeQuery" -> {
                    queries++
                    rows(responses.removeFirst())
                }
                "executeUpdate" -> {
                    updates++
                    writes.add(parameters.toMap())
                    updateResult
                }
                "setObject", "setBytes", "setString", "setLong", "setInt", "setTimestamp" -> {
                    parameters[args!![0] as Int] = args[1]
                    null
                }
                "setNull" -> {
                    parameters[args!![0] as Int] = null
                    null
                }
                "close" -> null
                else -> null
            }
        }
    }

    private fun rows(values: List<Map<String, Any?>>): ResultSet {
        var index = -1
        return proxy(ResultSet::class.java) { name, args ->
            when (name) {
                "next" -> {
                    index++
                    index < values.size
                }
                "getObject", "getString", "getBytes", "getTimestamp" -> values[index][args!![0].toString()]
                "getLong" -> (values[index][args!![0].toString()] as? Number)?.toLong() ?: 0L
                "wasNull" -> true
                "getBoolean" -> values[index][args!![0].toString()] as? Boolean ?: false
                "close" -> null
                else -> null
            }
        }
    }

    companion object {
        private fun <T> proxy(
            type: Class<T>,
            call: (String, Array<out Any?>?) -> Any?,
        ): T =
            type.cast(
                Proxy.newProxyInstance(type.classLoader, arrayOf(type)) { _, method, args -> call(method.name, args) },
            )
    }
}
