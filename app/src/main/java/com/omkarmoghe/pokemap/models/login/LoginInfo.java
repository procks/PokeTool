package com.omkarmoghe.pokemap.models.login;

import com.pokegoapi.auth.CredentialProvider;
import com.pokegoapi.exceptions.LoginFailedException;
import com.pokegoapi.exceptions.RemoteServerException;

import POGOProtos.Networking.Envelopes.RequestEnvelopeOuterClass.RequestEnvelope.AuthInfo;
import okhttp3.OkHttpClient;

/**
 * Created by chris on 7/25/2016.
 */

public abstract class LoginInfo {

    public static final String PROVIDER_GOOGLE = "google";
    public static final String PROVIDER_PTC = "ptc";

    private String mToken;

    public LoginInfo(String token){
        mToken = token;
    }

    public void setToken(String token) {
        this.mToken = token;
    }

    public String getToken(){
        return mToken;
    }

    public abstract String getProvider();

    public AuthInfo createAuthInfo(){
        AuthInfo.Builder builder = AuthInfo.newBuilder();
        builder.setProvider(getProvider());
        builder.setToken(AuthInfo.JWT.newBuilder().setContents(mToken).setUnknown2(59).build());
        return builder.build();
    }

    public abstract CredentialProvider getCredentialProvider(OkHttpClient client) throws LoginFailedException, RemoteServerException;
}