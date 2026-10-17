package com.spoolin.core.features.draft

import com.spoolin.core.features.account.persistence.AccountProtectionSettings
import com.spoolin.core.features.crypto.AeadCipher
import com.spoolin.core.features.crypto.KeyPurpose
import com.spoolin.core.features.crypto.KeyRegistry
import com.spoolin.core.features.crypto.MountedKeys
import com.spoolin.core.features.crypto.NonceAllocator
import com.spoolin.core.operations.receipts.OwnerReceipts
import org.slf4j.LoggerFactory
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
    ): DraftCryptography = DraftCryptography(registry, keys, nonces, AeadCipher())

    @Bean
    fun draftAlert(): com.spoolin.core.features.crypto.ProtectionAlert =
        com.spoolin.core.features.crypto.ProtectionAlert { row: UUID ->
            LoggerFactory.getLogger(DraftCryptography::class.java).error(
                "Protected draft unavailable row={}",
                row,
            )
        }

    @Bean
    fun protectedDrafts(
        dataSource: DataSource,
        draftCrypto: DraftCryptography,
        draftPayload: DraftPayload,
        alert: com.spoolin.core.features.crypto.ProtectionAlert,
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
