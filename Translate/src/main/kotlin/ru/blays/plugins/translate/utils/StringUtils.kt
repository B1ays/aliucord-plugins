package ru.blays.plugins.translate.utils

import java.io.IOException
import java.io.StringWriter
import java.io.Writer

object TranslateUnescaper {

    @Throws(IOException::class)
    fun unescape(input: String): String {
        val writer = StringWriter(input.length * 2)

        var pos = 0
        val len = input.length
        while (pos < len) {
            val consumed: Int = translate(input, pos, writer)
            if (consumed == 0) {
                // inlined implementation of Character.toChars(Character.codePointAt(input, pos))
                // avoids allocating temp char arrays and duplicate checks
                val c1 = input[pos]
                writer.write(c1.code)
                pos++
                if (Character.isHighSurrogate(c1) && pos < len) {
                    val c2 = input[pos]
                    if (Character.isLowSurrogate(c2)) {
                        writer.write(c2.code)
                        pos++
                    }
                }
                continue
            }
            // contract with translators is that they have to understand code points
            // and they just took care of a surrogate pair
            for (pt in 0 until consumed) {
                pos += Character.charCount(Character.codePointAt(input, pos))
            }
        }
        return writer.toString()
    }

    /**
     * {@inheritDoc}
     */
    @Throws(IOException::class)
    private fun translate(input: String, index: Int, writer: Writer): Int {
        if (input[index] == '\\' && index + 1 < input.length && input[index + 1] == 'u') {
            // consume optional additional 'u' chars
            var i = 2
            while (index + i < input.length && input[index + i] == 'u') {
                i++
            }

            if (index + i < input.length && input[index + i] == '+') {
                i++
            }

            if (index + i + 4 <= input.length) {
                // Get 4 hex digits
                val unicode = input.subSequence(index + i, index + i + 4)

                try {
                    val value = unicode.toString().toInt(16)
                    writer.write(value.toChar().code)
                } catch (nfe: NumberFormatException) {
                    throw IllegalArgumentException("Unable to parse unicode value: $unicode", nfe)
                }
                return i + 4
            }
            throw IllegalArgumentException(
                "Less than 4 hex digits in unicode value: '"
                        + input.subSequence(index, input.length)
                        + "' due to end of CharSequence"
            )
        }
        return 0
    }
}