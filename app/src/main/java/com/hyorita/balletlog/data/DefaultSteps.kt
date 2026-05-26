package com.hyorita.balletlog.data

import com.hyorita.balletlog.data.model.Step

object DefaultSteps {

    // 1.8: pluralized to match how class syllabi typically name exercises.
    // Mirrors iOS DefaultSteps.swift. Existing user logs retain whatever
    // names they were created with; only new editor defaults change.
    val barre: List<Step> = listOf(
        Step(name = "Pliés"),
        Step(name = "Slow tendus"),
        Step(name = "Tendus"),
        Step(name = "Jetés"),
        Step(name = "Ronds de jambe à terre"),
        Step(name = "Fondus"),
        Step(name = "Ronds de jambe en l'air"),
        Step(name = "Frappés"),
        Step(name = "Développés"),
        Step(name = "Grand battements")
    )

    val center: List<Step> = listOf(
        Step(name = "Adagio"),
        Step(name = "Tendus"),
        Step(name = "Fondus"),
        Step(name = "Grand battements"),
        Step(name = "Waltz"),
        Step(name = "Allegro 1 (Sautés)"),
        Step(name = "Allegro 2 (Assemblés)"),
        Step(name = "Allegro 3 (Jetés)"),
        Step(name = "Medium allegro"),
        Step(name = "Grand allegro")
    )

    // Legacy: combined list (kept for compatibility)
    val list: List<Step> get() = barre
}
