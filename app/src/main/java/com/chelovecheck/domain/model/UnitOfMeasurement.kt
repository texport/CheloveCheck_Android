package com.chelovecheck.domain.model

@Suppress("SpellCheckingInspection")
enum class UnitOfMeasurement(val code: String, val info: Info) {
    PIECE("796", Info("Штука", "Дана", "шт", "дана")),
    KILOGRAM("116", Info("Килограмм", "Килограмм", "кг", "кг")),
    SERVICE("5114", Info("Услуга", "Қызмет", "усл", "қзм")),
    METER("006", Info("Метр", "Метр", "м", "м")),
    LITER("112", Info("Литр", "Литр", "л", "л")),
    LINEAR_METER("021", Info("Погонный метр", "Өткел қума метр", "пог.м", "өқм")),
    TON("168", Info("Тонна", "Тонна", "т", "т")),
    HOUR("356", Info("Час", "Сағат", "ч", "сағ")),
    DAY("359", Info("Сутки", "Тәулік", "с", "тлк")),
    WEEK("360", Info("Неделя", "Апта", "нед", "апт")),
    MONTH("362", Info("Месяц", "Ай", "мес", "ай")),
    MILLIMETER("003", Info("Миллиметр", "Миллиметр", "мм", "мм")),
    CENTIMETER("004", Info("Сантиметр", "Сантиметр", "см", "см")),
    DECIMETER("005", Info("Дециметр", "Дециметр", "дм", "дм")),
    UNIT("642", Info("Единица", "Бірлік", "ед", "брл")),
    KILOMETER("008", Info("Километр", "Километр", "км", "км")),
    HECTOGRAM("160", Info("Гектограмм", "Гектограмм", "гг", "гг")),
    MILLIGRAM("161", Info("Миллиграмм", "Миллиграмм", "мг", "мг")),
    METRIC_CARAT("162", Info("Метрический карат", "Метрлік карат", "мкар", "мкар")),
    GRAM("163", Info("Грамм", "Грамм", "гр", "гр")),
    MICROGRAM("164", Info("Микрограмм", "Микрограмм", "мкг", "мкг")),
    CUBIC_MILLIMETER("110", Info("Кубический миллиметр", "Куб миллиметр", "мм3", "мм3")),
    MILLILITER("111", Info("Миллилитр", "Миллилитр", "мл", "мл")),
    SQUARE_METER("055", Info("Квадратный метр", "Шаршы метр", "м2", "м2")),
    HECTARE("059", Info("Гектар", "Гектар", "га", "га")),
    SQUARE_KILOMETER("061", Info("Квадратный километр", "Шаршы километр", "км2", "км2")),
    SHEET("625", Info("Лист", "Парақ", "лист", "прқ")),
    PACK("728", Info("Пачка", "Бума", "пач", "бм")),
    ROLL("736", Info("Рулон", "Орам", "рул", "орам")),
    PACKAGE("778", Info("Упаковка", "Орама", "упак", "орм")),
    BOTTLE("868", Info("Бутылка", "Бөтелке", "бут", "бөт")),
    WORK("931", Info("Работа", "Жұмыс", "раб", "жұм")),
    CUBIC_METER("113", Info("Метр кубический", "Куб метр", "м3", "м3")),
    UNKNOWN("000", Info("Неизвестно", "Белгісіз", "?", "?"));

    data class Info(
        val nameRus: String,
        val nameKaz: String,
        val shortRus: String,
        val shortKaz: String,
    )

    companion object {
        fun from(value: String?): UnitOfMeasurement {
            if (value == null) return UNKNOWN
            val normalized = value.lowercase().trim()
            entries.firstOrNull { it.code == normalized }?.let { return it }

            return entries.firstOrNull { unit ->
                val info = unit.info
                normalized == info.shortRus.lowercase() ||
                    normalized == info.shortKaz.lowercase() ||
                    normalized == info.nameRus.lowercase() ||
                    normalized == info.nameKaz.lowercase()
            } ?: UNKNOWN
        }
    }
}
