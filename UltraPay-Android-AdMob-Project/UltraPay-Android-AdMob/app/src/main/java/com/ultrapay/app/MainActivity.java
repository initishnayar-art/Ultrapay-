package com.ultrapay.app;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

public class MainActivity extends AppCompatActivity {
    private static final String HOME_URL = "https://ultrapayhome.netlify.app/";
    private static final String BANNER_ID = "ca-app-pub-3814920920932019/2247049069";
    private static final String INTERSTITIAL_ID = "ca-app-pub-3814920920932019/2655645541";

    private WebView webView;
    private InterstitialAd interstitialAd;
    private boolean buyAdShownForCurrentVisit = false;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.ultrapay.app.R.layout.activity_main);

        webView = findViewById(com.ultrapay.app.R.id.webView);
        FrameLayout bannerContainer = findViewById(com.ultrapay.app.R.id.bannerContainer);

        MobileAds.initialize(this, status -> {
            loadBanner(bannerContainer);
            loadInterstitial();
        });

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setUserAgentString(settings.getUserAgentString() + " UltraPayAndroid/1.0");

        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                String url = uri.toString();

                if (url.startsWith("https://ultrapayhome.netlify.app")) {
                    maybeShowBuyInterstitial(url);
                    return false;
                }

                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, uri));
                } catch (Exception ignored) { }
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                maybeShowBuyInterstitial(url);
                if (!url.contains("#buy")) {
                    buyAdShownForCurrentVisit = false;
                }
            }
        });

        webView.loadUrl(HOME_URL);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (webView.canGoBack()) webView.goBack();
                else finish();
            }
        });
    }

    private void loadBanner(FrameLayout container) {
        AdView adView = new AdView(this);
        adView.setAdUnitId(BANNER_ID);
        adView.setAdSize(AdSize.BANNER);
        container.removeAllViews();
        container.addView(adView);
        adView.loadAd(new AdRequest.Builder().build());
    }

    private void loadInterstitial() {
        InterstitialAd.load(
                this,
                INTERSTITIAL_ID,
                new AdRequest.Builder().build(),
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(InterstitialAd ad) {
                        interstitialAd = ad;
                        interstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                            @Override
                            public void onAdDismissedFullScreenContent() {
                                interstitialAd = null;
                                loadInterstitial();
                            }

                            @Override
                            public void onAdFailedToShowFullScreenContent(com.google.android.gms.ads.AdError adError) {
                                interstitialAd = null;
                                loadInterstitial();
                            }
                        });
                    }

                    @Override
                    public void onAdFailedToLoad(LoadAdError error) {
                        interstitialAd = null;
                    }
                }
        );
    }

    private void maybeShowBuyInterstitial(String url) {
        if (url != null && url.contains("#buy") && !buyAdShownForCurrentVisit) {
            buyAdShownForCurrentVisit = true;
            if (interstitialAd != null) {
                interstitialAd.show(this);
            }
        }
    }
}
