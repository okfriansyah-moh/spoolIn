package com.spoolin.core.features.account.persistence

import com.ibm.icu.lang.UCharacter
import com.ibm.icu.lang.UCharacterCategory
import com.ibm.icu.lang.UProperty
import com.ibm.icu.text.IDNA
import com.ibm.icu.text.Normalizer2
import com.ibm.icu.text.UnicodeSet
import java.util.Locale

class Idna2008 {
    private val idna =
        IDNA.getUTS46Instance(
            IDNA.USE_STD3_RULES or IDNA.CHECK_BIDI or IDNA.CHECK_CONTEXTJ or IDNA.CHECK_CONTEXTO or
                IDNA.NONTRANSITIONAL_TO_ASCII or IDNA.NONTRANSITIONAL_TO_UNICODE,
        )
    private val nfkc = Normalizer2.getNFKCInstance()
    private val exceptions =
        UnicodeSet(
            "[\\u00df\\u03c2\\u06fd\\u06fe\\u0f0b\\u3007" +
                "\\u00b7\\u0375\\u05f3\\u05f4\\u30fb\\u0660-\\u0669\\u06f0-\\u06f9]",
        ).freeze()
    private val disallowed = UnicodeSet("[\\u0640\\u07fa\\u302e\\u302f\\u3031-\\u3035\\u303b]").freeze()
    private val hostCharacters = UnicodeSet("[a-z0-9\\-\\u200c\\u200d]").freeze()
    private val ignoredBlocks = UnicodeSet("[\\u20d0-\\u20ff\\U0001d100-\\U0001d1ff\\U0001d200-\\U0001d24f]").freeze()
    private val letterDigits =
        setOf(
            UCharacterCategory.UPPERCASE_LETTER.toInt(),
            UCharacterCategory.LOWERCASE_LETTER.toInt(),
            UCharacterCategory.MODIFIER_LETTER.toInt(),
            UCharacterCategory.OTHER_LETTER.toInt(),
            UCharacterCategory.NON_SPACING_MARK.toInt(),
            UCharacterCategory.COMBINING_SPACING_MARK.toInt(),
            UCharacterCategory.DECIMAL_DIGIT_NUMBER.toInt(),
        )

    fun ascii(domain: String): String {
        val lowered = Normalizer2.getNFCInstance().normalize(UCharacter.toLowerCase(Locale.ROOT, domain))
        require(lowered.isNotEmpty() && !lowered.endsWith('.')) { "Email domain is invalid" }
        val info = IDNA.Info()
        val unicode = idna.nameToUnicode(lowered, StringBuilder(), info).toString()
        require(!info.hasErrors()) { "Email domain is invalid" }
        lowered.split('.').filterNot { it.startsWith("xn--") }.forEach { validateLabel(it) }
        unicode.split('.').forEach { validateLabel(it) }
        val result = idna.nameToASCII(lowered, StringBuilder(), info).toString()
        require(!info.hasErrors() && result.length <= MAX_DOMAIN_OCTETS) { "Email domain is invalid" }
        return result
    }

    private fun validateLabel(label: String) {
        require(label.isNotEmpty() && label.codePoints().allMatch { permitted(it) }) { "Email domain is invalid" }
    }

    // RFC 5892 section 3 orders exceptions before stability/category rules.
    // ICU supplies Unicode 17 properties and contextual checks.
    private fun permitted(cp: Int): Boolean =
        when {
            exceptions.contains(cp) -> true
            disallowed.contains(cp) -> false
            !UCharacter.isDefined(cp) -> false
            hostCharacters.contains(cp) -> true
            excluded(cp) -> false
            else -> UCharacter.getType(cp) in letterDigits
        }

    private fun excluded(cp: Int): Boolean = unstable(cp) || ignorable(cp) || ignoredBlocks.contains(cp) || oldJamo(cp)

    private fun oldJamo(cp: Int): Boolean =
        UCharacter.getIntPropertyValue(cp, UProperty.HANGUL_SYLLABLE_TYPE) in
            setOf(
                UCharacter.HangulSyllableType.LEADING_JAMO,
                UCharacter.HangulSyllableType.VOWEL_JAMO,
                UCharacter.HangulSyllableType.TRAILING_JAMO,
            )

    private fun unstable(cp: Int): Boolean {
        val text = String(Character.toChars(cp))
        return nfkc.normalize(UCharacter.foldCase(nfkc.normalize(text), true)) != text
    }

    private fun ignorable(cp: Int): Boolean =
        listOf(
            UProperty.DEFAULT_IGNORABLE_CODE_POINT,
            UProperty.WHITE_SPACE,
            UProperty.NONCHARACTER_CODE_POINT,
        ).any { UCharacter.hasBinaryProperty(cp, it) }
}

private const val MAX_DOMAIN_OCTETS = 253
