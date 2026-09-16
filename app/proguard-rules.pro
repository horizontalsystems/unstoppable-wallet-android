# ============================================================
# Debugging: preserve line numbers in stack traces
# ============================================================
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ============================================================
# Kotlin metadata / annotations
# ============================================================
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses, EnclosingMethod

# ============================================================
# Enums — must keep values() and valueOf() for reflection
# ============================================================
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ============================================================
# Parcelable
# ============================================================
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# ============================================================
# Serializable
# ============================================================
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ============================================================
# Gson — keep fields annotated with @SerializedName
# ============================================================
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# ============================================================
# Retrofit — keep annotated interface methods
# ============================================================
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn retrofit2.**
-dontwarn kotlin.Unit

# ============================================================
# OkHttp
# ============================================================
-dontwarn okhttp3.**
-dontwarn okio.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# ============================================================
# Room — keep entities, DAOs, and database classes
# ============================================================
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep @androidx.room.Dao class * { *; }
-keep @androidx.room.Database class * { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
-keepclassmembers @androidx.room.TypeConverter class * { *; }
-keepclassmembers class * {
    @androidx.room.TypeConverter *;
}

# ============================================================
# WorkManager workers
# ============================================================
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.CoroutineWorker
-keep class * extends androidx.work.ListenableWorker
-keepclassmembers class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# ============================================================
# App: entities and storage — used by Room and Gson
# ============================================================
# NOTE: the bulk of this code lives in the :walletkit modules
# (io.horizontalsystems.walletkit.**), not in the app package.
-keep class io.horizontalsystems.walletkit.entities.** { *; }
-keep class io.horizontalsystems.walletkit.core.storage.** { *; }
-keep class io.horizontalsystems.bankwallet.entities.** { *; }
-keep class io.horizontalsystems.bankwallet.core.storage.** { *; }

# ============================================================
# App: backup data classes — deserialized from JSON via Gson
# ============================================================
-keep class io.horizontalsystems.walletkit.modules.backuplocal.BackupLocalModule { *; }
-keep class io.horizontalsystems.walletkit.modules.backuplocal.BackupLocalModule$** { *; }
-keep class io.horizontalsystems.walletkit.modules.backuplocal.fullbackup.BackupProvider { *; }
-keep class io.horizontalsystems.walletkit.modules.backuplocal.fullbackup.BackupProvider$** { *; }

# ============================================================
# App: EVM label provider — JSON API response models
# ============================================================
-keep class io.horizontalsystems.walletkit.core.providers.EvmLabelProvider$** { *; }
-keep class io.horizontalsystems.bankwallet.core.providers.EvmLabelProvider$** { *; }

# ============================================================
# App: pin storage (Room entity with encrypted fields)
# ============================================================
-keep class io.horizontalsystems.walletkit.modules.pin.core.** { *; }

# ============================================================
# App: WalletConnect session storage
# ============================================================
-keep class io.horizontalsystems.walletkit.modules.walletconnect.storage.** { *; }

# ============================================================
# App: chart indicator settings (Room entity)
# ============================================================
-keep class io.horizontalsystems.walletkit.modules.chart.ChartIndicatorSetting { *; }
-keep class io.horizontalsystems.walletkit.modules.chart.ChartIndicatorSettingsDao { *; }

# ============================================================
# Kotlin Coroutines
# ============================================================
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# ============================================================
# RxJava 2
# ============================================================
-dontwarn rx.**
-dontwarn io.reactivex.**
-keepclassmembers class rx.internal.util.unsafe.*ArrayQueue*Field* {
    long producerIndex;
    long consumerIndex;
}
-keepclassmembers class rx.internal.util.unsafe.BaseLinkedQueueProducerNodeRef {
    rx.internal.util.atomic.LinkedQueueNode producerNode;
}
-keepclassmembers class rx.internal.util.unsafe.BaseLinkedQueueConsumerNodeRef {
    rx.internal.util.atomic.LinkedQueueNode consumerNode;
}

# ============================================================
# WalletConnect / Web3 — suppress warnings from optional deps
# ============================================================
-dontwarn com.sun.jna.**
-dontwarn org.slf4j.**
-dontwarn javax.naming.**
-dontwarn sun.security.**
-dontwarn java.lang.instrument.**
-dontwarn sun.misc.SignalHandler
-dontwarn android.support.**

# ============================================================
# Bouncy Castle (used by crypto kits)
# ============================================================
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**

# ============================================================
# Tor — keep Tor-related classes accessed by name
# ============================================================
-keep class net.freehaven.tor.control.** { *; }
-dontwarn net.freehaven.tor.control.**

# ============================================================
# Suppress common missing-class warnings from transitive deps
# ============================================================
-dontwarn com.google.errorprone.annotations.**
-dontwarn com.google.j2objc.annotations.**
-dontwarn org.checkerframework.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**

# ============================================================
# Lombok — used by Stellar SDK at compile time only; not in runtime classpath
# ============================================================
-dontwarn lombok.Generated
-dontwarn lombok.NonNull

# ============================================================
# Firebase Messaging — referenced by WalletConnect (Reown) push notifications
# but not present in F-Droid builds (no Google Play Services)
# ============================================================
-dontwarn com.google.firebase.messaging.FirebaseMessagingService
-dontwarn com.google.firebase.messaging.RemoteMessage
-dontwarn com.google.firebase.messaging.RemoteMessage$Notification

# ============================================================
# Gson DTOs — reflectively deserialized, must be HARD-kept
# ============================================================
# R8 cannot see Gson's reflective field access, so it renames AND
# tree-shakes fields it believes are unread.
#
# R8 full mode is ON (AGP 9.x default). In full mode `-keepclassmembers`
# is only a SOFT pin -- verified: with it, RpcTransaction still lost
# hash/nonce/value and kept only the reference-typed from/to. Gson's own
# documentation requires a hard `-keep` for such classes.
#
# These are deliberately WHOLE-NAMESPACE, not per-package. A surgical
# package list was tried and silently missed two classes in one kit:
#   - JsonRpc            (api.jsonrpc)  -- serialized INTO the request;
#                        losing `method`/`params` sends a malformed call
#   - RpcResponse        (api.core)     -- losing `id`/`result`/`error`
#                        makes every response parse to null, so the kit
#                        throws InvalidResult and re-syncs forever
# Both sit outside the `.models` packages you would think to keep. The
# kits are a small share of total classes, so pinning them whole costs
# little obfuscation and removes a whole class of silent data bugs.
# Add any NEW kit namespace here.

-keep class io.horizontalsystems.ethereumkit.** { *; }
-keep class io.horizontalsystems.erc20kit.** { *; }
-keep class io.horizontalsystems.nftkit.** { *; }
-keep class io.horizontalsystems.uniswapkit.** { *; }
-keep class io.horizontalsystems.oneinchkit.** { *; }
-keep class io.horizontalsystems.marketkit.** { *; }
-keep class io.horizontalsystems.tronkit.** { *; }
-keep class io.horizontalsystems.thorchainkit.** { *; }
-keep class io.horizontalsystems.xrpkit.** { *; }
-keep class io.horizontalsystems.stellarkit.** { *; }
-keep class io.horizontalsystems.tonkit.** { *; }
-keep class io.horizontalsystems.solanakit.** { *; }
-keep class io.horizontalsystems.bitcoincore.** { *; }
-keep class io.horizontalsystems.bitcoinkit.** { *; }
-keep class io.horizontalsystems.dashkit.** { *; }
-keep class io.horizontalsystems.litecoinkit.** { *; }
-keep class io.horizontalsystems.bitcoincashkit.** { *; }
-keep class io.horizontalsystems.ecashkit.** { *; }
-keep class io.horizontalsystems.hodler.** { *; }
-keep class io.horizontalsystems.hdwalletkit.** { *; }
-keep class io.horizontalsystems.feeratekit.** { *; }

# --- third-party SDKs the kits wrap (their own JSON DTOs) -----
# stellar-kit and solana-kit delegate parsing to these. Missing them
# showed up as "Sync error" on XLM and SOL only -- the kit classes were
# kept, but the SDK response objects they deserialize into were not:
#   org.stellar.sdk.responses.AccountResponse -> c7
-keep class org.stellar.sdk.** { *; }
-keep class com.solana.** { *; }
-keep class org.sol4k.** { *; }

# web3j ABI encoding/decoding.
# TypeReference resolves its own type argument at runtime via
# getClass().getGenericSuperclass(), so every anonymous
# `object : TypeReference<DynamicArray<...>>() {}` must keep its generic
# signature AND its place in the hierarchy. -keepattributes Signature
# alone is NOT enough: R8 still merges/rewrites the hierarchy, and the
# app crashed with
#   java.lang.RuntimeException: Missing type parameter.
#     at MulticallMethod$Factory$createMethod$decode$1.<init>
#     at DecorationManager.decorateTransactions
-keep class org.web3j.** { *; }
-keep class * extends org.web3j.abi.TypeReference { *; }
-keep class * extends org.web3j.abi.datatypes.Type { *; }

# --- app / walletkit: packages that touch Gson ----------------
-keep class io.horizontalsystems.walletkit.core.providers.** { *; }
-keep class io.horizontalsystems.walletkit.core.stats.** { *; }
-keep class io.horizontalsystems.walletkit.entities.** { *; }
-keep class io.horizontalsystems.walletkit.modules.backuplocal.** { *; }
-keep class io.horizontalsystems.walletkit.modules.contacts.** { *; }
-keep class io.horizontalsystems.walletkit.modules.multiswap.providers.** { *; }
-keep class io.horizontalsystems.walletkit.modules.opencryptopay.** { *; }
-keep class io.horizontalsystems.walletkit.modules.privatesend.** { *; }
-keep class io.horizontalsystems.walletkit.modules.restorelocal.** { *; }
-keep class io.horizontalsystems.walletkit.modules.walletconnect.** { *; }
-keep class io.horizontalsystems.walletkit.widgets.** { *; }

# ============================================================
# JNI / native-backed classes — resolved by NAME from C/C++/Rust
# ============================================================
# Native code calls FindClass("io/horizontalsystems/monerokit/model/CoinsInfo")
# and friends. R8 sees no Java reference, renames the class, and JNI_OnLoad
# then aborts the process. Observed crash before these rules:
#   ClassNotFoundException: io.horizontalsystems.monerokit.model.CoinsInfo
#   at WalletManager.<clinit>() -> libmonerujo.so (JNI_OnLoad+252) -> SIGABRT
# Every package below owns one of the .so files packaged in the APK.
# (zanokit and net.zetetic already ship their own consumer keep rules.)

# classes that declare native methods (names must match the C symbols)
-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}

# libmonerujo.so
-keep class io.horizontalsystems.monerokit.** { *; }

# libzcashwalletsdk.so
-keep class cash.z.** { *; }

# libjnidispatch.so — JNA maps by name and depends on declared field ORDER
-keep class com.sun.jna.** { *; }
-keep class * extends com.sun.jna.Structure { *; }
-keep class * implements com.sun.jna.Library { *; }
-keep class * implements com.sun.jna.Callback { *; }

# libtor.so
-keep class org.torproject.** { *; }

# libdashjbls.so
-keep class org.dashj.** { *; }

# libsecp256k1-jni.so
-keep class fr.acinq.secp256k1.** { *; }
# NOT org.bitcoin.** -- that pulls in bitcoinj's protobuf classes, which
# reference protobuf-full symbols absent from the runtime classpath and
# fail R8 with "Missing class com.google.protobuf.UnknownFieldSet".
-keep class org.bitcoin.NativeSecp256k1 { *; }
-keep class org.bitcoin.NativeSecp256k1Util { *; }
-keep class org.bitcoin.Secp256k1Context { *; }

# libsodium.so / liblibsodium.so
-keep class com.goterl.** { *; }
-keep class org.libsodium.** { *; }

# libuniffi_yttrium_wcpay.so — UniFFI generates name-bound bindings
-keep class uniffi.** { *; }

# ============================================================
# Classes that load PACKAGE-RELATIVE classpath resources
# ============================================================
# Unstoppable Domains' Client.<clinit> does, in bytecode:
#     Client.class.getResourceAsStream("client.json")
# No leading slash, so the JVM resolves it relative to the class's OWN
# package: /com/unstoppabledomains/config/client/client.json
# R8 renamed Client -> vl1 in the ROOT package, so the lookup became
# /client.json -> null -> new InputStreamReader(null) -> NPE. It crashed
# the app when opening a token's info screen (ConfiguredTokenInfoViewModel):
#     java.lang.ExceptionInInitializerError
#     Caused by: java.lang.NullPointerException
#       at java.io.Reader.<init>(Reader.java:167)
#       at vl1.<clinit>
# The .json files are still packaged correctly; only the class moved.
# Keeping the package intact is what makes the relative lookup resolve.
# Any library that reads a resource sitting next to its own class needs
# the same treatment -- grep the APK for non-class files under package
# directories to find them.
-keep class com.unstoppabledomains.** { *; }
