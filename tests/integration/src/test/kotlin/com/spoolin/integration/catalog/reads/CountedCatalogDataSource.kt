package com.spoolin.integration.catalog.reads

import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.sql.Connection
import java.sql.PreparedStatement
import java.util.concurrent.CopyOnWriteArrayList
import javax.sql.DataSource

/** Observes completed real JDBC executions; all calls and results delegate to PostgreSQL. */
internal class CountedCatalogDataSource(private val source: DataSource) : DataSource by source {
    val completed = CopyOnWriteArrayList<String>()

    override fun getConnection(): Connection = connection(source.connection)

    override fun getConnection(username: String, password: String): Connection =
        connection(source.getConnection(username, password))

    private fun connection(target: Connection): Connection =
        Connection::class.java.cast(
            Proxy.newProxyInstance(javaClass.classLoader, arrayOf(Connection::class.java)) { _, method, args ->
                val result = invoke(target, method, args)
                if (method.name == "prepareStatement" && result is PreparedStatement) {
                    statement(result, requireNotNull(args).first().toString())
                } else {
                    result
                }
            },
        )

    private fun statement(target: PreparedStatement, sql: String): PreparedStatement =
        PreparedStatement::class.java.cast(
            Proxy.newProxyInstance(javaClass.classLoader, arrayOf(PreparedStatement::class.java)) { _, method, args ->
                val result = invoke(target, method, args)
                if (method.name == "executeQuery") completed.add(sql)
                result
            },
        )

    private fun invoke(target: Any, method: Method, args: Array<out Any?>?): Any? =
        try {
            method.invoke(target, *(args ?: emptyArray()))
        } catch (failure: InvocationTargetException) {
            throw failure.targetException
        }
}
