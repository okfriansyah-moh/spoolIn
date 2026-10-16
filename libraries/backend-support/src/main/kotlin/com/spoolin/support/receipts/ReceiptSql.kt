package com.spoolin.support.receipts

import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import javax.sql.DataSource

fun <T> DataSource.readRows(operation: (Connection) -> T): T = connection.use(operation)

fun <T> DataSource.committed(operation: (Connection) -> T): T = connection.use { it.commitResult(operation) }

private fun <T> Connection.commitResult(operation: (Connection) -> T): T {
    autoCommit = false
    var committed = false
    try {
        val result = operation(this)
        commit()
        committed = true
        return result
    } finally {
        if (!committed) rollback()
    }
}

fun Connection.executeBound(
    sql: String,
    vararg values: Any?,
): Int =
    prepareStatement(sql).use { statement ->
        statement.bind(values.toList())
        statement.executeUpdate()
    }

fun <T> Connection.queryBound(
    sql: String,
    values: List<Any?> = emptyList(),
    map: (ResultSet) -> T,
): List<T> =
    prepareStatement(sql).use { statement ->
        statement.bind(values)
        statement.executeQuery().use { it.collect(map) }
    }

private fun PreparedStatement.bind(values: List<Any?>) {
    values.forEachIndexed { index, value -> setObject(index + 1, value) }
}

private fun <T> ResultSet.collect(map: (ResultSet) -> T): List<T> =
    buildList {
        while (next()) add(map(this@collect))
    }
