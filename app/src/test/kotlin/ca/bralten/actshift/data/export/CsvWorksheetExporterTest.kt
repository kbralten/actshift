package ca.bralten.actshift.data.export

import ca.bralten.actshift.domain.model.ActivationLog
import ca.bralten.actshift.domain.model.ActivityCategory
import ca.bralten.actshift.domain.model.ActivityStatus
import ca.bralten.actshift.domain.model.ActivityWithLog
import ca.bralten.actshift.domain.model.ScheduledActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class CsvWorksheetExporterTest {

  private val utc: ZoneId = ZoneId.of("UTC")

  @Test
  fun exportsChronologicalCsvWithSignedDeltasAndRfc4180Escaping() {
    val firstTs = ZonedDateTime.of(2026, 11, 19, 9, 30, 0, 0, utc).toInstant().toEpochMilli()
    val secondTs = ZonedDateTime.of(2026, 11, 20, 14, 15, 0, 0, utc).toInstant().toEpochMilli()

    val secondItem = ActivityWithLog(
      activity = ScheduledActivity(
        id = 2L,
        templateId = null,
        title = "Errand, \"quick\" trip",
        category = ActivityCategory.BOTH,
        scheduledStartEpochMs = secondTs - 900_000L,
        durationMinutes = 15,
        status = ActivityStatus.COMPLETED,
        calendarEventId = null,
        calendarInstanceBeginMs = null,
        startedAtEpochMs = secondTs - 900_000L,
        timerEndEpochMs = null,
        completedAtEpochMs = secondTs,
        snoozeCount = 0,
      ),
      log = ActivationLog(
        id = 2L,
        activityId = 2L,
        preDepression = 5,
        prePleasure = 1,
        preAchievement = 1,
        preLoggedAtEpochMs = secondTs - 900_000L,
        postDepression = 2,
        postPleasure = 3,
        postAchievement = 4,
        postLoggedAtEpochMs = secondTs,
        reflectionNote = "Line one\nSaid \"okay\" after",
      ),
    )

    val firstItem = ActivityWithLog(
      activity = ScheduledActivity(
        id = 1L,
        templateId = null,
        title = "Get the mail",
        category = ActivityCategory.ACHIEVEMENT,
        scheduledStartEpochMs = firstTs - 600_000L,
        durationMinutes = 10,
        status = ActivityStatus.COMPLETED,
        calendarEventId = null,
        calendarInstanceBeginMs = null,
        startedAtEpochMs = firstTs - 600_000L,
        timerEndEpochMs = null,
        completedAtEpochMs = firstTs,
        snoozeCount = 0,
      ),
      log = ActivationLog(
        id = 1L,
        activityId = 1L,
        preDepression = 6,
        prePleasure = 0,
        preAchievement = 0,
        preLoggedAtEpochMs = firstTs - 600_000L,
        postDepression = 3,
        postPleasure = 0,
        postAchievement = 1,
        postLoggedAtEpochMs = firstTs,
        reflectionNote = null,
      ),
    )

    // Intentionally pass out of chronological order to verify sorting ascending
    val csv = CsvWorksheetExporter.export(listOf(secondItem, firstItem), zoneId = utc)
    val expectedHeader =
      "Date,Time,Activity,Category,Duration (min),Before Depression,After Depression,Depression Shift,Before Pleasure,After Pleasure,Pleasure Shift,Before Achievement,After Achievement,Achievement Shift,What Did You Notice?"
    assertTrue(csv.startsWith(expectedHeader))

    val expectedFirstRow = "2026-11-19,09:30,Get the mail,Achievement,10,6,3,-3,0,0,0,0,1,+1,"
    assertTrue(csv.contains(expectedFirstRow))

    val expectedEscapedTitle = "\"Errand, \"\"quick\"\" trip\""
    val expectedEscapedNote = "\"Line one\nSaid \"\"okay\"\" after\""
    assertTrue(csv.contains("2026-11-20,14:15,$expectedEscapedTitle,Both / Mixed,15,5,2,-3,1,3,+2,1,4,+3,$expectedEscapedNote"))

    assertEquals("plain", CsvWorksheetExporter.escapeCsvField("plain"))
    assertEquals("\"a,b\"", CsvWorksheetExporter.escapeCsvField("a,b"))
  }
}
