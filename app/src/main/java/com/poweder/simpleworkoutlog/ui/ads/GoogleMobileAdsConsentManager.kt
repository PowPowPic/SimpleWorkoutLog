package com.poweder.simpleworkoutlog.ui.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.FormError
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Google UMP consent manager.
 *
 * Consent information is refreshed at every app launch. Ads are not requested until
 * the update/form flow has finished and ConsentInformation.canRequestAds() is true.
 */
class GoogleMobileAdsConsentManager private constructor(context: Context) {

    companion object {
        private const val TAG = "GoogleMobileAdsConsent"

        @Volatile
        private var instance: GoogleMobileAdsConsentManager? = null

        fun getInstance(context: Context): GoogleMobileAdsConsentManager =
            instance ?: synchronized(this) {
                instance ?: GoogleMobileAdsConsentManager(context.applicationContext)
                    .also { instance = it }
            }
    }

    private val appContext = context.applicationContext
    private val consentInformation: ConsentInformation =
        UserMessagingPlatform.getConsentInformation(appContext)

    private val mobileAdsInitializationStarted = AtomicBoolean(false)

    private val _consentGatheringComplete = MutableStateFlow(false)
    val consentGatheringComplete: StateFlow<Boolean> =
        _consentGatheringComplete.asStateFlow()

    private val _canRequestAds = MutableStateFlow(false)
    val canRequestAds: StateFlow<Boolean> = _canRequestAds.asStateFlow()

    private val _privacyOptionsRequired = MutableStateFlow(isPrivacyOptionsRequired())
    val privacyOptionsRequired: StateFlow<Boolean> =
        _privacyOptionsRequired.asStateFlow()

    private val _mobileAdsInitialized = MutableStateFlow(false)
    val mobileAdsInitialized: StateFlow<Boolean> =
        _mobileAdsInitialized.asStateFlow()

    /** Refresh consent information and show a consent form when required. */
    fun gatherConsent(
        activity: Activity,
        onConsentGatheringComplete: (FormError?) -> Unit = {}
    ) {
        _consentGatheringComplete.value = false

        val params = ConsentRequestParameters.Builder()
            .setTagForUnderAgeOfConsent(false)
            .build()

        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                refreshState()
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    refreshState()
                    _consentGatheringComplete.value = true
                    onConsentGatheringComplete(formError)
                }
            },
            { requestConsentError ->
                Log.w(
                    TAG,
                    "Consent information update failed: " +
                        "${requestConsentError.errorCode} ${requestConsentError.message}"
                )
                // A valid choice from an earlier session may still permit ad requests.
                refreshState()
                _consentGatheringComplete.value = true
                onConsentGatheringComplete(requestConsentError)
            }
        )
    }

    /** Initialize Google Mobile Ads once this launch's consent flow permits requests. */
    fun initializeMobileAds() {
        if (!_consentGatheringComplete.value || !_canRequestAds.value) return
        if (_mobileAdsInitialized.value) return
        if (!mobileAdsInitializationStarted.compareAndSet(false, true)) return

        MobileAds.initialize(appContext) {
            _mobileAdsInitialized.value = true
            Log.d(TAG, "Google Mobile Ads SDK initialized")
        }
    }

    /** Show the privacy-options form after an explicit user action. */
    fun showPrivacyOptionsForm(
        activity: Activity,
        onFormDismissed: (FormError?) -> Unit = {}
    ) {
        // Temporarily close the ad gate so already loaded ads are discarded while the
        // user reviews or changes their privacy choices.
        _consentGatheringComplete.value = false
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError ->
            refreshState()
            _consentGatheringComplete.value = true
            onFormDismissed(formError)
        }
    }

    private fun refreshState() {
        _canRequestAds.value = consentInformation.canRequestAds()
        _privacyOptionsRequired.value = isPrivacyOptionsRequired()
    }

    private fun isPrivacyOptionsRequired(): Boolean =
        consentInformation.privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
}
