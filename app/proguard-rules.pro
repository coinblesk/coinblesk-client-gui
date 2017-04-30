# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Users/ckiller/Library/Android/sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}
-dontobfuscate

-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver

# Pretty Time
-keepnames class ** implements org.ocpsoft.prettytime.TimeUnit
-keep class org.ocpsoft.prettytime.i18n.**

-assumenosideeffects class * implements org.slf4j.Logger {
    public *** trace(...);
    public *** debug(...);
    public *** info(...);
    public *** warn(...);
    public *** error(...);
}



-dontwarn org.slf4j.**
-dontwarn ch.qos.logback.**
-dontwarn org.spongycastle.x509.util.LDAPStoreHelper**
-dontwarn org.spongycastle.jce.provider.X509LDAPCertStoreSpi**
-dontwarn org.ocpsoft.pretty.time.web.jsf.**
-dontwarn org.bitcoinj.store.LevelDBBlockStore.**
-dontwarn com.fasterxml.jackson.databind.**

##---------------Begin: proguard configuration for Gson  ----------
# Gson uses generic type information stored in a class file when working with fields. Proguard
# removes such information by default, so configure it to keep all of it.
-keepattributes Signature

# For using GSON @Expose annotation
-keepattributes *Annotation*

# Gson specific classes
-keep class sun.misc.Unsafe { *; }
#-keep class com.google.gson.stream.** { *; }

# Application classes that will be serialized/deserialized over Gson
-keep class com.google.gson.examples.android.model.** { *; }


##---------------End: proguard configuration for Gson  ----------

# Retrofit 2.X
## https://square.github.io/retrofit/ ##

-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions

-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# Also you must note that if you are using GSON for conversion from JSON to POJO representation, you must ignore those POJO classes from being obfuscated.
# Here include the POJO's that have you have created for mapping JSON response to POJO for example.

# bitcoinj
-keep,includedescriptorclasses class org.bitcoinj.wallet.Protos$** { *; }
-keep,includedescriptorclasses class org.bitcoin.protocols.payments.Protos$** { *; }
-dontwarn org.bitcoinj.store.WindowsMMapHack
-dontwarn org.bitcoinj.store.LevelDBBlockStore
-dontwarn org.bitcoinj.store.LevelDBFullPrunedBlockStore*
-dontnote org.bitcoinj.crypto.DRMWorkaround
-dontnote org.bitcoinj.crypto.TrustStoreLoader$DefaultTrustStoreLoader
-dontnote com.subgraph.orchid.crypto.PRNGFixes
-dontwarn okio.DeflaterSink
-dontwarn okio.Okio
-dontnote com.squareup.okhttp.internal.Platform

# zxing
-dontwarn com.google.zxing.common.BitMatrix

# Guava
-dontwarn sun.misc.Unsafe
-dontnote com.google.common.reflect.**
-dontnote com.google.common.util.concurrent.MoreExecutors
-dontnote com.google.common.cache.Striped64,com.google.common.cache.Striped64$Cell

# xchange
-keep class com.xeiam.xchange.bitstamp.BitstampExchange
-keep class com.xeiam.xchange.bitstamp.dto.marketdata.** {*;}
-keepnames class com.fasterxml.jackson.** {
*;
}
-keepnames interface com.fasterxml.jackson.** {
    *;
}

# spongy castle
-keep class org.spongycastle.crypto.* {*;}
-keep class org.spongycastle.crypto.digests.* {*;}
-keep class org.spongycastle.crypto.encodings.* {*;}
-keep class org.spongycastle.crypto.engines.* {*;}
-keep class org.spongycastle.crypto.macs.* {*;}
-keep class org.spongycastle.crypto.modes.* {*;}
-keep class org.spongycastle.crypto.paddings.* {*;}
-keep class org.spongycastle.crypto.params.* {*;}
-keep class org.spongycastle.crypto.prng.* {*;}
-keep class org.spongycastle.crypto.signers.* {*;}

-keep class org.spongycastle.jcajce.provider.digest.** {*;}
-keep class org.spongycastle.jcajce.provider.symmetric.** {*;}
-keep class org.spongycastle.jcajce.provider.asymmetric.** {*;}
-keep class org.spongycastle.jcajce.spec.* {*;}

# keep everything that is serialized
-keep class com.coinblesk.json.** { *; }
#-keep class com.coinblesk.payments.models.** { *; }

-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
