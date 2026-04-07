package com.chelovecheck.domain.repository

import com.chelovecheck.domain.model.RetailCategoryHint

interface RetailCategoryHintRepository {
    fun getHint(networkName: String?): RetailCategoryHint?
}
