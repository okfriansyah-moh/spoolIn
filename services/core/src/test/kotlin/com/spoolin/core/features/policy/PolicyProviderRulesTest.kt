package com.spoolin.core.features.policy

import com.google.protobuf.Empty
import com.spoolin.support.policy.ConfigurationSources
import com.spoolin.support.policy.PolicySettings
import io.grpc.Status
import io.grpc.stub.StreamObserver
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import spoolin.v1.Spoolin.PolicySnapshot

class PolicyProviderRulesTest {
    private val provider = PolicyProvider(PolicySettings.resolve(ConfigurationSources()))

    @Test
    fun `ATDD-255-T1-R01 isolated protobuf projection has exact current fields`() {
        val result = provider.protobuf()
        assertEquals(provider.policy.revision, result.revision)
        assertEquals(
            setOf("revision", "text_bounds", "artwork", "request_limits"),
            result.allFields.keys
                .map { it.name }
                .toSet(),
        )
        assertEquals(13, result.textBoundsCount)
        assertEquals(5242880, result.artwork.maxInputBytes)
        assertEquals(40000000, result.artwork.maxPixels)
        assertEquals(100, result.requestLimits.maxPage)
        assertEquals(131072, result.requestLimits.maxMetadataBytes)
        assertEquals(200, result.requestLimits.maxSearchCodePoints)
    }

    @Test
    fun `T009 gateway unavailable authority fails closed without positive connector claim`() {
        var failure: Throwable? = null
        var response: PolicySnapshot? = null
        var completed = false
        PolicyGateway(provider, PolicyAccess { false }).getPolicy(
            Empty.getDefaultInstance(),
            object : StreamObserver<PolicySnapshot> {
                override fun onNext(value: PolicySnapshot) {
                    response = value
                }

                override fun onError(error: Throwable) {
                    failure = error
                }

                override fun onCompleted() {
                    completed = true
                }
            },
        )
        assertNull(response)
        assertFalse(completed)
        assertEquals(Status.Code.UNAVAILABLE, Status.fromThrowable(requireNotNull(failure)).code)
    }

    @Test
    fun `T009 controlled verified connector projector returns same snapshot`() {
        var response: PolicySnapshot? = null
        var completed = false
        PolicyGateway(provider, PolicyAccess { true }).getPolicy(
            Empty.getDefaultInstance(),
            object : StreamObserver<PolicySnapshot> {
                override fun onNext(value: PolicySnapshot) {
                    response = value
                }

                override fun onError(error: Throwable): Unit = throw error

                override fun onCompleted() {
                    completed = true
                }
            },
        )
        assertEquals(provider.protobuf(), response)
        assertTrue(completed)
    }
}
