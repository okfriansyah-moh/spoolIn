package com.spoolin.catalog.features.taxonomy.read

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.testcontainers.postgresql.PostgreSQLContainer

class CatalogFilterOptionsRulesTest {
    @Test
    fun `filter choices contain only active title genres and actual release dates`() {
        PostgreSQLContainer("postgres:18.6-bookworm").use { postgres ->
            postgres.start()
            val config =
                HikariConfig().apply {
                    jdbcUrl = postgres.jdbcUrl
                    username = postgres.username
                    password = postgres.password
                }
            HikariDataSource(config).use { pool ->
                createFixture(pool)
                val reads = TaxonomyReads(pool)
                assertTrue(reads.catalogFilterOptions().genres.isEmpty())
                assertTrue(reads.catalogFilterOptions().releaseYears.isEmpty())
                seedFixture(pool)
                val options = reads.catalogFilterOptions()
                assertEquals(listOf("Drama", "Year only"), options.genres.map { it.label })
                assertEquals(listOf(2006, 1999), options.releaseYears.map { it.year })
                assertEquals(listOf(1, 3), options.releaseYears[0].months)
                assertEquals(emptyList<Int>(), options.releaseYears[1].months)
                assertEquals(4, reads.listGenres().size, "Management still sees unused genres")
                assertRpcProjection(pool, reads)
            }
        }
    }

    private fun assertRpcProjection(
        pool: HikariDataSource,
        taxonomy: TaxonomyReads,
    ) {
        val context =
            com.spoolin.catalog.boundary.grpc.PublicReadContext(
                com.spoolin.catalog.features.search
                    .CatalogCursor(ByteArray(32)),
                com.spoolin.catalog.features.search
                    .PeopleCursor(ByteArray(32)),
                java.time.Clock.systemUTC(),
                {
                    com.spoolin.support.policy.PolicyEnvironment
                        .resolve { null }
                },
            )
        val reads =
            com.spoolin.catalog.boundary.grpc.PublicCatalogReads(
                com.spoolin.catalog.features.search
                    .TitleSearchSql(pool),
                com.spoolin.catalog.features.person.read
                    .PersonSearch(pool),
                taxonomy,
                com.spoolin.catalog.features.provenance
                    .ProvenanceReads(pool),
                context,
            )
        var completed = false
        reads.catalogFilterOptions(
            object : io.grpc.stub.StreamObserver<spoolin.v1.Spoolin.CatalogFilterOptions> {
                override fun onNext(value: spoolin.v1.Spoolin.CatalogFilterOptions) {
                    assertEquals(listOf("Drama", "Year only"), value.genresList.map { it.label })
                    assertEquals(listOf(2006, 1999), value.releaseYearsList.map { it.year })
                    assertEquals(listOf(1, 3), value.releaseYearsList[0].monthsList)
                    assertTrue(value.releaseYearsList[1].monthsList.isEmpty())
                }

                override fun onError(error: Throwable) = throw AssertionError(error)

                override fun onCompleted() {
                    completed = true
                }
            },
        )
        assertTrue(completed)
    }

    private fun createFixture(pool: HikariDataSource) {
        pool.connection.use { connection ->
            connection.createStatement().use { sql ->
                sql.execute("CREATE TABLE genre(id uuid PRIMARY KEY, label text, normalized_label text)")
                sql.execute(
                    "CREATE TABLE title(id uuid PRIMARY KEY, status text, " +
                        "release_year smallint, release_month smallint)",
                )
                sql.execute("CREATE TABLE title_genre(title_id uuid, genre_id uuid)")
            }
        }
    }

    private fun seedFixture(pool: HikariDataSource) {
        pool.connection.use { connection ->
            connection.createStatement().use { sql ->
                sql.execute(
                    "INSERT INTO genre SELECT md5(n::text)::uuid, label, lower(label) FROM " +
                        "(VALUES (1, 'Drama'), (2, 'Unused'), (3, 'Archived only'), (4, 'Year only')) g(n, label)",
                )
                sql.execute(
                    "INSERT INTO title SELECT md5(('title' || n)::text)::uuid, status, year, month FROM " +
                        "(VALUES (1, 'ACTIVE', 2006, 3), (2, 'ACTIVE', 2006, 1), " +
                        "(3, 'ACTIVE', 2006, 3), (4, 'ACTIVE', 1999, NULL), " +
                        "(5, 'ARCHIVED', 1800, 12), (6, 'ACTIVE', NULL, NULL)) t(n, status, year, month)",
                )
                sql.execute(
                    "INSERT INTO title_genre VALUES (md5('title1')::uuid, md5('1')::uuid), " +
                        "(md5('title2')::uuid, md5('1')::uuid), " +
                        "(md5('title4')::uuid, md5('4')::uuid), " +
                        "(md5('title5')::uuid, md5('3')::uuid)",
                )
            }
        }
    }
}
