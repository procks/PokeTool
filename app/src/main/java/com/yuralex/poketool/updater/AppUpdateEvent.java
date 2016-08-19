package com.yuralex.poketool.updater;

public class AppUpdateEvent {
    public static final int OK = 1;
    public static final int FAILED = 2;
    public static final int UPTODATE = 3;

    public AppUpdate appUpdate;
    public int status;

    public AppUpdateEvent(int status) { this.status = status; }
    public AppUpdateEvent(int status, AppUpdate update) {
        this.status = status;
        this.appUpdate = update;
    }
}
