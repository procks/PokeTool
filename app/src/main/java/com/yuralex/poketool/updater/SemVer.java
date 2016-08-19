package com.yuralex.poketool.updater;

public class SemVer implements Comparable<SemVer> {
    int major;
    int minor;
    int patch;

    public SemVer(int major, int minor, int patch) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
    }

    public static SemVer parse(String versionString) {
        int majorVersion = 0;
        int minorVersion = 0;
        int patchVersion = 0;
        if (!versionString.equals("")) {
            versionString = versionString.trim().replace("v", "");
            String[] versionParts = versionString.split("\\.");
            if (versionParts.length > 0) {
                majorVersion = Integer.parseInt(versionParts[0]);
            }
            if (versionParts.length > 1) {
                minorVersion = Integer.parseInt(versionParts[1]);
            }
            if (versionParts.length > 2) {
                patchVersion = Integer.parseInt(versionParts[2]);
            }
        }
        return new SemVer(majorVersion, minorVersion, patchVersion);
    }

    @Override
    public int compareTo(SemVer other) {
        int result = major - other.major;
        if (result == 0) {
            result = minor - other.minor;
            if (result == 0) {
                result = patch - other.patch;
            }
        }
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof SemVer)) {
            return false;
        }
        return compareTo((SemVer) other) == 0;
    }

    @Override
    public String toString() {
        return String.format("v%d.%d.%d", major, minor, patch);
    }
}
