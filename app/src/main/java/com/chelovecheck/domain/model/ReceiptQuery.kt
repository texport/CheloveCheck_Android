package com.chelovecheck.domain.model

data class ReceiptQuery(
    val ofd: Ofd,
    val t: String? = null,
    val i: String? = null,
    val f: String? = null,
    val s: String? = null,
    val url: String? = null,
)
