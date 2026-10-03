package com.kuniran.feature.finance

import com.kuniran.core.common.asText

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddBusiness
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kuniran.R
import com.kuniran.core.common.CurrencyFormatter
import com.kuniran.core.common.DateTimeUtils
import com.kuniran.core.model.FinanceCategory
import com.kuniran.core.model.FinanceTransaction
import com.kuniran.core.model.TransactionType
import com.kuniran.core.model.UserRole
import com.kuniran.core.ui.components.KuniranEmptyState
import com.kuniran.core.ui.components.KuniranTopAppBar

@Composable
fun FinanceScreen(
    viewModel: FinanceViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var showCreateTxDialog by remember { mutableStateOf(false) }
    var showCreateCatDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<FinanceCategory?>(null) }
    var assigningCategory by remember { mutableStateOf<FinanceCategory?>(null) }
    var showRecapDialog by remember { mutableStateOf(false) }
    var selectedTx by remember { mutableStateOf<FinanceTransaction?>(null) }
    var overflowMenuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { err ->
            snackbarHostState.showSnackbar(err.asText(context))
            viewModel.clearError()
        }
    }

    // Intercept system back when viewing a specific Pos Ledger
    BackHandler(enabled = uiState.activeLedgerCategory != null) {
        viewModel.closeCategoryLedger()
    }

    val isAdmin = uiState.currentUser?.role == UserRole.ADMIN_RT
    val activeCat = uiState.activeLedgerCategory

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            if (activeCat == null) {
                // Main Finance Overview TopBar
                KuniranTopAppBar(
                    title = stringResource(R.string.finance_title),
                    actions = {
                        IconButton(
                            onClick = { viewModel.refreshFinances() },
                            modifier = Modifier.testTag("btn_refresh_finances")
                        ) {
                            if (uiState.isRefreshing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.padding(8.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = stringResource(R.string.btn_retry)
                                )
                            }
                        }

                        if (uiState.canManageAnyPos) {
                            Box {
                                IconButton(onClick = { overflowMenuExpanded = true }) {
                                    Icon(imageVector = Icons.Default.MoreVert, contentDescription = null)
                                }
                                DropdownMenu(
                                    expanded = overflowMenuExpanded,
                                    onDismissRequest = { overflowMenuExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.finance_btn_add_pos)) },
                                        leadingIcon = { Icon(Icons.Default.AddBusiness, contentDescription = null) },
                                        onClick = {
                                            overflowMenuExpanded = false
                                            showCreateCatDialog = true
                                        }
                                    )
                                }
                            }
                        }
                    }
                )
            } else {
                // Pos Ledger TopBar with Back Button
                KuniranTopAppBar(
                    title = stringResource(R.string.finance_ledger_title, activeCat.name),
                    navigationIcon = {
                        IconButton(
                            onClick = { viewModel.closeCategoryLedger() },
                            modifier = Modifier.testTag("btn_back_to_pos_list")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.btn_back)
                            )
                        }
                    },
                    actions = {
                        if (uiState.isAdmin) {
                            IconButton(
                                onClick = { assigningCategory = activeCat },
                                modifier = Modifier.testTag("btn_assign_bendahara")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PersonAdd,
                                    contentDescription = stringResource(R.string.finance_btn_assign_bendahara)
                                )
                            }
                        }
                        if (uiState.canManagePos(activeCat)) {
                            IconButton(
                                onClick = { editingCategory = activeCat },
                                modifier = Modifier.testTag("btn_edit_active_pos")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = stringResource(R.string.finance_btn_edit_pos)
                                )
                            }
                        }
                        IconButton(
                            onClick = { viewModel.refreshFinances() },
                            modifier = Modifier.testTag("btn_refresh_ledger")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = stringResource(R.string.btn_retry)
                            )
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            if (activeCat == null) {
                // FAB to add Pos Kas (only if Bendahara or Admin)
                if (uiState.canManageAnyPos) {
                    FloatingActionButton(
                        onClick = { showCreateCatDialog = true },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.testTag("btn_add_pos_fab")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddBusiness,
                            contentDescription = stringResource(R.string.finance_btn_add_pos)
                        )
                    }
                }
            } else {
                // FAB to record transaction in this Pos (only if Bendahara of this pos or Admin)
                if (uiState.isBendaharaOfSelected) {
                    FloatingActionButton(
                        onClick = { showCreateTxDialog = true },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.testTag("btn_add_transaction_fab")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(R.string.finance_btn_add_transaction)
                        )
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (activeCat == null) {
                // 1. OVERVIEW DAFTAR POS KAS & ARISAN
                FinancePosOverview(
                    uiState = uiState,
                    onSelectPos = { cat -> viewModel.openCategoryLedger(cat) },
                    onEditPos = { cat -> editingCategory = cat },
                    onAddPos = { showCreateCatDialog = true }
                )
            } else {
                // 2. DETAIL PEMBUKUAN LENGKAP POS (LEDGER VIEW)
                FinancePosLedgerView(
                    category = activeCat,
                    uiState = uiState,
                    onSearchQueryChange = { viewModel.setSearchQuery(it) },
                    onFilterTypeChange = { viewModel.setFilterType(it) },
                    onTransactionClick = { selectedTx = it },
                    onPublishRecap = { showRecapDialog = true }
                )
            }
        }

        // Dialogs
        if (showCreateTxDialog) {
            CreateTransactionDialog(
                onDismiss = { showCreateTxDialog = false },
                onSubmit = { title, amt, type, contributor, note, date ->
                    showCreateTxDialog = false
                    viewModel.createTransaction(title, amt, type, contributor, note, date) {
                        viewModel.refreshFinances()
                    }
                }
            )
        }

        if (showCreateCatDialog) {
            CategoryManagementDialog(
                onDismiss = { showCreateCatDialog = false },
                onCreateCategory = { name, desc ->
                    showCreateCatDialog = false
                    viewModel.createCategory(name, desc) {
                        viewModel.refreshFinances()
                    }
                }
            )
        }

        editingCategory?.let { cat ->
            EditCategoryDialog(
                category = cat,
                onDismiss = { editingCategory = null },
                onUpdateCategory = { newName, newDesc ->
                    editingCategory = null
                    viewModel.updateCategory(cat.id, newName, newDesc) {
                        viewModel.refreshFinances()
                    }
                }
            )
        }

        assigningCategory?.let { cat ->
            BendaharaPickerDialog(
                category = cat,
                members = uiState.members,
                onDismiss = { assigningCategory = null },
                onAssign = { profileId ->
                    assigningCategory = null
                    viewModel.assignBendahara(cat.id, profileId) {
                        viewModel.refreshFinances()
                    }
                }
            )
        }

        if (showRecapDialog && activeCat != null) {
            PublishRecapDialog(
                categoryName = activeCat.name,
                onDismiss = { showRecapDialog = false },
                onPublish = { monthDate ->
                    showRecapDialog = false
                    viewModel.publishMonthlyRecap(monthDate) {
                        viewModel.refreshFinances()
                    }
                }
            )
        }

        selectedTx?.let { tx ->
            val canManageTx = uiState.isBendaharaOfSelected
            TransactionDetailDialog(
                transaction = tx,
                isBendahara = canManageTx,
                onPublish = {
                    viewModel.publishTransaction(tx.id, null) {
                        selectedTx = null
                        viewModel.refreshFinances()
                    }
                },
                onDelete = {
                    viewModel.deleteTransaction(tx.id)
                    selectedTx = null
                },
                onDismiss = { selectedTx = null }
            )
        }
    }
}

/**
 * Tampilan 1: Overview Pos Keuangan (Daftar Kartu Pos: Arisan, Arisan Pemuda, Kas RT, dll)
 */
@Composable
private fun FinancePosOverview(
    uiState: FinanceUiState,
    onSelectPos: (FinanceCategory) -> Unit,
    onEditPos: (FinanceCategory) -> Unit,
    onAddPos: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("pos_overview_list"),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Overall Balance Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("balance_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = stringResource(R.string.finance_total_balance),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = CurrencyFormatter.formatRupiah(uiState.balance),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowDownward,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.finance_type_in),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                                )
                                Text(
                                    text = CurrencyFormatter.formatRupiah(uiState.totalIncome),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowUpward,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.finance_type_out),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                                )
                                Text(
                                    text = CurrencyFormatter.formatRupiah(uiState.totalExpense),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section Title & Add Pos Button
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.finance_header_pos_list),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.finance_pos_list_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // List of Pos Cards
        if (uiState.categories.isEmpty()) {
            item {
                KuniranEmptyState(
                    icon = Icons.Default.Savings,
                    message = "Belum ada pos kas. Pengurus dapat menambahkan pos kas atau arisan."
                )
            }
        } else {
            items(uiState.categories, key = { it.id }) { cat ->
                val posBalance = uiState.getCategoryBalance(cat.id)
                val posIncome = uiState.getCategoryIncome(cat.id)
                val posExpense = uiState.getCategoryExpense(cat.id)
                val isUserBendaharaOfPos = uiState.canManagePos(cat)

                PosCategoryCard(
                    category = cat,
                    balance = posBalance,
                    income = posIncome,
                    expense = posExpense,
                    canEdit = isUserBendaharaOfPos,
                    onClick = { onSelectPos(cat) },
                    onEditClick = { onEditPos(cat) }
                )
            }
        }
    }
}

/**
 * Kartu Pos Keuangan individual (e.g. Arisan, Arisan Pemuda, Kas RT)
 */
@Composable
private fun PosCategoryCard(
    category: FinanceCategory,
    balance: Long,
    income: Long,
    expense: Long,
    canEdit: Boolean,
    onClick: () -> Unit,
    onEditClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("pos_card_${category.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Savings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = category.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        category.description?.let { desc ->
                            if (desc.isNotBlank()) {
                                Text(
                                    text = desc,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (canEdit) {
                        IconButton(
                            onClick = onEditClick,
                            modifier = Modifier.testTag("btn_edit_pos_${category.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = stringResource(R.string.finance_btn_edit_pos),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Balance Details Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.finance_ledger_saldo),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = CurrencyFormatter.formatRupiah(balance),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (balance >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = stringResource(R.string.finance_type_in),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = CurrencyFormatter.formatRupiah(income),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = stringResource(R.string.finance_type_out),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = CurrencyFormatter.formatRupiah(expense),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

/**
 * Tampilan 2: Detail Pembukuan Lengkap Pos (Ledger View)
 * Menampilkan: Tanggal, Uang Masuk, Uang Keluar, Asal Dana, Untuk Apa, dan Nominal secara transparan.
 */
@Composable
private fun FinancePosLedgerView(
    category: FinanceCategory,
    uiState: FinanceUiState,
    onSearchQueryChange: (String) -> Unit,
    onFilterTypeChange: (String) -> Unit,
    onTransactionClick: (FinanceTransaction) -> Unit,
    onPublishRecap: () -> Unit
) {
    val filteredTransactions = remember(uiState.transactions, uiState.searchQuery, uiState.filterType) {
        uiState.transactions.filter { tx ->
            val matchesType = when (uiState.filterType) {
                "MASUK" -> tx.type == TransactionType.MASUK
                "KELUAR" -> tx.type == TransactionType.KELUAR
                else -> true
            }
            val q = uiState.searchQuery.trim().lowercase()
            val matchesSearch = q.isEmpty() ||
                    tx.title.lowercase().contains(q) ||
                    (tx.contributorName?.lowercase()?.contains(q) == true) ||
                    (tx.note?.lowercase()?.contains(q) == true)

            matchesType && matchesSearch
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("ledger_transactions_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Pos Summary Header Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = category.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    category.description?.let { desc ->
                        if (desc.isNotBlank()) {
                            Text(
                                text = desc,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.finance_ledger_saldo),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                            )
                            Text(
                                text = CurrencyFormatter.formatRupiah(uiState.ledgerBalance),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }

                        if (uiState.isBendaharaOfSelected) {
                            Button(
                                onClick = onPublishRecap,
                                modifier = Modifier.testTag("btn_publish_recap_inline")
                            ) {
                                Text(stringResource(R.string.finance_tab_recap), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${stringResource(R.string.finance_type_in)}: ${CurrencyFormatter.formatRupiah(uiState.ledgerIncome)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${stringResource(R.string.finance_type_out)}: ${CurrencyFormatter.formatRupiah(uiState.ledgerExpense)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        // Search Bar & Filter Chips
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = onSearchQueryChange,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    placeholder = { Text(stringResource(R.string.finance_ledger_search_hint)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_search_ledger"),
                    shape = RoundedCornerShape(12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = uiState.filterType == "ALL",
                        onClick = { onFilterTypeChange("ALL") },
                        label = { Text("Semua") }
                    )
                    FilterChip(
                        selected = uiState.filterType == "MASUK",
                        onClick = { onFilterTypeChange("MASUK") },
                        label = { Text(stringResource(R.string.finance_type_in)) }
                    )
                    FilterChip(
                        selected = uiState.filterType == "KELUAR",
                        onClick = { onFilterTypeChange("KELUAR") },
                        label = { Text(stringResource(R.string.finance_type_out)) }
                    )
                }
            }
        }

        // Transaction Ledger Entries
        if (filteredTransactions.isEmpty()) {
            item {
                KuniranEmptyState(
                    icon = Icons.Default.ReceiptLong,
                    message = stringResource(R.string.finance_ledger_empty)
                )
            }
        } else {
            items(filteredTransactions, key = { it.id }) { tx ->
                LedgerTransactionCard(
                    transaction = tx,
                    onClick = { onTransactionClick(tx) }
                )
            }
        }
    }
}

/**
 * Kartu Rincian Transaksi Pembukuan Lengkap (Menampilkan Asal Dana, Untuk Apa, Tanggal, Nominal)
 */
@Composable
private fun LedgerTransactionCard(
    transaction: FinanceTransaction,
    onClick: () -> Unit
) {
    val isIncome = transaction.type == TransactionType.MASUK

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("ledger_tx_item_${transaction.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header: Tanggal & Badge Uang Masuk / Keluar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isIncome) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                        contentDescription = null,
                        tint = if (isIncome) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = DateTimeUtils.formatWibDate(transaction.transactionDate),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                // Amount Badge
                Text(
                    text = (if (isIncome) "+ " else "- ") + CurrencyFormatter.formatRupiah(transaction.amount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isIncome) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }

            // Keterangan / Untuk Apa (Purpose)
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    text = "${stringResource(R.string.finance_item_purpose)}: ",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = transaction.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (transaction.isLocked) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            // Asal Dana (Contributor / Source)
            transaction.contributorName?.let { contributor ->
                if (contributor.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${stringResource(R.string.finance_item_origin)}: ",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = contributor,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Catatan Tambahan (jika ada)
            transaction.note?.let { note ->
                if (note.isNotBlank()) {
                    Text(
                        text = "${stringResource(R.string.finance_item_note)}: $note",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}
