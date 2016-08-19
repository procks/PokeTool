package com.omkarmoghe.pokemap.controllers.net;

import android.app.Activity;
import android.os.AsyncTask;
import android.support.annotation.NonNull;

import com.omkarmoghe.pokemap.models.login.LoginInfo;
import com.pokegoapi.api.PokemonGo;
import com.pokegoapi.auth.PtcCredentialProvider;

import okhttp3.OkHttpClient;

/**
 * Created by vanshilshah on 20/07/16.
 */
public class NianticManager {
    private static final String TAG = "NianticManager";
    private static final NianticManager instance = new NianticManager();

    private Activity mActivity;
    private LoginInfo mInfo;
    private AuthListener mAuthListener;
    private LoginListener mLoginListener;
    private PokemonGo mPokemonGo;

    public static NianticManager getInstance(){
        return instance;
    }

    private NianticManager(){

    }

    public void login(final String username, final String password, final LoginListener loginListener){
        mLoginListener = loginListener;
        new LoginAsyncTask().execute(username, password);
    }

    private class LoginAsyncTask extends AsyncTask<String, Void, String[]> {
        @Override
        protected String[] doInBackground(String... params) {
            String error = null;
            String authToken = null;
            OkHttpClient http = new OkHttpClient();
            try {
                // check readme for other example
                PokemonGo go = new PokemonGo(new PtcCredentialProvider(http, params[0],
                        params[1]), http);
                authToken = go.getAuthInfo().getToken().toString();
                mPokemonGo = go;
//            } catch (LoginFailedException | RemoteServerException e) {
            } catch (Exception e) {
                mPokemonGo = null;
                error = e.getMessage();
                e.printStackTrace();
            }
            return new String[]{authToken, error};
        }

        @Override
        protected void onPostExecute(String[] result) {
            if (result[0] != null) {
                mLoginListener.authSuccessful(result[0]);
            } else {
                mLoginListener.authFailed(result[1]);
            }
        }
    }

    public interface LoginListener {
        void authSuccessful(String authToken);
        void authFailed(String message);
    }

    public interface AuthListener{
        void authSuccessful();
        void authFailed(String message, String Provider);
    }

    /**
     * Sets the pokemon trainer club auth token for the auth info also invokes the onLogin callback.
     */
    public void setLoginInfo(final Activity activity, @NonNull final LoginInfo info, @NonNull final AuthListener listener) {
        mActivity = activity;
        mInfo = info;
        mAuthListener = listener;
        new AuthAsyncTask().execute(info);
    }

    private class AuthAsyncTask extends AsyncTask<LoginInfo, Void, String[]> {
        @Override
        protected String[] doInBackground(LoginInfo... params) {
            String error = null;
            OkHttpClient http = new OkHttpClient();
            try {
                // check readme for other example
                mPokemonGo = new PokemonGo(params[0].getCredentialProvider(http), http);
//            } catch (LoginFailedException | RemoteServerException e) {
            } catch (Exception e) {
                mPokemonGo = null;
                error = e.getMessage();
                e.printStackTrace();
            }
            return new String[]{error, params[0].getProvider()};
        }

        @Override
        protected void onPostExecute(String[] result) {
            if (result[0] == null) {
                mAuthListener.authSuccessful();
            } else {
                mAuthListener.authFailed(result[0], result[1]);
            }
        }
    }
    public PokemonGo getPokemonGo() {
        return mPokemonGo;
    }
}
