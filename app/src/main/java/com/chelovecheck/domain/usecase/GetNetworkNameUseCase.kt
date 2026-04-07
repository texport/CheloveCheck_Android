package com.chelovecheck.domain.usecase

import com.chelovecheck.domain.repository.RetailRepository
import javax.inject.Inject

class GetNetworkNameUseCase @Inject constructor(
    private val repository: RetailRepository,
) {
    suspend operator fun invoke(bin: String): String? = repository.getNetworkName(bin)
}
