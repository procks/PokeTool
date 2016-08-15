package com.omkarmoghe.pokemap.views;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.MenuItem;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.pokegoapi.auth.GoogleUserCredentialProvider;
import com.yuralex.poketool.R;

import okhttp3.HttpUrl;


public class GoogleAuthActivity extends AppCompatActivity {
    private static final String TAG = "GoogleAuthActivity";

    private static final String ARG_URL = "Google Auth Url";
    private static final String ARG_CODE = "Google User Code";
    public static final String EXTRA_CODE = "Extra Google Code";

    public static final String OAUTH_ENDPOINT = "https://accounts.google.com/o/oauth2/auth";//!!!

    private WebView webView;

    public static void startForResult(Activity starter, int requestCode){
        Intent intent = new Intent(starter, GoogleAuthActivity.class);
        starter.startActivityForResult(intent, requestCode);
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_google_auth);
//        OAUTH_ENDPOINT
        HttpUrl url = HttpUrl.parse(OAUTH_ENDPOINT).newBuilder()
                .addQueryParameter("client_id", GoogleUserCredentialProvider.CLIENT_ID)
                .addQueryParameter("scope", "openid email https://www.googleapis.com/auth/userinfo.email")
                .addQueryParameter("response_type","code")
                .addQueryParameter("redirect_uri","http://127.0.0.1:8080")
                // TODO: 13.08.2016 redirect_uri http://127.0.0.1:8080 in PokeGOAPI-library
//                .addQueryParameter("redirect_uri","urn:ietf:wg:oauth:2.0:oob")
                .build();

//        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);
//        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white);

        webView = (WebView) findViewById(R.id.webview);


        WebViewClient client = new WebViewClient(){
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.startsWith("http://127.0.0.1")) {
                    Uri uri = Uri.parse(url);

                    sendResults(uri.getQueryParameter("code"));

                    return true;
                }
                return super.shouldOverrideUrlLoading(view, url);
            }
        };

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webView.setWebViewClient(client);
        webView.loadUrl(url.toString());
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_CANCELED);
        finish();
    }

    private void sendResults(String code){
        if(!TextUtils.isEmpty(code)) {
            Intent intent = new Intent();
            intent.putExtra(EXTRA_CODE, code);
            setResult(RESULT_OK, intent);
        }else{
            setResult(RESULT_CANCELED);
        }
        finish();
    }
}
