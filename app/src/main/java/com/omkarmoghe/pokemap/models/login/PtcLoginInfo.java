package com.omkarmoghe.pokemap.models.login;

import com.pokegoapi.auth.CredentialProvider;
import com.pokegoapi.auth.PtcCredentialProvider;
import com.pokegoapi.exceptions.LoginFailedException;
import com.pokegoapi.exceptions.RemoteServerException;

import okhttp3.OkHttpClient;

/**
 * Created by chris on 7/26/2016.
 */

public class PtcLoginInfo extends LoginInfo {

    private String mUsername;
    private String mPassword;

    public PtcLoginInfo(String authToken, String username, String password){
        super(authToken);
        mUsername = username;
        mPassword = password;
    }

    public String getUsername() {
        return mUsername;
    }

    public void setUsername(String username) {
        this.mUsername = username;
    }

    public String getPassword() {
        return mPassword;
    }

    public void setPassword(String password) {
        this.mPassword = password;
    }

    @Override
    public String getProvider() {
        return PROVIDER_PTC;
    }

    public CredentialProvider getCredentialProvider(OkHttpClient client) throws LoginFailedException, RemoteServerException {
        return new PtcCredentialProvider(client, mUsername, mPassword);
    }
}
