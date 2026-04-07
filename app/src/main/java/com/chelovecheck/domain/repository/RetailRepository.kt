package com.chelovecheck.domain.repository

interface RetailRepository {
    suspend fun getNetworkName(bin: String): String?
}
