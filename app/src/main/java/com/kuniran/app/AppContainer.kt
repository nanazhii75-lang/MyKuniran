package com.kuniran.app

import android.content.Context
import com.kuniran.core.database.AppDatabase
import com.kuniran.core.network.SessionManager
import com.kuniran.core.network.SupabaseApiService
import com.kuniran.core.network.SupabaseClient
import com.kuniran.core.network.SupabaseConfig
import com.kuniran.data.repository.AuthRepositoryImpl
import com.kuniran.data.repository.FinanceRecordRepositoryImpl
import com.kuniran.data.repository.FinanceRepositoryImpl
import com.kuniran.data.repository.MemberRepositoryImpl
import com.kuniran.data.repository.PostRepositoryImpl
import com.kuniran.data.repository.RtRepositoryImpl
import com.kuniran.data.repository.WargaRepositoryImpl
import com.kuniran.domain.repository.AuthRepository
import com.kuniran.domain.repository.FinanceRecordRepository
import com.kuniran.domain.repository.FinanceRepository
import com.kuniran.domain.repository.MemberRepository
import com.kuniran.domain.repository.PostRepository
import com.kuniran.domain.repository.RtRepository
import com.kuniran.domain.repository.WargaRepository
import com.kuniran.domain.usecase.*

class AppContainer(context: Context) {

    val sessionManager: SessionManager by lazy {
        SessionManager(context)
    }

    val database: AppDatabase by lazy {
        AppDatabase.getInstance(context)
    }

    val supabaseClient: SupabaseClient by lazy {
        SupabaseClient.getInstance(sessionManager)
    }

    val apiService: SupabaseApiService
        get() = supabaseClient.apiService

    // Repositories
    val authRepository: AuthRepository by lazy {
        AuthRepositoryImpl(apiService, database.profileDao(), sessionManager, database)
    }

    val rtRepository: RtRepository by lazy {
        RtRepositoryImpl(apiService, database.rtGroupDao(), database.profileDao(), sessionManager, database)
    }

    val postRepository: PostRepository by lazy {
        PostRepositoryImpl(apiService, database.postDao())
    }

    val financeRepository: FinanceRepository by lazy {
        FinanceRepositoryImpl(apiService, database.financeDao(), database.categoryDao())
    }

    val memberRepository: MemberRepository by lazy {
        MemberRepositoryImpl(apiService, database.memberDao())
    }

    val wargaRepository: WargaRepository by lazy {
        WargaRepositoryImpl(supabaseClient)
    }

    val financeRecordRepository: FinanceRecordRepository by lazy {
        FinanceRecordRepositoryImpl(supabaseClient)
    }

    // Use Cases
    val getCurrentUserUseCase by lazy { GetCurrentUserUseCase(authRepository) }
    val signInWithGoogleUseCase by lazy { SignInWithGoogleUseCase(authRepository) }
    val signOutUseCase by lazy { SignOutUseCase(authRepository) }
    val updateProfileUseCase by lazy { UpdateProfileUseCase(authRepository) }

    val createRtUseCase by lazy { CreateRtUseCase(rtRepository) }
    val previewRtUseCase by lazy { PreviewRtUseCase(rtRepository) }
    val requestJoinRtUseCase by lazy { RequestJoinRtUseCase(rtRepository) }
    val cancelJoinRequestUseCase by lazy { CancelJoinRequestUseCase(rtRepository) }
    val listPendingJoinRequestsUseCase by lazy { ListPendingJoinRequestsUseCase(rtRepository) }
    val approveJoinRequestUseCase by lazy { ApproveJoinRequestUseCase(rtRepository) }
    val rejectJoinRequestUseCase by lazy { RejectJoinRequestUseCase(rtRepository) }
    val setAutoApproveUseCase by lazy { SetAutoApproveUseCase(rtRepository) }
    val setInviteUsernameUseCase by lazy { SetInviteUsernameUseCase(rtRepository) }
    val checkUsernameAvailableUseCase by lazy { CheckUsernameAvailableUseCase(rtRepository) }
    val updateRtInfoUseCase by lazy { UpdateRtInfoUseCase(rtRepository) }
    val leaveRtUseCase by lazy { LeaveRtUseCase(rtRepository) }

    val getRtFeedUseCase by lazy { GetRtFeedUseCase(postRepository) }
    val createPostUseCase by lazy { CreatePostUseCase(postRepository) }
    val pinPostUseCase by lazy { PinPostUseCase(postRepository) }
    val unpinPostUseCase by lazy { UnpinPostUseCase(postRepository) }
    val deletePostUseCase by lazy { DeletePostUseCase(postRepository) }
    val syncFeedUseCase by lazy { SyncFeedUseCase(postRepository) }

    val getFinanceCategoriesUseCase by lazy { GetFinanceCategoriesUseCase(financeRepository) }
    val getTransactionsUseCase by lazy { GetTransactionsUseCase(financeRepository) }
    val getAllRtTransactionsUseCase by lazy { GetAllRtTransactionsUseCase(financeRepository) }
    val createTransactionUseCase by lazy { CreateTransactionUseCase(financeRepository) }
    val deleteTransactionUseCase by lazy { DeleteTransactionUseCase(financeRepository) }
    val publishFinanceReportUseCase by lazy { PublishFinanceReportUseCase(financeRepository) }
    val publishMonthlyRecapUseCase by lazy { PublishMonthlyRecapUseCase(financeRepository) }
    val createFinanceAgendaUseCase by lazy { CreateFinanceAgendaUseCase(financeRepository) }
    val syncFinancesUseCase by lazy { SyncFinancesUseCase(financeRepository) }
    val getFinanceRecordsUseCase by lazy { GetFinanceRecordsUseCase(financeRecordRepository) }
    val createFinanceCategoryUseCase by lazy { CreateFinanceCategoryUseCase(financeRepository) }
    val updateFinanceCategoryUseCase by lazy { UpdateFinanceCategoryUseCase(financeRepository) }

    val getRtMembersUseCase by lazy { GetRtMembersUseCase(memberRepository) }
    val syncMembersUseCase by lazy { SyncMembersUseCase(memberRepository) }
    val transferAdminUseCase by lazy { TransferAdminUseCase(memberRepository) }
    val assignBendaharaUseCase by lazy { AssignBendaharaUseCase(memberRepository) }
    val removeMemberUseCase by lazy { RemoveMemberUseCase(memberRepository) }
    val getWargaListUseCase by lazy { GetWargaListUseCase(wargaRepository) }
    val logAttendanceUseCase by lazy { LogAttendanceUseCase(wargaRepository) }
    val getAttendanceHistoryUseCase by lazy { GetAttendanceHistoryUseCase(wargaRepository) }
    val submitRsvpUseCase by lazy { SubmitRsvpUseCase(postRepository) }
    val getAgendaRsvpsUseCase by lazy { GetAgendaRsvpsUseCase(postRepository) }
}
