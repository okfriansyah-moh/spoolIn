package com.spoolin.core.features.ratelimit

import com.spoolin.core.features.account.application.AuthenticationAction
import com.spoolin.core.features.account.application.AuthenticationRequest
import com.spoolin.support.identity.RateProfile
import com.spoolin.support.identity.RateScope

class TrustedAdmission(
    private val limiter: RateAdmission,
) {
    fun check(
        action: AuthenticationAction,
        identity: String?,
        request: AuthenticationRequest,
    ) {
        val demands =
            when (action) {
                AuthenticationAction.REGISTER -> listOf(source(request, RateProfile.REGISTER))
                AuthenticationAction.SIGN_IN ->
                    listOf(
                        RateDemand(
                            RateProfile.SIGNIN_ACCOUNT,
                            RateScope.ACCOUNT_IDENTIFIER,
                            identity ?: throw RateUnavailable(),
                        ),
                        source(request, RateProfile.SIGNIN_SOURCE),
                    )
                AuthenticationAction.SIGN_OUT -> listOf(credential(request, RateProfile.MUTATION))
            }
        request.charge(demands, limiter::admit)
    }

    fun reads(
        request: AuthenticationRequest,
        public: Boolean,
        protected: Boolean,
    ) {
        val demands =
            buildList {
                if (public || request.credentialId == null) add(source(request, RateProfile.READ))
                if (protected && request.credentialId != null) add(credential(request, RateProfile.READ))
            }
        request.charge(demands, limiter::admit)
    }

    fun mutation(request: AuthenticationRequest) =
        request.charge(
            listOf(credential(request, RateProfile.MUTATION)),
            limiter::admit,
        )

    private fun source(
        request: AuthenticationRequest,
        profile: RateProfile,
    ): RateDemand = RateDemand(profile, RateScope.SOURCE, request.sourceAddress ?: throw RateUnavailable())

    private fun credential(
        request: AuthenticationRequest,
        profile: RateProfile,
    ): RateDemand =
        RateDemand(
            profile,
            RateScope.CREDENTIAL,
            request.credentialId?.toString() ?: throw RateUnavailable(),
        )
}
