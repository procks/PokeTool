package com.omkarmoghe.pokemap.models.login;

import com.pokegoapi.auth.CredentialProvider;
import com.pokegoapi.exceptions.LoginFailedException;
import com.pokegoapi.exceptions.RemoteServerException;
import com.yuralex.poketool.GoogleUserCredentialProvider;

import okhttp3.OkHttpClient;

/**
 * Created by chris on 7/26/2016.
 */

public class GoogleLoginInfo extends LoginInfo {

    private String mRefreshToken;

    public GoogleLoginInfo(String authToken, String refreshToken){
        super(authToken);
        mRefreshToken = refreshToken;
    }

    public String getRefreshToken() {
        return mRefreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.mRefreshToken = refreshToken;
    }

    @Override
    public String getProvider() {
        return PROVIDER_GOOGLE;
    }

    public CredentialProvider getCredentialProvider(OkHttpClient client) throws LoginFailedException, RemoteServerException {
        return new GoogleUserCredentialProvider(client, mRefreshToken);
    }
}
