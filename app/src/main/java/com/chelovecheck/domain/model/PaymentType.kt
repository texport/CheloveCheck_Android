package com.chelovecheck.domain.model

enum class PaymentType(val id: Int) {
    CASH(0),
    CARD(1),
    MOBILE(4);

    val paymentCode: String
        get() = when (this) {
            CASH -> "PAYMENT_CASH"
            CARD -> "PAYMENT_CARD"
            MOBILE -> "PAYMENT_MOBILE"
        }

    companion object {
        fun fromId(id: Int): PaymentType? = entries.firstOrNull { it.id == id }
    }
}
