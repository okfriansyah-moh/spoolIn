package com.spoolin.core.features.artworkproxy

/** Browser-facing artwork stage separation (AC-038 / AC-038-C02). */
object ArtworkProgressRules {
    enum class Stage { IDLE, TRANSFERRING, PROCESSING, SAVED, FAILED, UNCERTAIN }

    fun stageFromBytes(
        transferred: Long,
        declared: Long,
        processing: Boolean,
        attached: Boolean,
    ): Stage =
        when {
            attached -> Stage.SAVED
            transferred > 0 && transferred < declared -> Stage.TRANSFERRING
            else -> postTransferStage(transferred, declared, processing)
        }

    private fun postTransferStage(
        transferred: Long,
        declared: Long,
        processing: Boolean,
    ): Stage =
        when {
            processing && transferred >= declared -> Stage.PROCESSING
            transferred >= declared -> Stage.PROCESSING
            else -> Stage.IDLE
        }

    fun transferCompleteIsNotSaved(
        transferred: Long,
        declared: Long,
        attached: Boolean,
    ): Boolean = transferred >= declared && !attached
}
