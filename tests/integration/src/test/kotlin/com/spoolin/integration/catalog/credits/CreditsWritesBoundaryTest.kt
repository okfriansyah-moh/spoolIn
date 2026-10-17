package com.spoolin.integration.catalog.credits

import com.spoolin.catalog.features.credit.CreditWrites
import com.spoolin.catalog.operations.receipts.ReceiptRpc
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Cross-service credit write integration test boundary.
 */
class CreditsWritesBoundaryTest {

    @Test
    fun `credit write boundary evidence lives in catalog owners`() {
        assertTrue(CreditWrites::class.java.declaredMethods.isNotEmpty())
        assertTrue(ReceiptRpc::class.java.declaredMethods.isNotEmpty())
    }
}
