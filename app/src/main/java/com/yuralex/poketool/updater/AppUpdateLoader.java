package com.yuralex.poketool.updater;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.support.compat.BuildConfig;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class AppUpdateLoader extends AsyncTask<Void, Void, AppUpdateEvent> {
    private final Context mContext;
    private final OnAppUpdateEventListener mListener;

    public AppUpdateLoader(Context context, OnAppUpdateEventListener listener) {
        mContext = context;
        mListener = listener;
    }

    @Override
    protected AppUpdateEvent doInBackground(Void... voids) {
        AppUpdateEvent appUpdateEvent;

        OkHttpClient httpClient = new OkHttpClient();
        String apiEndpoint = "https://api.github.com/repos/procks/PokeTool/releases";
        Request request = new Request.Builder()
                .url(apiEndpoint)
                .build();

        try {
            Response response = httpClient.newCall(request).execute();
            JSONArray releaseInfo = new JSONArray(response.body().string());
            JSONObject latestRelease = releaseInfo.getJSONObject(0);
            JSONObject releaseAssets = latestRelease.getJSONArray("assets").getJSONObject(0);

            AppUpdate update = new AppUpdate(releaseAssets.getString("browser_download_url"), latestRelease.getString("tag_name"), latestRelease.getString("body"));

            String version = "";
            try {
                PackageInfo pInfo =  mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0);
                version = pInfo.versionName;
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }

            SemVer currentVersion = SemVer.parse(version);
            SemVer remoteVersion = SemVer.parse(update.version);

            if (currentVersion.compareTo(remoteVersion) < 0) {
                appUpdateEvent = new AppUpdateEvent(AppUpdateEvent.OK, update);
            } else {
                appUpdateEvent = new AppUpdateEvent(AppUpdateEvent.UPTODATE, update);
            }
        } catch (JSONException | IOException e) {
//            if (EventBus.getDefault().hasSubscriberForEvent(AppUpdateEvent.class)) {
//                EventBus.getDefault().post();
//            }
            appUpdateEvent = new AppUpdateEvent(AppUpdateEvent.FAILED);
            e.printStackTrace();
        }
        return appUpdateEvent;
    }

    @Override
    protected void onPostExecute(AppUpdateEvent appUpdateEvent) {
        mListener.onAppUpdateEvent(appUpdateEvent);
    }

    public interface OnAppUpdateEventListener {
        void onAppUpdateEvent(AppUpdateEvent event);
    }

}
