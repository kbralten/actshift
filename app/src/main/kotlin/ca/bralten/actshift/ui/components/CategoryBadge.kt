package ca.bralten.actshift.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ca.bralten.actshift.domain.model.ActivityCategory
import ca.bralten.actshift.ui.theme.LocalActShiftColors

@Composable
fun CategoryBadge(
  category: ActivityCategory,
  modifier: Modifier = Modifier,
) {
  val semantic = LocalActShiftColors.current
  val (container, onContainer) =
    when (category) {
      ActivityCategory.PLEASURE -> semantic.pleasureContainer to semantic.onPleasureContainer
      ActivityCategory.ACHIEVEMENT -> semantic.achievementContainer to semantic.onAchievementContainer
      ActivityCategory.BOTH -> semantic.bothContainer to semantic.onBothContainer
    }

  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(999.dp),
    color = container,
  ) {
    Text(
      text = category.label,
      style = MaterialTheme.typography.labelSmall,
      color = onContainer,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
    )
  }
}
