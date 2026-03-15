package com.hyorita.balletlog.data.model

data class Step(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String = "",
    val count: Int = 0,
    val note: String = ""
)
