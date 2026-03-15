package com.hyorita.balletlog.data.model

data class PhotoItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val fileName: String = ""
)
