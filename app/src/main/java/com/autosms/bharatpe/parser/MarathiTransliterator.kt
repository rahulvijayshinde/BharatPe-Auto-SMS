package com.autosms.bharatpe.parser

/**
 * Converts English (Latin script) Indian/Marathi names into Marathi (Devanagari script).
 *
 * Uses a hybrid approach:
 * 1. Curated dictionary of common Marathi first names, middle names, and surnames.
 * 2. Rule-based phonetic transliteration engine for unlisted names.
 *
 * Examples:
 *   "RINKAL RAVINDR CHAURPAGAR" -> "रिंकल रवींद्र चौरपगार"
 *   "Miss DISHA SURESH RANDIVE" -> "दिशा सुरेश रणदिवे"
 *   "RAHUL VIJAY SHINDE"        -> "राहुल विजय शिंदे"
 */
object MarathiTransliterator {

    /**
     * Common prefixes/honorifics to strip or convert.
     */
    private val HONORIFICS = mapOf(
        "MISS" to "",
        "MRS" to "",
        "MR" to "",
        "MS" to "",
        "SMT" to "",
        "SHRIMATI" to "",
        "SHRI" to "",
        "SHREE" to "",
        "DR" to "डॉ."
    )

    /**
     * Dictionary of common Marathi first names, middle names, and surnames.
     * Guaranteed 100% accurate spellings for frequent names.
     */
    private val NAME_DICTIONARY = mapOf(
        // First & Middle Names
        "RINKAL" to "रिंकल",
        "RINKLE" to "रिंकल",
        "RAVINDRA" to "रवींद्र",
        "RAVINDR" to "रवींद्र",
        "RAVINDAR" to "रवींद्र",
        "DISHA" to "दिशा",
        "SURESH" to "सुरेश",
        "RAHUL" to "राहुल",
        "VIJAY" to "विजय",
        "PRIYANKA" to "प्रियंका",
        "SACHIN" to "सचिन",
        "AMIT" to "अमित",
        "ANIKET" to "अनिकेत",
        "PRADIP" to "प्रदीप",
        "PRADEEP" to "प्रदीप",
        "PRAVIN" to "प्रवीण",
        "PRASHANT" to "प्रशांत",
        "GANESH" to "गणेश",
        "MAHESH" to "महेश",
        "DINESH" to "दिनेश",
        "RAMESH" to "रमेश",
        "RAJESH" to "राजेश",
        "SUNIL" to "सुनील",
        "ANIL" to "अनिल",
        "SANJAY" to "संजय",
        "AJAY" to "अजय",
        "VIKRAM" to "विक्रम",
        "VISHAL" to "विशाल",
        "ROHIT" to "रोहित",
        "SAGAR" to "सागर",
        "SWAPNIL" to "स्वप्नील",
        "NITIN" to "नितीन",
        "POOJA" to "पूजा",
        "PUJA" to "पूजा",
        "SNEHAL" to "स्नेहल",
        "NEHA" to "नेहा",
        "ANITA" to "अनिता",
        "SUNITA" to "सुनिता",
        "KAVITA" to "कविता",
        "SHEETAL" to "शीतल",
        "SHITAL" to "शीतल",
        "VAISHALI" to "वैशाली",
        "ASHWINI" to "अश्विनी",
        "PRANITA" to "प्रणिता",
        "MONIKA" to "मोनिका",
        "SHILPA" to "शिल्पा",
        "RASHMI" to "रश्मी",
        "ARTI" to "आरती",
        "AARTI" to "आरती",
        "KALYANI" to "कल्याणी",
        "RUTUJA" to "ऋतुजा",
        "PRATIKSHA" to "प्रतीक्षा",
        "TEJAS" to "तेजस",
        "PRASAD" to "प्रसाद",
        "AMOL" to "अमोल",
        "OMKAR" to "ओंकार",
        "AKSHAY" to "अक्षय",
        "ABHISHEK" to "अभिषेक",
        "SHUBHAM" to "शुभम",
        "SAURABH" to "सौरभ",
        "SOURABH" to "सौरभ",
        "VAIBHAV" to "वैभव",
        "CHETAN" to "चेतन",
        "MANOJ" to "मनोज",
        "PRAMOD" to "प्रमोद",
        "VINOD" to "विनोद",
        "SANTOSH" to "संतोष",
        "DILIP" to "दिलीप",
        "DEEPAK" to "दीपक",
        "DIPAK" to "दीपक",
        "PRAKASH" to "प्रकाश",
        "KISHOR" to "किशोर",
        "ASHOK" to "अशोक",
        "SUREKHA" to "सुरेखा",
        "LAXMAN" to "लक्ष्मण",
        "LAKSHMAN" to "लक्ष्मण",
        "SHANKAR" to "शंकर",
        "MARUTI" to "मारुती",
        "TUKARAM" to "तुकाराम",
        "DATTATRAY" to "दत्तात्रय",
        "PANDURANG" to "पांडुरंग",
        "ANAND" to "आनंद",
        "GOVIND" to "गोविंद",
        "BHAGWAN" to "भगवान",
        "SHARAD" to "शरद",
        "CHANDRAKANT" to "चंद्रकांत",
        "SUDHIR" to "सुधीर",
        "SANDIP" to "संदीप",
        "SANDEEP" to "संदीप",
        "VIKAS" to "विकास",
        "AVINASH" to "अविनाश",
        "SURAJ" to "सूरज",
        "SUMIT" to "सुमित",
        "KARTIK" to "कार्तिक",
        "MAYUR" to "मयूर",
        "SIDDHESH" to "सिद्धेश",
        "SANKET" to "संकेत",
        "HARSHAL" to "हर्षल",
        "ROSHAN" to "रोशन",
        "NIKHIL" to "निखिल",
        "YOGESH" to "योगेश",
        "MANGESH" to "मंगेश",
        "PANKAJ" to "पंकज",

        // Surnames
        "CHAURPAGAR" to "चौरपगार",
        "CHAVHAN" to "चव्हाण",
        "CHAVAN" to "चव्हाण",
        "RANDIVE" to "रणदिवे",
        "RANDEEVE" to "रणदिवे",
        "SHINDE" to "शिंदे",
        "PATIL" to "पाटील",
        "JADHAV" to "जाधव",
        "PAWAR" to "पवार",
        "KADAM" to "कदम",
        "GAIKWAD" to "गायकवाड",
        "GAYAKWAD" to "गायकवाड",
        "MORE" to "मोरे",
        "SHETTY" to "शेट्टी",
        "DESHMUKH" to "देशमुख",
        "JOSHI" to "जोशी",
        "KULKARNI" to "कुलकर्णी",
        "SAWANT" to "सावंत",
        "BHOSALE" to "भोसले",
        "BHOSLE" to "भोसले",
        "MANE" to "माने",
        "KAMBLE" to "कांबळे",
        "THORAT" to "थोरात",
        "SHARMA" to "शर्मा",
        "VERMA" to "वर्मा",
        "GUPTA" to "गुप्ता",
        "SINGH" to "सिंग",
        "YADAV" to "यादव",
        "KHAN" to "खान",
        "KALE" to "काळे",
        "JAGTAP" to "जगताप",
        "NAIK" to "नाईक",
        "RAUT" to "राऊत",
        "SALUNKE" to "साळुंके",
        "SALUNKHE" to "साळुंखे",
        "WAGH" to "वाघ",
        "SHAH" to "शाह",
        "AGRAWAL" to "अगरवाल",
        "AGARWAL" to "अगरवाल",
        "JAIN" to "जैन",
        "MISHRA" to "मिश्रा",
        "PANDEY" to "पांडे",
        "TIWARI" to "तिवारी",
        "CHOUDHARY" to "चौधरी",
        "CHOUDHARI" to "चौधरी",
        "THAKUR" to "ठाकूर",
        "BHANDARI" to "भंडारी",
        "KHANNA" to "खन्ना",
        "MEHTA" to "मेहता",
        "TAMBE" to "तांबे",
        "SABLE" to "साबळे",
        "GHADGE" to "घाडगे",
        "GORE" to "गोरे",
        "GITE" to "गिते",
        "DHAWALE" to "ढवळे",
        "DHAVALE" to "ढवळे",
        "SONAWANE" to "सोनवणे",
        "SURYAWANSHI" to "सूर्यवंशी",
        "LOKHANDE" to "लोखंडे",
        "LONDHE" to "लोंढे",
        "INGLE" to "इंगळे",
        "KHARE" to "खरे",
        "GHARAT" to "घरत",
        "MHATRE" to "म्हात्रे",
        "THAKARE" to "ठाकरे",
        "THACKERAY" to "ठाकरे",
        "KHANDEKAR" to "खांडेकर",
        "SATHE" to "साठे",
        "KAKADE" to "काकडे",
        "NARAYAN" to "नारायण",
        "CHOUGULE" to "चौगुले",
        "DHUMAL" to "धुमाळ",
        "WADIKAR" to "वाडीकर",
        "PANDIT" to "पंडित",
        "KAPOOR" to "कपूर",
        "KAPUR" to "कपूर",
        "CHOPRA" to "चोप्रा",
        "MALHOTRA" to "मल्होत्रा",
        "BHAT" to "भट",
        "BHATT" to "भट्ट",
        "PRABHU" to "प्रभू",
        "HEGDE" to "हेगडे",
        "REDDY" to "रेड्डी",
        "NAIR" to "नायर",
        "PILLAI" to "पिल्लई",
        "MENON" to "मेनन"
    )

    /**
     * Convert a full name (potentially multiple words) to Marathi Devanagari.
     *
     * @param englishName The name in English (e.g. "RINKAL RAVINDR CHAURPAGAR")
     * @return The name in Marathi (e.g. "रिंकल रवींद्र चौरपगार")
     */
    fun toMarathi(englishName: String): String {
        if (englishName.isBlank()) return ""

        val words = englishName.trim().split(Regex("\\s+"))
        val marathiWords = mutableListOf<String>()

        for (word in words) {
            val clean = word.replace(Regex("[^a-zA-Z]"), "").uppercase()
            if (clean.isBlank()) continue

            // 1. Check honorifics
            if (HONORIFICS.containsKey(clean)) {
                val prefix = HONORIFICS[clean] ?: ""
                if (prefix.isNotBlank()) {
                    marathiWords.add(prefix)
                }
                continue
            }

            // 2. Check dictionary
            val dictMatch = NAME_DICTIONARY[clean]
            if (dictMatch != null) {
                marathiWords.add(dictMatch)
                continue
            }

            // 3. Fallback to phonetic transliterator
            marathiWords.add(transliterateWord(clean.lowercase()))
        }

        return if (marathiWords.isNotEmpty()) {
            marathiWords.joinToString(" ")
        } else {
            englishName.trim()
        }
    }

    /**
     * Rule-based phonetic transliteration of an English word to Devanagari.
     */
    private fun transliterateWord(w: String): String {
        val result = StringBuilder()
        var i = 0
        val len = w.length
        var prevWasConsonant = false

        while (i < len) {
            // Check 4-character clusters
            if (i + 4 <= len) {
                val c4 = w.substring(i, i + 4)
                if (c4 == "ndra") {
                    result.append("ंद्र")
                    i += 4
                    prevWasConsonant = false
                    continue
                }
            }

            // Check 3-character clusters
            if (i + 3 <= len) {
                val c3 = w.substring(i, i + 3)
                when (c3) {
                    "ndr" -> { result.append("ंद्र"); i += 3; prevWasConsonant = false; continue }
                    "chh" -> { appendConsonant(result, "छ", prevWasConsonant); i += 3; prevWasConsonant = true; continue }
                    "ksh" -> { appendConsonant(result, "क्ष", prevWasConsonant); i += 3; prevWasConsonant = true; continue }
                    "shr" -> { appendConsonant(result, "श्र", prevWasConsonant); i += 3; prevWasConsonant = true; continue }
                    "dny", "jny" -> { appendConsonant(result, "ज्ञ", prevWasConsonant); i += 3; prevWasConsonant = true; continue }
                    "nth" -> { result.append("ंथ"); i += 3; prevWasConsonant = false; continue }
                }
            }

            // Check 2-character clusters
            if (i + 2 <= len) {
                val c2 = w.substring(i, i + 2)
                when (c2) {
                    "kh" -> { appendConsonant(result, "ख", prevWasConsonant); i += 2; prevWasConsonant = true; continue }
                    "gh" -> { appendConsonant(result, "घ", prevWasConsonant); i += 2; prevWasConsonant = true; continue }
                    "ch" -> { appendConsonant(result, "च", prevWasConsonant); i += 2; prevWasConsonant = true; continue }
                    "jh", "zh" -> { appendConsonant(result, "झ", prevWasConsonant); i += 2; prevWasConsonant = true; continue }
                    "th" -> { appendConsonant(result, "थ", prevWasConsonant); i += 2; prevWasConsonant = true; continue }
                    "dh" -> { appendConsonant(result, "ध", prevWasConsonant); i += 2; prevWasConsonant = true; continue }
                    "ph" -> { appendConsonant(result, "फ", prevWasConsonant); i += 2; prevWasConsonant = true; continue }
                    "bh" -> { appendConsonant(result, "भ", prevWasConsonant); i += 2; prevWasConsonant = true; continue }
                    "sh" -> { appendConsonant(result, "श", prevWasConsonant); i += 2; prevWasConsonant = true; continue }
                    "tr" -> { appendConsonant(result, "त्र", prevWasConsonant); i += 2; prevWasConsonant = true; continue }
                    "nd" -> { result.append("ंद"); i += 2; prevWasConsonant = false; continue }
                    "nk" -> { result.append("ंक"); i += 2; prevWasConsonant = false; continue }
                    "ng" -> { result.append("ंग"); i += 2; prevWasConsonant = false; continue }
                    "nt" -> { result.append("ंत"); i += 2; prevWasConsonant = false; continue }
                    "mb" -> { result.append("ंब"); i += 2; prevWasConsonant = false; continue }
                    "mp" -> { result.append("ंप"); i += 2; prevWasConsonant = false; continue }

                    // Vowels (2 chars)
                    "aa" -> {
                        if (prevWasConsonant) result.append("ा") else result.append("आ")
                        i += 2
                        prevWasConsonant = false
                        continue
                    }
                    "ee" -> {
                        if (prevWasConsonant) result.append("ी") else result.append("ई")
                        i += 2
                        prevWasConsonant = false
                        continue
                    }
                    "oo" -> {
                        if (prevWasConsonant) result.append("ू") else result.append("ऊ")
                        i += 2
                        prevWasConsonant = false
                        continue
                    }
                    "ai" -> {
                        if (prevWasConsonant) result.append("ै") else result.append("ऐ")
                        i += 2
                        prevWasConsonant = false
                        continue
                    }
                    "au", "ou" -> {
                        if (prevWasConsonant) result.append("ौ") else result.append("औ")
                        i += 2
                        prevWasConsonant = false
                        continue
                    }
                }
            }

            // Single characters
            val c = w[i]
            when (c) {
                // Vowels
                'a' -> {
                    // In Indian names: if at the end of word and previous was consonant, usually silent 'a' or 'aa'
                    // e.g. "DISHA" -> 'ा', "KADAM" -> second 'a' is inherent
                    if (prevWasConsonant) {
                        if (i == len - 1) {
                            result.append("ा")
                        }
                        // Middle 'a' is inherent in consonant, do not append matra
                    } else {
                        result.append("अ")
                    }
                    prevWasConsonant = false
                }
                'i' -> {
                    if (prevWasConsonant) result.append("ि") else result.append("इ")
                    prevWasConsonant = false
                }
                'u' -> {
                    if (prevWasConsonant) result.append("ु") else result.append("उ")
                    prevWasConsonant = false
                }
                'e' -> {
                    if (prevWasConsonant) result.append("े") else result.append("ए")
                    prevWasConsonant = false
                }
                'o' -> {
                    if (prevWasConsonant) result.append("ो") else result.append("ओ")
                    prevWasConsonant = false
                }

                // Consonants
                'k' -> { appendConsonant(result, "क", prevWasConsonant); prevWasConsonant = true }
                'g' -> { appendConsonant(result, "ग", prevWasConsonant); prevWasConsonant = true }
                'j' -> { appendConsonant(result, "ज", prevWasConsonant); prevWasConsonant = true }
                't' -> { appendConsonant(result, "त", prevWasConsonant); prevWasConsonant = true }
                'd' -> { appendConsonant(result, "द", prevWasConsonant); prevWasConsonant = true }
                'n' -> { appendConsonant(result, "न", prevWasConsonant); prevWasConsonant = true }
                'p' -> { appendConsonant(result, "प", prevWasConsonant); prevWasConsonant = true }
                'f' -> { appendConsonant(result, "फ", prevWasConsonant); prevWasConsonant = true }
                'b' -> { appendConsonant(result, "ब", prevWasConsonant); prevWasConsonant = true }
                'm' -> { appendConsonant(result, "म", prevWasConsonant); prevWasConsonant = true }
                'y' -> { appendConsonant(result, "य", prevWasConsonant); prevWasConsonant = true }
                'r' -> { appendConsonant(result, "र", prevWasConsonant); prevWasConsonant = true }
                'l' -> { appendConsonant(result, "ल", prevWasConsonant); prevWasConsonant = true }
                'v', 'w' -> { appendConsonant(result, "व", prevWasConsonant); prevWasConsonant = true }
                's' -> { appendConsonant(result, "स", prevWasConsonant); prevWasConsonant = true }
                'h' -> { appendConsonant(result, "ह", prevWasConsonant); prevWasConsonant = true }
                'c' -> { appendConsonant(result, "क", prevWasConsonant); prevWasConsonant = true }
                'x' -> { appendConsonant(result, "क्स", prevWasConsonant); prevWasConsonant = true }
                'z' -> { appendConsonant(result, "झ", prevWasConsonant); prevWasConsonant = true }

                else -> {
                    // Unknown character, skip
                    prevWasConsonant = false
                }
            }
            i++
        }

        return result.toString()
    }

    private fun appendConsonant(sb: StringBuilder, dev: String, prevWasConsonant: Boolean) {
        // If two consonants appear adjacent without vowel (e.g. "SK"), join with halant
        if (prevWasConsonant && sb.isNotEmpty() && !sb.endsWith("ं") && !sb.endsWith("्")) {
            sb.append("्")
        }
        sb.append(dev)
    }
}
