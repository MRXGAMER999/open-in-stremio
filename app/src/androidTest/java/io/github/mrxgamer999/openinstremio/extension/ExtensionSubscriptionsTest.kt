package io.github.mrxgamer999.openinstremio.extension

import android.content.ComponentName
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Pins the pairing store to the file, key and format that are already on disk. Nothing shares this
 * store any more, but every installed user has one: change the layout and they are all silently
 * unpaired from SeriesGuide until they notice the button is gone and toggle the extension again.
 */
class ExtensionSubscriptionsTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val prefs = context.getSharedPreferences(ExtensionSubscriptions.PREFS_FILE, Context.MODE_PRIVATE)
    private val subscriptions = ExtensionSubscriptions(context)
    // What SeriesGuide actually sends: new ComponentName(context, ExtensionActionReceiver.class).
    private val seriesGuide =
        ComponentName(
            "com.battlelancer.seriesguide",
            "com.battlelancer.seriesguide.extensions.ExtensionActionReceiver",
        )

    @Before
    @After
    fun clear() {
        prefs.edit().clear().commit()
    }

    @Test
    fun prefsFile_matchesTheFormatAlreadyOnDisk() {
        // Also the path excluded in backup_rules.xml / data_extraction_rules.xml.
        assertEquals("seriesguideextension_OpenInStremioExtension", ExtensionSubscriptions.PREFS_FILE)
    }

    @Test
    fun subscribe_storesLibraryReadableEntry() {
        val active = subscriptions.setSubscription(seriesGuide, "token-1")

        assertTrue(active)
        assertEquals(
            setOf("${seriesGuide.flattenToShortString()}|token-1"),
            prefs.getStringSet(ExtensionSubscriptions.PREF_SUBSCRIPTIONS, null),
        )
        assertEquals(mapOf(seriesGuide to "token-1"), subscriptions.subscribers())
    }

    @Test
    fun resubscribe_replacesTheToken() {
        subscriptions.setSubscription(seriesGuide, "old")

        subscriptions.setSubscription(seriesGuide, "new")

        assertEquals(mapOf(seriesGuide to "new"), subscriptions.subscribers())
    }

    @Test
    fun emptyToken_unsubscribes() {
        subscriptions.setSubscription(seriesGuide, "token-1")

        val active = subscriptions.setSubscription(seriesGuide, null)

        assertFalse(active)
        assertTrue(subscriptions.subscribers().isEmpty())
    }

    @Test
    fun unsubscribe_keepsOtherSubscribers() {
        val other = ComponentName("com.example.other", "com.example.other.Receiver")
        subscriptions.setSubscription(seriesGuide, "token-1")
        subscriptions.setSubscription(other, "token-2")

        val active = subscriptions.setSubscription(seriesGuide, "")

        assertTrue(active)
        assertEquals(mapOf(other to "token-2"), subscriptions.subscribers())
    }

    @Test
    fun shortClassName_stillUnsubscribes() {
        // Flattening drops the package prefix and unflattening puts it back, so a subscriber named
        // with a short class name must still match the entry it wrote.
        val shortForm = ComponentName("com.example.app", ".Receiver")
        subscriptions.setSubscription(shortForm, "token-1")

        val active = subscriptions.setSubscription(shortForm, null)

        assertFalse(active)
        assertTrue(subscriptions.subscribers().isEmpty())
    }

    @Test
    fun malformedEntries_areIgnored() {
        prefs
            .edit()
            .putStringSet(
                ExtensionSubscriptions.PREF_SUBSCRIPTIONS,
                setOf("no-separator", "|orphan-token", "${seriesGuide.flattenToShortString()}|", "missing-slash|t"),
            )
            .commit()

        assertTrue(subscriptions.subscribers().isEmpty())
    }
}
