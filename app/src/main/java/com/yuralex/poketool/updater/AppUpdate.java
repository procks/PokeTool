package com.yuralex.poketool.updater;

public class AppUpdate {
    public String assetUrl;
    public String version;
    public String changelog;
    public AppUpdate(String assetUrl, String version, String changelog) {
        this.assetUrl = assetUrl;
        this.version = version;
        this.changelog = changelog;
    }
}
