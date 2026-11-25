package ca.bralten.actshift.widget

fun interface WidgetUpdater {
  fun requestUpdate()

  companion object {
    val NoOp = WidgetUpdater {}
  }
}
