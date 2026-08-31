package pl.prodevcode.learnmobiledev.presentation.theme

import androidx.compose.ui.unit.dp

/**
 * Shared application spacing.
 *
 * One place instead of scattered `16.dp` literals: changing the screen margin does not
 * require searching every UI file or risk making views drift apart.
 *
 * Watch the **stacking**: the screen margin is added to the card padding. With
 * 20 dp + 16 dp, text would start 36 dp from the edge, which eats a noticeable amount of
 * width on a phone. That is why the outer margin is deliberately small: the card adds
 * the breathing room.
 */
object Spacing {
    /**
     * Horizontal margin for screen content (outside cards).
     *
     * Deliberately small: cards have their own [cardPadding], so their interior provides
     * the breathing room from the screen edge. A larger value would only narrow the text
     * column.
     */
    val screenHorizontal = 8.dp

    /** Spacing from the top edge of the safe area. */
    val screenTop = 12.dp

    /**
     * Bottom margin for lists. It must be clearly larger than internal spacing so the last
     * item does not hide under the bottom navigation.
     */
    val listBottom = 32.dp

    val itemSpacing = 10.dp

    /** Padding inside a card: this is where content needs breathing room from the card edge. */
    val cardPadding = 14.dp

    val small = 8.dp
}
