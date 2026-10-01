package com.kuniran.feature.help

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SupervisorAccount
import com.kuniran.R

object HelpFaqDataProvider {

    fun getSections(): List<FaqSection> = listOf(
        FaqSection(
            id = "section_join",
            titleRes = R.string.faq_section_join_title,
            descRes = R.string.faq_section_join_desc,
            icon = Icons.Default.GroupAdd,
            items = listOf(
                FaqItem(
                    id = "join_q1",
                    questionRes = R.string.faq_join_q1,
                    answerRes = R.string.faq_join_a1
                ),
                FaqItem(
                    id = "join_q2",
                    questionRes = R.string.faq_join_q2,
                    answerRes = R.string.faq_join_a2
                ),
                FaqItem(
                    id = "join_q3",
                    questionRes = R.string.faq_join_q3,
                    answerRes = R.string.faq_join_a3
                )
            )
        ),
        FaqSection(
            id = "section_admin",
            titleRes = R.string.faq_section_admin_title,
            descRes = R.string.faq_section_admin_desc,
            icon = Icons.Default.SupervisorAccount,
            items = listOf(
                FaqItem(
                    id = "admin_q1",
                    questionRes = R.string.faq_admin_q1,
                    answerRes = R.string.faq_admin_a1
                ),
                FaqItem(
                    id = "admin_q2",
                    questionRes = R.string.faq_admin_q2,
                    answerRes = R.string.faq_admin_a2
                ),
                FaqItem(
                    id = "admin_q3",
                    questionRes = R.string.faq_admin_q3,
                    answerRes = R.string.faq_admin_a3
                )
            )
        ),
        FaqSection(
            id = "section_finance",
            titleRes = R.string.faq_section_finance_title,
            descRes = R.string.faq_section_finance_desc,
            icon = Icons.Default.AccountBalance,
            items = listOf(
                FaqItem(
                    id = "finance_q1",
                    questionRes = R.string.faq_finance_q1,
                    answerRes = R.string.faq_finance_a1
                ),
                FaqItem(
                    id = "finance_q2",
                    questionRes = R.string.faq_finance_q2,
                    answerRes = R.string.faq_finance_a2
                ),
                FaqItem(
                    id = "finance_q3",
                    questionRes = R.string.faq_finance_q3,
                    answerRes = R.string.faq_finance_a3
                )
            )
        ),
        FaqSection(
            id = "section_privacy",
            titleRes = R.string.faq_section_privacy_title,
            descRes = R.string.faq_section_privacy_desc,
            icon = Icons.Default.Security,
            items = listOf(
                FaqItem(
                    id = "privacy_q1",
                    questionRes = R.string.faq_privacy_q1,
                    answerRes = R.string.faq_privacy_a1
                )
            )
        )
    )
}
