package com.hyorita.balletlog.data

import com.hyorita.balletlog.data.model.Step

object DefaultSteps {

    val barre: List<Step> = listOf(
        Step(name = "Plié"),
        Step(name = "Slow tendu"),
        Step(name = "Tendu"),
        Step(name = "Tendu jeté"),
        Step(name = "Ronds de jambe à terre"),
        Step(name = "Fondu"),
        Step(name = "Ronds de jambe en l'air"),
        Step(name = "Frappé"),
        Step(name = "Développé"),
        Step(name = "Grand battements")
    )

    val center: List<Step> = listOf(
        Step(name = "Adagio"),
        Step(name = "Tendu"),
        Step(name = "Fondu"),
        Step(name = "Grand battements"),
        Step(name = "Waltz"),
        Step(name = "Allegro 1 (Sautés)"),
        Step(name = "Allegro 2 (Assemblé)"),
        Step(name = "Allegro 3 (Jetés)"),
        Step(name = "Medium allegro"),
        Step(name = "Grand allegro")
    )

    // Legacy: combined list (kept for compatibility)
    val list: List<Step> get() = barre
}
