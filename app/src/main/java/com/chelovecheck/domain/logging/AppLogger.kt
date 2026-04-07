package com.chelovecheck.domain.logging

interface AppLogger {
    fun debug(tag: String, message: String, throwable: Throwable? = null)
    fun error(tag: String, message: String, throwable: Throwable? = null)
}
