package com.spoolin.core.boundary.graphql

import graphql.schema.DataFetchingEnvironment

/** PolicyScalars keeps Long inputs as decimal strings; convert only at the JVM DTO boundary. */
object GraphqlLongArguments {
    fun required(
        env: DataFetchingEnvironment,
        name: String = "expectedVersion",
    ): Long = requireNotNull(env.getArgument<String>(name)).toLong()
}
