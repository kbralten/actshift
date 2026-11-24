package ca.bralten.actshift.data.calendar

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import android.provider.CalendarContract.Calendars
import android.provider.CalendarContract.Events
import android.provider.CalendarContract.Instances
import androidx.core.content.ContextCompat
import java.util.TimeZone

class AndroidCalendarGateway(
  private val context: Context,
) : CalendarContractGateway {

  override fun hasCalendarPermissions(): Boolean {
    val readGranted =
      ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) ==
        PackageManager.PERMISSION_GRANTED
    val writeGranted =
      ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) ==
        PackageManager.PERMISSION_GRANTED
    return readGranted && writeGranted
  }

  override fun findOrCreateDedicatedCalendar(): Long? {
    if (!hasCalendarPermissions()) return null

    val resolver = context.contentResolver
    val projection = arrayOf(Calendars._ID)
    val selection =
      "${Calendars.ACCOUNT_NAME} = ? AND ${Calendars.ACCOUNT_TYPE} = ? AND ${Calendars.CALENDAR_DISPLAY_NAME} = ?"
    val selectionArgs = arrayOf(ACCOUNT_NAME, ACCOUNT_TYPE, DISPLAY_NAME)

    resolver.query(Calendars.CONTENT_URI, projection, selection, selectionArgs, null)?.use { cursor ->
      if (cursor.moveToFirst()) {
        return cursor.getLong(0)
      }
    }

    val syncAdapterUri =
      Calendars.CONTENT_URI
        .buildUpon()
        .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
        .appendQueryParameter(Calendars.ACCOUNT_NAME, ACCOUNT_NAME)
        .appendQueryParameter(Calendars.ACCOUNT_TYPE, ACCOUNT_TYPE)
        .build()

    val values =
      ContentValues().apply {
        put(Calendars.ACCOUNT_NAME, ACCOUNT_NAME)
        put(Calendars.ACCOUNT_TYPE, ACCOUNT_TYPE)
        put(Calendars.NAME, INTERNAL_NAME)
        put(Calendars.CALENDAR_DISPLAY_NAME, DISPLAY_NAME)
        put(Calendars.CALENDAR_COLOR, CALENDAR_COLOR)
        put(Calendars.CALENDAR_ACCESS_LEVEL, Calendars.CAL_ACCESS_OWNER)
        put(Calendars.OWNER_ACCOUNT, ACCOUNT_NAME)
        put(Calendars.VISIBLE, 1)
        put(Calendars.SYNC_EVENTS, 1)
        put(Calendars.CALENDAR_TIME_ZONE, TimeZone.getDefault().id)
      }

    val insertedUri = runCatching { resolver.insert(syncAdapterUri, values) }.getOrNull() ?: return null
    return runCatching { ContentUris.parseId(insertedUri) }.getOrNull()
  }

  override fun upsertEvent(
    calendarId: Long,
    existingEventId: Long?,
    title: String,
    startEpochMs: Long,
    durationMinutes: Int,
    rrule: String?,
  ): Long? {
    if (!hasCalendarPermissions()) return null
    val resolver = context.contentResolver
    val tz = TimeZone.getDefault().id
    val safeDuration = durationMinutes.coerceAtLeast(1)

    val values =
      ContentValues().apply {
        put(Events.CALENDAR_ID, calendarId)
        put(Events.TITLE, title)
        put(Events.DESCRIPTION, "Scheduled with ActShift (Behavioural Activation)")
        put(Events.DTSTART, startEpochMs)
        put(Events.EVENT_TIMEZONE, tz)
        if (rrule.isNullOrBlank()) {
          put(Events.DTEND, startEpochMs + safeDuration * 60_000L)
          putNull(Events.DURATION)
          putNull(Events.RRULE)
        } else {
          put(Events.DURATION, "PT${safeDuration}M")
          put(Events.RRULE, rrule)
          putNull(Events.DTEND)
        }
      }

    if (existingEventId != null) {
      val updateUri = ContentUris.withAppendedId(Events.CONTENT_URI, existingEventId)
      val updatedRows = runCatching { resolver.update(updateUri, values, null, null) }.getOrDefault(0)
      if (updatedRows > 0) {
        return existingEventId
      }
    }

    val insertedUri = runCatching { resolver.insert(Events.CONTENT_URI, values) }.getOrNull() ?: return null
    return runCatching { ContentUris.parseId(insertedUri) }.getOrNull()
  }

  override fun deleteEvent(calendarId: Long, eventId: Long): Boolean {
    if (!hasCalendarPermissions()) return false
    val resolver = context.contentResolver
    val selection = "${Events._ID} = ? AND ${Events.CALENDAR_ID} = ?"
    val args = arrayOf(eventId.toString(), calendarId.toString())
    return runCatching { resolver.delete(Events.CONTENT_URI, selection, args) > 0 }.getOrDefault(false)
  }

  override fun queryInstances(
    calendarId: Long,
    windowStartMs: Long,
    windowEndMs: Long,
  ): List<CalendarInstanceRecord> {
    if (!hasCalendarPermissions()) return emptyList()
    val resolver = context.contentResolver
    val uriBuilder = Instances.CONTENT_URI.buildUpon()
    ContentUris.appendId(uriBuilder, windowStartMs)
    ContentUris.appendId(uriBuilder, windowEndMs)

    val projection =
      arrayOf(
        Instances.EVENT_ID,
        Instances.CALENDAR_ID,
        Instances.TITLE,
        Instances.BEGIN,
        Instances.END,
      )
    val selection = "${Instances.CALENDAR_ID} = ?"
    val selectionArgs = arrayOf(calendarId.toString())

    val results = mutableListOf<CalendarInstanceRecord>()
    runCatching {
      resolver
        .query(
          uriBuilder.build(),
          projection,
          selection,
          selectionArgs,
          "${Instances.BEGIN} ASC",
        )?.use { cursor ->
          val eventIdCol = cursor.getColumnIndexOrThrow(Instances.EVENT_ID)
          val calIdCol = cursor.getColumnIndexOrThrow(Instances.CALENDAR_ID)
          val titleCol = cursor.getColumnIndexOrThrow(Instances.TITLE)
          val beginCol = cursor.getColumnIndexOrThrow(Instances.BEGIN)
          val endCol = cursor.getColumnIndexOrThrow(Instances.END)

          while (cursor.moveToNext()) {
            val rowCalId = cursor.getLong(calIdCol)
            if (rowCalId != calendarId) continue
            val title = cursor.getString(titleCol)?.takeIf { it.isNotBlank() } ?: "Behavioural Activation"
            val begin = cursor.getLong(beginCol)
            val end = cursor.getLong(endCol).coerceAtLeast(begin + 5 * 60_000L)
            results.add(
              CalendarInstanceRecord(
                eventId = cursor.getLong(eventIdCol),
                calendarId = rowCalId,
                title = title,
                beginMs = begin,
                endMs = end,
              ),
            )
          }
        }
    }
    return results
  }

  companion object {
    const val ACCOUNT_NAME = "ActShift"
    const val ACCOUNT_TYPE = CalendarContract.ACCOUNT_TYPE_LOCAL
    const val INTERNAL_NAME = "actshift_behavioural_activation"
    const val DISPLAY_NAME = "Behavioural Activation"
    const val CALENDAR_COLOR = 0xFF5B7B6A.toInt()
  }
}
