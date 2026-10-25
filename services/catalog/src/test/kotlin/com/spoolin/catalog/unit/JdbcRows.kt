package com.spoolin.catalog.unit

import io.grpc.stub.StreamObserver
import java.lang.reflect.Proxy
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import javax.sql.DataSource
import java.sql.Array as SqlArray

/** Scripted JDBC boundary; rows describe driver responses, not a replacement SQL engine. */
internal class JdbcRows(
    vararg results: List<Map<String, Any?>>,
) {
    private val results = ArrayDeque(results.toList())
    val writes = mutableListOf<List<Any?>>()
    val source: DataSource =
        proxy { name, _ ->
            when (name) {
                "getConnection" -> connection()
                else -> null
            }
        }

    private fun connection(): Connection {
        var autoCommit = true
        return proxy { name, args ->
            when (name) {
                "prepareStatement" -> statement()
                "getAutoCommit" -> autoCommit
                "setAutoCommit" -> {
                    autoCommit = args[0] as Boolean
                    null
                }
                "createArrayOf" -> sqlArray(args[1])
                else -> null
            }
        }
    }

    private fun statement(): PreparedStatement {
        val parameters = sortedMapOf<Int, Any?>()
        return proxy { name, args ->
            when {
                name == "executeQuery" -> rows(results.removeFirstOrNull() ?: error("No scripted JDBC response remains"))
                name == "executeUpdate" -> {
                    writes.add(parameters.values.toList())
                    1
                }
                name.startsWith("set") && args.firstOrNull() is Int -> {
                    parameters[args[0] as Int] = if (name == "setNull") null else args.getOrNull(1)
                    null
                }
                else -> null
            }
        }
    }

    private fun rows(values: List<Map<String, Any?>>): ResultSet {
        var index = -1
        var previous: Any? = null
        return proxy { name, args ->
            if (name == "next") return@proxy (++index < values.size)
            if (name == "wasNull") return@proxy previous == null
            if (name.startsWith("get")) {
                val key = args.firstOrNull()
                val row = values[index]
                previous = if (key is Int) row.values.elementAt(key - 1) else row[key]
                return@proxy when (name) {
                    "getString" -> previous?.toString()
                    "getInt" -> (previous as? Number)?.toInt() ?: 0
                    "getLong" -> (previous as? Number)?.toLong() ?: 0L
                    "getBoolean" -> previous as? Boolean ?: false
                    "getArray" -> previous?.let(::sqlArray)
                    else -> previous
                }
            }
            null
        }
    }

    private fun sqlArray(value: Any?): SqlArray = proxy { name, _ -> if (name == "getArray") value else null }
}

internal inline fun <reified T> proxy(crossinline invoke: (String, Array<out Any?>) -> Any?): T =
    Proxy.newProxyInstance(T::class.java.classLoader, arrayOf(T::class.java)) { _, method, args ->
        when (method.name) {
            "toString" -> "Scripted JDBC boundary"
            "hashCode" -> 1
            "equals" -> false
            else -> invoke(method.name, args ?: emptyArray())
        }
    } as T

internal class RpcResult<T> : StreamObserver<T> {
    val values = mutableListOf<T>()
    var failure: Throwable? = null
    var completed = false

    override fun onNext(value: T) {
        values.add(value)
    }

    override fun onError(value: Throwable) {
        failure = value
    }

    override fun onCompleted() {
        completed = true
    }

    fun value(): T {
        check(failure == null) { "RPC failed: $failure" }
        check(completed) { "RPC did not complete" }
        return values.single()
    }
}
