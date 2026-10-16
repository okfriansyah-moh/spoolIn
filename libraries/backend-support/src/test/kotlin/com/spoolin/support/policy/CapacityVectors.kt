package com.spoolin.support.policy

internal data class CapacityVector(
    val field: String,
    val upper: Int,
    val upperBytes: Int,
    val excessiveBytes: Int,
    val request: String,
)

internal object CapacityVectors {
    val all =
        listOf(
            CapacityVector(
                "personName",
                21797,
                131070,
                131076,
                "{\"query\":\"mutation(\$id:UUID!,\$p:String!,\$input:PersonInput!){createPerson(op" +
                    "erationId:\$id,policyRevision:\$p,input:\$input){problem{code}}}\",\"variables\":{" +
                    "\"id\":\"c1bb0e81-232b-5f83-9d73-a5273dcac001\",\"p\":\"000000000000000000000000000" +
                    "0000000000000000000000000000000000000\",\"input\":{\"name\":\"<candidate>\"}}}",
            ),
            CapacityVector(
                "title",
                21781,
                131069,
                131075,
                "{\"query\":\"mutation(\$id:UUID!,\$p:String!,\$input:MovieInput!){createMovie(oper" +
                    "ationId:\$id,policyRevision:\$p,input:\$input){problem{code}}}\",\"variables\":{\"i" +
                    "d\":\"c1bb0e81-232b-5f83-9d73-a5273dcac001\",\"p\":\"00000000000000000000000000000" +
                    "00000000000000000000000000000000000\",\"input\":{\"title\":\"<candidate>\",\"synopsi" +
                    "s\":\"\\u0001\",\"genreIds\":[\"c1bb0e81-232b-5f83-9d73-a5273dcac001\"],\"release\":{\"" +
                    "year\":2026}}}}",
            ),
            CapacityVector(
                "originalTitle",
                21777,
                131070,
                131076,
                "{\"query\":\"mutation(\$id:UUID!,\$p:String!,\$input:MovieInput!){createMovie(oper" +
                    "ationId:\$id,policyRevision:\$p,input:\$input){problem{code}}}\",\"variables\":{\"i" +
                    "d\":\"c1bb0e81-232b-5f83-9d73-a5273dcac001\",\"p\":\"00000000000000000000000000000" +
                    "00000000000000000000000000000000000\",\"input\":{\"title\":\"\\u0001\",\"synopsis\":\"\\" +
                    "u0001\",\"genreIds\":[\"c1bb0e81-232b-5f83-9d73-a5273dcac001\"],\"release\":{\"year\"" +
                    ":2026},\"originalTitle\":\"<candidate>\"}}}",
            ),
            CapacityVector(
                "synopsis",
                21781,
                131069,
                131075,
                "{\"query\":\"mutation(\$id:UUID!,\$p:String!,\$input:MovieInput!){createMovie(oper" +
                    "ationId:\$id,policyRevision:\$p,input:\$input){problem{code}}}\",\"variables\":{\"i" +
                    "d\":\"c1bb0e81-232b-5f83-9d73-a5273dcac001\",\"p\":\"00000000000000000000000000000" +
                    "00000000000000000000000000000000000\",\"input\":{\"title\":\"\\u0001\",\"synopsis\":\"<" +
                    "candidate>\",\"genreIds\":[\"c1bb0e81-232b-5f83-9d73-a5273dcac001\"],\"release\":{\"" +
                    "year\":2026}}}}",
            ),
            CapacityVector(
                "biography",
                21793,
                131067,
                131073,
                "{\"query\":\"mutation(\$id:UUID!,\$p:String!,\$input:PersonInput!){createPerson(op" +
                    "erationId:\$id,policyRevision:\$p,input:\$input){problem{code}}}\",\"variables\":{" +
                    "\"id\":\"c1bb0e81-232b-5f83-9d73-a5273dcac001\",\"p\":\"000000000000000000000000000" +
                    "0000000000000000000000000000000000000\",\"input\":{\"name\":\"\\u0001\",\"biography\":" +
                    "\"<candidate>\"}}}",
            ),
            CapacityVector(
                "creditRole",
                21797,
                131068,
                131074,
                "{\"query\":\"mutation(\$id:UUID!,\$p:String!,\$label:String!){createRole(operation" +
                    "Id:\$id,policyRevision:\$p,category:CAST,label:\$label){problem{code}}}\",\"varia" +
                    "bles\":{\"id\":\"c1bb0e81-232b-5f83-9d73-a5273dcac001\",\"p\":\"00000000000000000000" +
                    "00000000000000000000000000000000000000000000\",\"label\":\"<candidate>\"}}",
            ),
            CapacityVector(
                "characterName",
                21772,
                131071,
                131077,
                "{\"query\":\"mutation(\$id:UUID!,\$p:String!,\$v:Long!,\$input:CreditInput!){addCre" +
                    "dit(operationId:\$id,titleId:\$id,expectedVersion:\$v,policyRevision:\$p,input:\$" +
                    "input){problem{code}}}\",\"variables\":{\"id\":\"c1bb0e81-232b-5f83-9d73-a5273dcac" +
                    "001\",\"p\":\"0000000000000000000000000000000000000000000000000000000000000000\"," +
                    "\"input\":{\"personId\":\"c1bb0e81-232b-5f83-9d73-a5273dcac001\",\"roleId\":\"c1bb0e8" +
                    "1-232b-5f83-9d73-a5273dcac001\",\"characters\":[\"<candidate>\"]},\"v\":\"1\"}}",
            ),
            CapacityVector(
                "search",
                21827,
                131071,
                131077,
                "{\"query\":\"query(\$filter:SearchInput!){titles(filter:\$filter){nodes{id}}}\",\"v" +
                    "ariables\":{\"filter\":{\"text\":\"<candidate>\"}}}",
            ),
        )
}
