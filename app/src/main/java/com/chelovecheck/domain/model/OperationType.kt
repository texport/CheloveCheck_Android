package com.chelovecheck.domain.model

enum class OperationType(val id: Int) {
    BUY(0),
    BUY_RETURN(1),
    SELL(2),
    SELL_RETURN(3);

    val operationCode: String
        get() = when (this) {
            BUY -> "OPERATION_BUY"
            BUY_RETURN -> "OPERATION_BUY_RETURN"
            SELL -> "OPERATION_SELL"
            SELL_RETURN -> "OPERATION_SELL_RETURN"
        }

    companion object {
        fun fromId(id: Int): OperationType? = entries.firstOrNull { it.id == id }
    }
}
