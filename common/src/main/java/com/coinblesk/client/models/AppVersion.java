package com.coinblesk.client.models;

import ch.papers.objectstorage.models.AbstractUuidObject;

/**
 * Version object to keep track of the app version and upgrades between app versions.
 * The version is persisted on disk in a file. This allows the app to check whether
 * (1) the version is the same as version of the software build (BuildConfig.VERSION_NAME) and
 * (2) whether there is a need for upgrading data or other migration tasks.
 *
 * Hence, the persisted version should be kept up to date.
 *
 * @author Andreas Albrecht
 */
public class AppVersion extends AbstractUuidObject {
    private String version;

    public String getVersion() {

        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
