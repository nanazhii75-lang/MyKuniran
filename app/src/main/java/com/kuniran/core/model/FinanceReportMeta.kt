package com.kuniran.core.model

data class FinanceReportMeta(
    val kind: String = "",
    val categoryName: String = "",
    val type: TransactionType? = null,
    val amount: Long? = null,
    val contributorName: String? = null,
    val transactionDate: String? = null,
    val masuk: Long? = null,
    val keluar: Long? = null,
    val saldoAwal: Long? = null,
    val saldoAkhir: Long? = null
)
