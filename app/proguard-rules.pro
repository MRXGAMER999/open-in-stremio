# Minification is intentionally disabled for this app (see README).
# The SeriesGuide extension API serializes Actions across process boundaries and
# the app is small enough that R8 gains are negligible. If minification is ever
# enabled, add keep rules for com.battlelancer.seriesguide.api.** and re-test the
# enable -> click flow in a release build on a real device.
