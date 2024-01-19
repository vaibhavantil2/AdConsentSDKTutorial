package com.edgedevstudio.consenttutorial

import android.os.Bundle
import android.os.PersistableBundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import com.google.ads.consent.*
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.*
import com.google.android.gms.ads.reward.RewardedVideoAd
import java.net.URL

class MainActivity : AppCompatActivity() {
    private var mShowPersonlAds = true
    private var mAdView: AdView? = null
    private var mInterstitialAd: InterstitialAd? = null
    private var mRewardedVideoAd: RewardedVideoAd? = null
    private var consentForm: ConsentForm? = null
    private val SHOW_PERSONAL_ADS_KEY = "show.personal.ads.key"
    private val device_id 

    val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (savedInstanceState != null)
            mShowPersonlAds = savedInstanceState.getBoolean(SHOW_PERSONAL_ADS_KEY)
        mAdView = findViewById(R.id.adViewMain)
        initialiseAds()
        checkAdConsent()
    }

    fun showInterstitialAd(view: View) {
        if (mInterstitialAd!!.isLoaded)
            mInterstitialAd?.show()
        else
            requestNewInterstitial()
    }

    fun showRewardedVideoAd(view: View) {
        if (mRewardedVideoAd!!.isLoaded)
            mRewardedVideoAd?.show()
        else
            loadRewardedVideoAd()
    }

    private fun checkAdConsent() {
        val consentInformation = ConsentInformation.getInstance(this)
        val publisherIds = Array(1, { "pub-9297690518647609" })
        consentInformation.requestConsentInfoUpdate(publisherIds, object : ConsentInfoUpdateListener {
            override fun onConsentInfoUpdated(consentStatus: ConsentStatus) {
                when (consentStatus) {
                    ConsentStatus.PERSONALIZED -> loadAds(true)
                    ConsentStatus.NON_PERSONALIZED -> loadAds(false)
                    ConsentStatus.UNKNOWN -> displayConsentForm()
                }
                Log.d(TAG, "onConsentInfoUpdated, Consent Status = ${consentStatus.name}")
            }

            override fun onFailedToUpdateConsentInfo(errorDescription: String) {
                Log.d(TAG, "onFailedToUpdateConsentInfo - $errorDescription")
            }
        })
    }

    private fun loadAds(showPersonlAds: Boolean) {
        mShowPersonlAds = showPersonlAds
        val build: AdRequest
        if (mShowPersonlAds)
            build = AdRequest.Builder()
                    .addNetworkExtrasBundle(AdMobAdapter::class.java, getNonPersonalizedAdsBundle())
                    .build()
        else
            build = AdRequest.Builder().build()

        mAdView!!.adListener = object : AdListener() {
            override fun onAdClosed() {
                mAdView!!.loadAd(build)
                Log.d(TAG, "onAdClosed Banner Ad")
            }

            override fun onAdFailedToLoad(p0: Int) {
                super.onAdFailedToLoad(p0)
                mAdView!!.loadAd(build)
                Log.d(TAG, "onAdFailedToLoad Banner Ad")
            }
        }
        mAdView!!.loadAd(build)
        requestNewInterstitial()
        loadRewardedVideoAd()
    }

    private fun initialiseAds() {
        mInterstitialAd = InterstitialAd(this)
        mInterstitialAd?.adUnitId = "ca-app-pub-3940256099942544/1033173712"
        mInterstitialAd?.adListener = object : AdListener() {
            override fun onAdClosed() {
                requestNewInterstitial()
            }
        }
        mRewardedVideoAd = MobileAds.getRewardedVideoAdInstance(this)
    }

    private fun getNonPersonalizedAdsBundle(): Bundle {
        val extra = Bundle()
        extra.putString("npa", "1")
        return extra
    }

    private fun requestNewInterstitial() {
        val adRequest: AdRequest
        if (mShowPersonlAds) {
            adRequest = AdRequest.Builder().build()
        } else {
            adRequest = AdRequest.Builder()
                    .addNetworkExtrasBundle(AdMobAdapter::class.java, getNonPersonalizedAdsBundle())
                    .build()
        }
        mInterstitialAd!!.loadAd(adRequest)
    }

    private fun loadRewardedVideoAd() {
        val adRequest: AdRequest
        if (mShowPersonlAds) {
            adRequest = AdRequest.Builder().build()
        } else {
            adRequest = AdRequest.Builder()
                    .addNetworkExtrasBundle(AdMobAdapter::class.java, getNonPersonalizedAdsBundle())
                    .build()

        }
        mRewardedVideoAd?.loadAd("ca-app-pub-3940256099942544/5224354917", adRequest)
    }

    private fun displayConsentForm() {
        consentForm = ConsentForm.Builder(this, URL("https://your.privacy.url/"))
                .withListener(object : ConsentFormListener() {
                    override fun onConsentFormOpened() {
                        super.onConsentFormOpened()
                        Log.d(TAG, "Requesting Consent: onConsentFormOpened")
                    }

                    override fun onConsentFormLoaded() {
                        super.onConsentFormLoaded()
                        Log.d(TAG, "Requesting Consent: onConsentFormLoaded")
                        consentForm?.show()
                    }

                    override fun onConsentFormError(reason: String?) {
                        super.onConsentFormError(reason)
                        ConsentInformation.getInstance(this@MainActivity).consentStatus = ConsentStatus.PERSONALIZED
                        Log.d(TAG, "Requesting Consent: onConsentFormError. $reason")
                    }

                    override fun onConsentFormClosed(consentStatus: ConsentStatus?, userPrefersAdFree: Boolean?) {
                        super.onConsentFormClosed(consentStatus, userPrefersAdFree)
                        Log.d(TAG, "Requesting Consent: onConsentFormClosed")
                        var userWantsAdFree = false
                        if (userPrefersAdFree != null) {
                            userWantsAdFree = userPrefersAdFree
                        }
                        if (userWantsAdFree) {
                            // Buy or Subscribe
                            Log.d(TAG, "Requesting Consent: User prefers AdFree")
                            // TODO This is where you write your Intent to launch the purchase flow dialog

                        } else {
                            Log.d(TAG, "Requesting Consent: onConsentFormClosed. Consent Status = $consentStatus")
                            when (consentStatus) {
                                ConsentStatus.PERSONALIZED -> loadAds(true)
                                ConsentStatus.NON_PERSONALIZED -> loadAds(false)
                                ConsentStatus.UNKNOWN -> loadAds(true)
                            }
                        }
                    }
                })
                .withPersonalizedAdsOption()
                .withNonPersonalizedAdsOption()
      //          .withAdFreeOption() TODO enable this option if you have created an inApp Purchase to eliminate Ads
                .build()
        consentForm!!.load()
    }

    override fun onSaveInstanceState(outState: Bundle?, outPersistentState: PersistableBundle?) {
        super.onSaveInstanceState(outState, outPersistentState)
        outState?.putBoolean(SHOW_PERSONAL_ADS_KEY, mShowPersonlAds)
    }
}
