package top.maary.emojiface.util

import java.text.BreakIterator
import java.util.Locale

fun splitEmoji(text: String): List<String> {
    val breaker = BreakIterator.getCharacterInstance(Locale.getDefault())
    breaker.setText(text)
    val result = mutableListOf<String>()
    var start = breaker.first()
    var end = breaker.next()
    while (end != BreakIterator.DONE) {
        result.add(text.substring(start, end))
        start = end
        end = breaker.next()
    }
    return result
}