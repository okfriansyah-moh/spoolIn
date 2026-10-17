package com.spoolin.integration.catalog.people

import com.spoolin.catalog.features.person.write.PersonWrites
import com.spoolin.catalog.operations.receipts.ReceiptRpc
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Cross-service person write integration test boundary.
 */
class PeopleWritesBoundaryTest {

    @Test
    fun `person write boundary evidence lives in catalog owners`() {
        assertTrue(PersonWrites::class.java.declaredMethods.isNotEmpty())
        assertTrue(ReceiptRpc::class.java.declaredMethods.isNotEmpty())
    }
}
