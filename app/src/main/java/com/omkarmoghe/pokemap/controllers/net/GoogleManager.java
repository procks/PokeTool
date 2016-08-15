package com.omkarmoghe.pokemap.controllers.net;

import android.os.AsyncTask;

import com.yuralex.poketool.GoogleUserCredentialProvider;

import okhttp3.OkHttpClient;

/**
 * Created by chris on 7/21/2016.
 */
public class GoogleManager {
    private static final String TAG = "GoogleManager";

    private static GoogleManager ourInstance = new GoogleManager();
    private LoginListener mListener;

    public static GoogleManager getInstance() {
        return ourInstance;
    }

    private GoogleManager() {
    }

    public void requestToken(String deviceCode, final LoginListener listener){
        mListener = listener;
        new RequestTokenAsyncTask().execute(deviceCode);
    }

    private class RequestTokenAsyncTask extends AsyncTask<String, Void, String[]> {
        @Override
        protected String[] doInBackground(String... params) {
            String token = params[0];
            String refreshToken = null;
            String authToken = null;
            String error = null;
            OkHttpClient http = new OkHttpClient();
            try {
                GoogleUserCredentialProvider provider;
                provider = new GoogleUserCredentialProvider(http);
                provider.login(token);
                refreshToken = provider.getRefreshToken();
                authToken = provider.getTokenId();
                System.out.println("Refresh token:" + provider.getRefreshToken());
//            } catch (LoginFailedException | RemoteServerException e) {
            } catch (Exception e) {
                error = e.getMessage();
                e.printStackTrace();
            }
            return new String[]{authToken, refreshToken, error};
        }

        @Override
        protected void onPostExecute(String[] result) {
            if (result[0] != null) {
                mListener.authSuccessful(result[0], result[1]);
            } else {
                mListener.authFailed(result[2]);
            }
        }
    }

    public interface LoginListener {
        void authSuccessful(String authToken, String refreshToken);
        void authFailed(String message);
    }
}
