package com.spoolin.core.features.draft

import com.spoolin.core.features.crypto.AeadCipher
import com.spoolin.core.features.crypto.KeyRegistry
import com.spoolin.core.features.crypto.MountedKeys
import com.spoolin.core.features.crypto.NonceAllocator
import com.spoolin.core.features.crypto.ProtectionAlert
import com.spoolin.core.features.crypto.ProtectionMetrics
import com.spoolin.core.operations.receipts.OwnerReceipts
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.UUID
import javax.sql.DataSource

/**
 * Wires the draft feature beans.
 *
 * Activated only when the receipt system and account-protection beans are present,
 * which guarantees that [MountedKeys] (including the DRAFT key) and [OwnerReceipts] are available.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnBean(OwnerReceipts::class, MountedKeys::class)
class DraftConfiguration {
    @Bean
    fun draftPayload(): DraftPayload = DraftPayload()

    @Bean
    fun draftCryptography(
        registry: KeyRegistry,
        keys: MountedKeys,
        nonces: NonceAllocator,
        @Qualifier("draftAlert") alert: ProtectionAlert,
    ): DraftCryptography = DraftCryptography(registry, keys, nonces, AeadCipher(), alert)

    @Bean
    fun draftAlert(metrics: ProtectionMetrics): ProtectionAlert =
        metrics.observing(
            ProtectionAlert { row: UUID ->
                LoggerFactory.getLogger(DraftCryptography::class.java).error("Protected draft unavailable row={}", row)
            },
        )

    @Bean
    fun protectedDrafts(
        dataSource: DataSource,
        draftCrypto: DraftCryptography,
        draftPayload: DraftPayload,
        @Qualifier("draftAlert") alert: ProtectionAlert,
    ): ProtectedDrafts = ProtectedDrafts(dataSource, draftCrypto, draftPayload, alert)

    @Bean
    fun saveDraft(
        protectedDrafts: ProtectedDrafts,
        draftCrypto: DraftCryptography,
        draftPayload: DraftPayload,
        receipts: OwnerReceipts,
    ): SaveDraft = SaveDraft(protectedDrafts, draftCrypto, draftPayload, receipts)

    @Bean
    fun discardDraft(
        protectedDrafts: ProtectedDrafts,
        receipts: OwnerReceipts,
    ): DiscardDraft = DiscardDraft(protectedDrafts, receipts)

    @Bean
    fun draftGraphql(
        protectedDrafts: ProtectedDrafts,
        saveDraft: SaveDraft,
        discardDraft: DiscardDraft,
    ): DraftGraphql = DraftGraphql(protectedDrafts, saveDraft, discardDraft)
}
