package com.spoolin.support.policy

private const val POOL_MAXIMUM = 8
private const val POOL_MINIMUMIDLE = 1
private const val CONNECTTIMEOUT = 2000
private const val STATEMENTTIMEOUT = 3000
private const val LOCKTIMEOUT = 2000
private const val IDLETRANSACTIONTIMEOUT = 5000
private const val MIGRATIONSTATEMENTTIMEOUT = 120000
private const val COREHTTPSPORT = 8443
private const val MCPTRANSPORTPORT = 8787
private const val MCPPRIVATEEXPORTERPORT = 8788
private const val AES_KEY_SIZE = 32
private const val ACCOUNT_DRAFT_PLAINTEXT_ENVELOPE = 65536
private const val VERIFY_MAP_ENTRIES_MAXIMUM = 4
private const val PREVIOUS_VERIFIER_MINIMUM_RETENTION_SECONDS = 60
private const val ASSERTION_LIFETIME_MAXIMUM_SECONDS = 30
private const val ASSERTION_METADATA_MAXIMUM_BYTES = 4096
private const val SKEW_MAXIMUM_SECONDS = 2
private const val NEW_PASSWORD_BYTE_CEILING = 512
private const val SOURCEURL = 2048
private const val REUSEBASIS = 4000
private const val MAXDIMENSIONPX = 8192
private const val MAXPIXELS = 40000000
private const val MAXDECODEDBYTES = 160000000
private const val PROCESSORRSS = 268435456
private const val PROCESSORCPU = 5
private const val PROCESSORWALL = 10
private const val PROCESSORTEMPDISK = 33554432
private const val MAXOUTPUTBYTES = 8388608
private const val GENRE_COUNT_MAXIMUM = 20
private const val BCP47_MAXIMUM_CHARACTERS = 35
private const val RUNTIME_MINIMUM_MINUTES = 1
private const val RUNTIME_MAXIMUM_MINUTES = 100000
private const val YEAR_MINIMUM = 1
private const val YEAR_MAXIMUM = 9999
private const val GRPC_METADATA_MESSAGE = 262144
private const val GRPC_LIST_REPLY = 1048576
private const val UPLOAD_CHUNK = 65536
private const val UPLOAD_MESSAGE = 131072
private const val OLD_KEY_READ_OVERLAP = 60

object CompatibilityFacts {
    val selected: Map<String, Any> =
        java.util.Collections.unmodifiableMap(
            mapOf(
                "pool.maximum" to POOL_MAXIMUM,
                "pool.minimumIdle" to POOL_MINIMUMIDLE,
                "connectTimeout" to CONNECTTIMEOUT,
                "statementTimeout" to STATEMENTTIMEOUT,
                "lockTimeout" to LOCKTIMEOUT,
                "idleTransactionTimeout" to IDLETRANSACTIONTIMEOUT,
                "migrationStatementTimeout" to MIGRATIONSTATEMENTTIMEOUT,
                "CoreHTTPSport" to COREHTTPSPORT,
                "MCPtransportPort" to MCPTRANSPORTPORT,
                "MCPprivateExporterPort" to MCPPRIVATEEXPORTERPORT,
                "AES key size" to AES_KEY_SIZE,
                "protected file mode" to "0600",
                "account/draft plaintext envelope" to ACCOUNT_DRAFT_PLAINTEXT_ENVELOPE,
                "verify map entries maximum" to VERIFY_MAP_ENTRIES_MAXIMUM,
                "previous verifier minimum retention seconds" to PREVIOUS_VERIFIER_MINIMUM_RETENTION_SECONDS,
                "assertion lifetime maximum seconds" to ASSERTION_LIFETIME_MAXIMUM_SECONDS,
                "assertion metadata maximum bytes" to ASSERTION_METADATA_MAXIMUM_BYTES,
                "skew maximum seconds" to SKEW_MAXIMUM_SECONDS,
                "jitter algorithm" to "uniform[0,min(cap,base*2^(attempt-1))]",
                "new-password byte ceiling" to NEW_PASSWORD_BYTE_CEILING,
                "sourceUrl" to SOURCEURL,
                "reuseBasis" to REUSEBASIS,
                "maxDimensionPx" to MAXDIMENSIONPX,
                "maxPixels" to MAXPIXELS,
                "maxDecodedBytes" to MAXDECODEDBYTES,
                "processorRss" to PROCESSORRSS,
                "processorCpu" to PROCESSORCPU,
                "processorWall" to PROCESSORWALL,
                "processorTempDisk" to PROCESSORTEMPDISK,
                "maxOutputBytes" to MAXOUTPUTBYTES,
                "genre count maximum" to GENRE_COUNT_MAXIMUM,
                "BCP47 maximum characters" to BCP47_MAXIMUM_CHARACTERS,
                "runtime minimum minutes" to RUNTIME_MINIMUM_MINUTES,
                "runtime maximum minutes" to RUNTIME_MAXIMUM_MINUTES,
                "year minimum" to YEAR_MINIMUM,
                "year maximum" to YEAR_MAXIMUM,
                "gRPC metadata message" to GRPC_METADATA_MESSAGE,
                "gRPC list reply" to GRPC_LIST_REPLY,
                "upload chunk" to UPLOAD_CHUNK,
                "upload message" to UPLOAD_MESSAGE,
                "old-key read overlap" to OLD_KEY_READ_OVERLAP,
            ),
        )

    fun validate(
        name: String,
        observed: Any,
    ) {
        if (observed != selected[name]) throw ConfigurationViolation(name, "Use the selected compatibility contract.")
    }

    fun keyId(
        value: String,
        setting: String = "KEY_ID",
    ) {
        if (!value.matches(
                Regex("[A-Za-z0-9_-]{1,64}"),
            )
        ) {
            throw ConfigurationViolation(setting, "Supply a bounded version identifier.")
        }
    }

    fun independentPurposeKeys(keys: Map<String, ByteArray>) {
        if (keys.values.any { it.size != AES_KEY_SIZE } ||
            keys.values.toList().let { values ->
                values.indices.any { i -> (i + 1 until values.size).any { j -> values[i].contentEquals(values[j]) } }
            }
        ) {
            throw ConfigurationViolation("PURPOSE_KEYS", "Use independent 32-byte material for each purpose.")
        }
    }
}
