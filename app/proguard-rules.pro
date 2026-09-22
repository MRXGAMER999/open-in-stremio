# Release builds are shrunk with R8. Retrofit, OkHttp, kotlinx.serialization, DataStore and the
# AndroidX libraries ship their own consumer rules; the manifest keeps the receiver and activities.
#
# The SeriesGuide extension API ships no rules. It builds Actions as Bundles keyed by string
# constants (no reflection), so shrinking it is safe in principle, but it is the contract with
# another app and is a handful of classes: keep it whole rather than bet the button on R8.
-keep class com.battlelancer.seriesguide.api.** { *; }
