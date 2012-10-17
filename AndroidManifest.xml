<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
          package="org.emergent.android.weave"
          android:versionName="1.2.0" android:versionCode="8">

  <!-- History:
  1.0.0 [1]: 2011-01-01
  1.1.0 [4]: 2012-08-03
  1.1.1 [5]: 2012-08-22
  1.1.2 [6]: 2012-08-22
  1.1.3 [7]: 2012-08-31
  1.2.0 [8]: 2012-09-01 backward compatibility with SDK API 7 added
  -->

  <uses-sdk android:targetSdkVersion="16" android:minSdkVersion="7"/>
  <supports-screens android:normalScreens="true" android:smallScreens="true" android:largeScreens="true"/>

  <uses-permission android:name="android.permission.INTERNET"/>
  <!--
  <uses-permission android:name="android.permission.CLEAR_APP_CACHE"/>
  -->

  <application android:name=".ShamanApplication"
               android:label="@string/app_name"
               android:icon="@drawable/ic_launcher"
               android:allowClearUserData="true"
      >

    <activity android:name=".MainActivity"
              android:label="@string/app_name"
              android:theme="@style/CustomTheme"
        >
      <intent-filter>
        <action android:name="android.intent.action.MAIN"/>
        <category android:name="android.intent.category.LAUNCHER"/>
      </intent-filter>
    </activity>

    <activity android:name=".LoginActivity"
              android:label="@string/login_activity_title"
              android:excludeFromRecents="true"
        >
    </activity>

    <activity android:name=".ApplicationOptionsActivity"
              android:label="@string/appopts_activity_title"
        />

    <activity android:name=".AboutActivity"
              android:label="@string/about_title"
              android:excludeFromRecents="true"
        >
    </activity>

    <provider android:name=".persistence.BookmarkContentProvider"
              android:label="@string/bookmark_provider_label"
              android:authorities="org.emergent.android.weave.bookmarkcontentprovider"
              android:exported="false"
              android:syncable="true"
        />

    <provider android:name=".persistence.PasswordContentProvider"
              android:label="@string/password_provider_label"
              android:authorities="org.emergent.android.weave.passwordcontentprovider"
              android:exported="false"
              android:syncable="true"
        />

    <service android:name=".syncadapter.SyncService"
        />

  </application>

</manifest>
