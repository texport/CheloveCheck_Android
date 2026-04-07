package com.chelovecheck.data.analytics

import kotlinx.serialization.Serializable

@Serializable
internal data class TokenizerConfigDto(
    val do_lower_case: Boolean? = null,
    val cls_token: String? = null,
    val sep_token: String? = null,
    val pad_token: String? = null,
    val unk_token: String? = null,
    val model_max_length: Int? = null,
) {
    val doLowerCase: Boolean? get() = do_lower_case
    val clsToken: String? get() = cls_token
    val sepToken: String? get() = sep_token
    val padToken: String? get() = pad_token
    val unkToken: String? get() = unk_token
    val modelMaxLength: Int? get() = model_max_length
}
