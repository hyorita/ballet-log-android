package com.hyorita.balletlog.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.Normalizer
import java.util.Locale

// ──────────────────────────────────────────────────────────────────────────
// Language preference
// ──────────────────────────────────────────────────────────────────────────

enum class TermLanguage {
    System, English, Korean;

    /** Resolves System to either Korean or English based on device locale. */
    fun resolved(): TermLanguage {
        if (this != System) return this
        return if (Locale.getDefault().language == "ko") Korean else English
    }

    companion object {
        fun fromKey(key: String?): TermLanguage = when (key) {
            "english" -> English
            "korean" -> Korean
            else -> System
        }

        fun toKey(value: TermLanguage): String = when (value) {
            System -> "system"
            English -> "english"
            Korean -> "korean"
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────
// Term (immutable template)
// ──────────────────────────────────────────────────────────────────────────

data class BalletTerm(val english: String, val korean: String) {
    fun text(language: TermLanguage): String =
        if (language.resolved() == TermLanguage.Korean) korean else english
}

// ──────────────────────────────────────────────────────────────────────────
// Defaults + featured mapping
// ──────────────────────────────────────────────────────────────────────────

object BalletTerms {

    val defaults: List<BalletTerm> = listOf(
        // Featured (shown first when no usage history)
        BalletTerm("demi", "드미"),
        BalletTerm("grand", "그랑"),
        BalletTerm("plié", "플리에"),
        BalletTerm("1st", "1번"),
        BalletTerm("2nd", "2번"),
        BalletTerm("4th", "4번"),
        BalletTerm("5th", "5번"),

        // Positions
        BalletTerm("demi-pointe", "드미포인"),
        BalletTerm("preparation", "프레파라시옹"),

        // Barre basics
        BalletTerm("grand plié", "그랑플리에"),
        BalletTerm("relevé", "를르베"),
        BalletTerm("relevé lent", "를르베랑"),
        BalletTerm("élevé", "엘레베"),
        BalletTerm("tendu", "탄듀"),
        BalletTerm("dégagé", "데가제"),
        BalletTerm("frappé", "프라페"),
        BalletTerm("petit battement", "쁘띠바뜨망"),
        BalletTerm("battement", "바뜨망"),
        BalletTerm("battu", "바튜"),
        BalletTerm("grand jeté", "그랑제떼"),
        BalletTerm("rond", "론드"),
        BalletTerm("rond de jambe", "론드잠"),
        BalletTerm("fondu", "폰듀"),
        BalletTerm("développé", "데벨로뻬"),
        BalletTerm("enveloppé", "엔벨로뻬"),
        BalletTerm("retiré", "레티레"),

        // Center / jumps
        BalletTerm("arabesque", "아라베스크"),
        BalletTerm("attitude", "애티튜드"),
        BalletTerm("pirouette", "피루엣"),
        BalletTerm("fouetté", "훼떼"),
        BalletTerm("assemblé", "아쌈블레"),
        BalletTerm("sissonne", "시손느"),
        BalletTerm("glissade", "글리사드"),
        BalletTerm("chassé", "샷세"),
        BalletTerm("pas de chat", "파드샤"),
        BalletTerm("grand pas de chat", "그랑 파드샤"),
        BalletTerm("pas de bourrée", "파드브레"),
        BalletTerm("changement", "샹쥬망"),
        BalletTerm("entrechat", "앙트르샤"),
        BalletTerm("cabriole", "까브리올"),
        BalletTerm("sauté", "소떼"),
        BalletTerm("petit sauté", "스몰점프"),
        BalletTerm("temps levé", "탕르베"),
        BalletTerm("jeté", "제떼"),
        BalletTerm("tour jeté", "투르제떼"),
        BalletTerm("entrelacé", "앙트를라세"),
        BalletTerm("tour en l'air", "투르앙레르"),
        BalletTerm("ballonné", "발로네"),
        BalletTerm("ballotté", "발로떼"),
        BalletTerm("brisé", "브리제"),
        BalletTerm("emboîté", "앙부아떼"),
        BalletTerm("soubresaut", "수브르수"),

        // Port de bras / arms
        BalletTerm("port de bras", "폴드브라"),
        BalletTerm("allongé", "알론제"),
        BalletTerm("épaulement", "에뽈망"),
        BalletTerm("cambré", "깜블레"),

        // Turns / traveling
        BalletTerm("chaînés", "쉐네"),
        BalletTerm("piqué", "피께"),
        BalletTerm("déboulés", "피케턴"),
        BalletTerm("soutenu", "스튜뉴"),
        BalletTerm("demi-détourné", "하프턴"),
        BalletTerm("manèges", "마네쥬"),
        BalletTerm("promenade", "프로미나드"),
        BalletTerm("renversé", "랑베르세"),

        // Directions / modifiers
        BalletTerm("en avant", "아나방"),
        BalletTerm("en arrière", "앙아리에르"),
        BalletTerm("de côté", "드꼬떼"),
        BalletTerm("devant", "드방"),
        BalletTerm("derrière", "데리에"),
        BalletTerm("en dedans", "앙드당"),
        BalletTerm("en dehors", "앙디올"),
        BalletTerm("en croix", "엉크루아"),
        BalletTerm("en tournant", "앙트르낭"),
        BalletTerm("en face", "앙파스"),
        BalletTerm("en l'air", "앙레르"),
        BalletTerm("en haut", "앙오"),
        BalletTerm("en bas", "앙바"),
        BalletTerm("up", "업"),
        BalletTerm("à la seconde", "알라스콩"),
        BalletTerm("croisé", "크로아제"),
        BalletTerm("effacé", "에파세"),
        BalletTerm("écarté", "에까르떼"),
        BalletTerm("fermé", "페르메"),
        BalletTerm("ouvert", "우베르"),
        BalletTerm("double", "더블"),

        // Other steps
        BalletTerm("balancé", "발란세"),
        BalletTerm("pas de valse", "바드발스"),
        BalletTerm("pas de basque", "파드바스크"),
        BalletTerm("tombé", "톰베"),
        BalletTerm("coupé", "꾸뻬"),
        BalletTerm("passé", "파세"),
        BalletTerm("passé par terre", "파세파테르"),
        BalletTerm("temps lié", "탕리에"),
        BalletTerm("failli", "화이"),
        BalletTerm("penché", "팡쉐"),
        BalletTerm("flic-flac", "플릭플락"),
        BalletTerm("échappé", "에샤뻬"),
        BalletTerm("batterie", "바뜨리"),
        BalletTerm("bourrée", "부레"),
        BalletTerm("couru", "꾸루"),
        BalletTerm("pas couru", "파크루"),
        BalletTerm("sous-sus", "숫수"),
        BalletTerm("waltz", "왈츠"),
        BalletTerm("turnout", "턴아우"),
        BalletTerm("révérence", "레베랑스")
    )

    private val defaultOrderIndex: Map<String, Int> =
        defaults.mapIndexed { i, term -> term.english to i }.toMap()

    fun defaultOrder(term: BalletTerm): Int =
        defaultOrderIndex[term.english] ?: Int.MAX_VALUE

    // Chip sets — one constant per step family so the keyword table below
    // can map several language variants of the same step to the same list.
    private val pliéChips = listOf("demi", "grand", "plié", "1st", "2nd", "4th", "5th")
    private val tenduChips = listOf("tendu", "devant", "à la seconde", "derrière", "1st", "5th")
    private val jetéChips = listOf("jeté", "devant", "à la seconde", "derrière", "1st", "5th")
    private val fonduChips = listOf("fondu", "devant", "à la seconde", "derrière", "1st", "5th")
    private val frappéChips = listOf("frappé", "devant", "à la seconde", "derrière", "1st", "5th", "battu", "up", "petit battement")
    private val développéChips = listOf("passé", "développé", "enveloppé", "relevé lent", "rond", "devant", "à la seconde", "derrière", "écarté")
    private val àTerreChips = listOf("rond", "grand", "plié", "devant", "derrière", "battement", "passé", "développé", "écarté")
    private val enLAirChips = listOf("passé", "développé", "en dehors", "en dedans", "relevé", "up", "5th")
    private val grandBattementChips = listOf("grand", "battement", "devant", "à la seconde", "derrière", "1st", "5th", "en croix", "en avant", "en arrière", "de côté")
    private val adagioChips = listOf("croisé", "écarté", "effacé", "devant", "derrière", "passé", "à la seconde", "promenade", "allongé")
    private val waltzChips = listOf("balancé", "tombé", "pas de bourrée", "preparation", "4th", "pirouette", "en dehors", "en dedans", "renversé")
    private val allegro1Chips = listOf("sauté", "changement", "1st", "2nd", "4th", "5th", "entrechat", "soubresaut")
    private val allegro2Chips = listOf("glissade", "assemblé", "sauté", "soubresaut", "tombé", "brisé", "pas de bourrée")
    private val allegro3Chips = listOf("glissade", "jeté", "temps levé", "2nd", "pas de bourrée", "brisé")
    private val mediumAllegroChips = listOf("fermé", "failli", "assemblé", "tombé", "pas de bourrée", "grand", "ouvert", "pas de chat")
    private val grandAllegroChips = listOf("tombé", "pas de bourrée", "glissade", "grand", "pas de chat", "jeté", "piqué", "arabesque", "chassé", "entrelacé", "en tournant")

    /**
     * Step-name keyword → priority list of english keys. First entry whose
     * keyword the step-name contains (case-insensitive substring) wins.
     * Each step has en + ja + ko keyword variants so localized step names
     * (e.g. "탄듀", "タンデュ") match the same chip set as the English form.
     * Most specific keywords come first.
     */
    private val featuredByKeyword: List<Pair<String, List<String>>> = listOf(
        // Adagio
        "adagio" to adagioChips, "アダージョ" to adagioChips, "아다지오" to adagioChips,

        // Allegro 1/2/3 — must come before generic "allegro" mentions and
        // before "jeté" ("알레그로 3 (제떼)" contains "제떼" too).
        "allegro 1" to allegro1Chips, "アレグロ1" to allegro1Chips, "알레그로 1" to allegro1Chips,
        "allegro 2" to allegro2Chips, "アレグロ2" to allegro2Chips, "알레그로 2" to allegro2Chips,
        "allegro 3" to allegro3Chips, "アレグロ3" to allegro3Chips, "알레그로 3" to allegro3Chips,

        // Medium / Grand allegro
        "medium allegro" to mediumAllegroChips, "ミディアム・アレグロ" to mediumAllegroChips, "미디엄 알레그로" to mediumAllegroChips,
        "grand allegro" to grandAllegroChips, "グラン・アレグロ" to grandAllegroChips, "그랑 알레그로" to grandAllegroChips,

        // Waltz
        "waltz" to waltzChips, "ワルツ" to waltzChips, "왈츠" to waltzChips,

        // Ronds de jambe à terre / en l'air
        "à terre" to àTerreChips, "ア・テール" to àTerreChips, "아 떼르" to àTerreChips,
        "en l'air" to enLAirChips, "アン・レール" to enLAirChips, "앙레르" to enLAirChips,

        // Grand battements — must come before plain "battement" or "grand"
        "grand battement" to grandBattementChips, "グラン・バットマン" to grandBattementChips, "그랑 바뜨망" to grandBattementChips,

        // Développés
        "développé" to développéChips, "developpe" to développéChips, "デヴェロッペ" to développéChips, "데벨로뻬" to développéChips,

        // Frappés
        "frappé" to frappéChips, "frappe" to frappéChips, "フラッペ" to frappéChips, "프라뻬" to frappéChips,

        // Fondus
        "fondu" to fonduChips, "フォンデュ" to fonduChips, "퐁듀" to fonduChips,

        // Jetés — must come after "allegro 3" (whose ko form contains "제떼")
        "jeté" to jetéChips, "ジュテ" to jetéChips, "제떼" to jetéChips,

        // Tendus (also matches "Slow tendus" / "슬로우 탄듀" / "スロー・タンデュ")
        "tendu" to tenduChips, "タンデュ" to tenduChips, "탄듀" to tenduChips,

        // Pliés
        "plié" to pliéChips, "plie" to pliéChips, "プリエ" to pliéChips, "플리에" to pliéChips
    )

    fun featuredEnglish(forStepName: String): List<String>? {
        val normalized = forStepName.lowercase()
        return featuredByKeyword.firstOrNull { (keyword, _) ->
            normalized.contains(keyword.lowercase())
        }?.second
    }
}

// ──────────────────────────────────────────────────────────────────────────
// Persisted usage stats + reactive store
// ──────────────────────────────────────────────────────────────────────────

data class TermStats(
    val useCount: Int = 0,
    val lastUsed: Long? = null
)

/**
 * Reactive singleton holding term usage counts plus the editor's current
 * step-name and word-fragment context. Compose code observes the flows to
 * rebuild the chip list. Persistence is a single SharedPreferences key
 * (`balletTermUsage.v1`) — same key shape iOS uses.
 */
object TermStore {

    private const val PREFS = "balletlog_term_usage"
    private const val KEY_STATS = "balletTermUsage.v1"

    private val gson = Gson()

    private val _stats = MutableStateFlow<Map<String, TermStats>>(emptyMap())
    val stats: StateFlow<Map<String, TermStats>> = _stats.asStateFlow()

    /** Step-name of the focused combo field. "" = no context (global sort). */
    private val _currentStepName = MutableStateFlow("")
    val currentStepName: StateFlow<String> = _currentStepName.asStateFlow()

    /**
     * Word fragment immediately before the caret in the focused editor.
     * Non-empty → chip list filters to terms whose english (diacritic-
     * insensitive) or korean form begins with the fragment.
     */
    private val _currentWordFragment = MutableStateFlow("")
    val currentWordFragment: StateFlow<String> = _currentWordFragment.asStateFlow()

    private var loaded = false

    fun loadIfNeeded(context: Context) {
        if (loaded) return
        loaded = true
        val json = context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_STATS, null) ?: return
        runCatching {
            val type = object : TypeToken<Map<String, TermStats>>() {}.type
            val decoded: Map<String, TermStats> = gson.fromJson(json, type)
            _stats.value = decoded
        }
    }

    private fun persist(context: Context) {
        val json = gson.toJson(_stats.value)
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_STATS, json)
            .apply()
    }

    fun recordUsage(context: Context, term: BalletTerm) {
        val updated = _stats.value.toMutableMap()
        val current = updated[term.english] ?: TermStats()
        updated[term.english] = current.copy(
            useCount = current.useCount + 1,
            lastUsed = System.currentTimeMillis()
        )
        _stats.value = updated
        persist(context)
    }

    fun setStepName(name: String) {
        _currentStepName.value = name
    }

    fun setWordFragment(fragment: String) {
        _currentWordFragment.value = fragment
    }

    fun clearContext() {
        _currentStepName.value = ""
        _currentWordFragment.value = ""
    }

    /**
     * Featured terms at the front (priority order), then everything else by
     * useCount desc → default array order. Filtered by `currentWordFragment`
     * when non-empty.
     */
    fun computeOrderedTerms(
        statsSnapshot: Map<String, TermStats> = _stats.value,
        stepName: String = _currentStepName.value,
        fragment: String = _currentWordFragment.value
    ): List<BalletTerm> {
        val featuredKeys = BalletTerms.featuredEnglish(stepName) ?: emptyList()
        val featuredSet = featuredKeys.toSet()
        val featuredList = featuredKeys.mapNotNull { key ->
            BalletTerms.defaults.firstOrNull { it.english == key }
        }
        val rest = BalletTerms.defaults
            .filter { it.english !in featuredSet }
            .sortedWith(
                compareByDescending<BalletTerm> { statsSnapshot[it.english]?.useCount ?: 0 }
                    .thenBy { BalletTerms.defaultOrder(it) }
            )
        val combined = featuredList + rest

        if (fragment.isEmpty()) return combined

        val needleEn = removeDiacritics(fragment).lowercase()
        return combined.filter { term ->
            val en = removeDiacritics(term.english).lowercase()
            if (en.startsWith(needleEn)) return@filter true
            if (term.korean.startsWith(fragment)) return@filter true
            false
        }
    }

    private fun removeDiacritics(text: String): String {
        val normalized = Normalizer.normalize(text, Normalizer.Form.NFD)
        return normalized.replace(Regex("\\p{Mn}+"), "")
    }
}
