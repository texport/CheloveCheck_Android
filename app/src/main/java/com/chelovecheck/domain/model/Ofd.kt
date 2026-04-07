package com.chelovecheck.domain.model

enum class Ofd(val id: String) {
    KAZAKHTELECOM("1"),
    TRANSTELECOM("2"),
    KOFD("3"),
    WOFD("4"),
    KASPI("5");

    companion object {
        fun fromId(id: String): Ofd? = entries.firstOrNull { it.id == id }
    }
}
