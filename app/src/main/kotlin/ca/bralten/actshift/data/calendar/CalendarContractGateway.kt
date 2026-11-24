package ca.bralten.actshift.data.calendar

data class CalendarInstanceRecord(
  val eventId: Long,
  val calendarId: Long,
  val title: String,
  val beginMs: Long,
  val endMs: Long,
) {
  val durationMinutes: Int
    get() = (((endMs - beginMs) / 60_000L).toInt()).coerceIn(1, 720)
}

/**
 * Abstraction over Android CalendarContract so sync logic can be tested purely on the JVM.
 */
interface CalendarContractGateway {
  fun hasCalendarPermissions(): Boolean

  fun findOrCreateDedicatedCalendar(): Long?

  fun upsertEvent(
    calendarId: Long,
    existingEventId: Long?,
    title: String,
    startEpochMs: Long,
    durationMinutes: Int,
    rrule: String?,
  ): Long?

  fun deleteEvent(calendarId: Long, eventId: Long): Boolean

  fun queryInstances(
    calendarId: Long,
    windowStartMs: Long,
    windowEndMs: Long,
  ): List<CalendarInstanceRecord>
}
