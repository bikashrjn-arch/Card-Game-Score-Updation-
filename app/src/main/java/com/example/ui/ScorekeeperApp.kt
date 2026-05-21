package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScorekeeperApp(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val allGames by viewModel.allGames.collectAsStateWithLifecycle()
    val activeGame by viewModel.activeGame.collectAsStateWithLifecycle()
    val currentGame by viewModel.currentGame.collectAsStateWithLifecycle()
    val profiles by viewModel.allPlayerProfiles.collectAsStateWithLifecycle()
    val profileMap = remember(profiles) { profiles.associateBy { it.name.lowercase().trim() } }

    var showSetupDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: History, 1: Rule Helpers

    // If currently playing/viewing a game, show Game Screen; otherwise show Main Dashboard
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (currentGame != null) {
            GameDetailScreen(
                game = currentGame!!,
                viewModel = viewModel,
                onBack = { viewModel.selectGame(null) }
            )
        } else {
            Scaffold(
                topBar = {
                    LargeTopAppBar(
                        title = {
                            Column {
                                Text(
                                    "Scorekeeper",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    "Physical Cards Companion",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        actions = {
                            if (allGames.isNotEmpty()) {
                                var showDeleteAllConfirm by remember { mutableStateOf(false) }
                                IconButton(onClick = { showDeleteAllConfirm = true }) {
                                    Icon(
                                        Icons.Default.DeleteSweep,
                                        contentDescription = "Clear All History",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                                if (showDeleteAllConfirm) {
                                    AlertDialog(
                                        onDismissRequest = { showDeleteAllConfirm = false },
                                        title = { Text("Reset Application Data") },
                                        text = { Text("Are you sure you want to permanently erase all card game score sheets and history? This cannot be undone.") },
                                        confirmButton = {
                                            TextButton(
                                                onClick = {
                                                    viewModel.deleteAllHistory()
                                                    showDeleteAllConfirm = false
                                                },
                                                colors = ButtonDefaults.textButtonColors(
                                                    contentColor = MaterialTheme.colorScheme.error
                                                )
                                            ) {
                                                Text("Reset All")
                                            }
                                        },
                                        dismissButton = {
                                            TextButton(onClick = { showDeleteAllConfirm = false }) {
                                                Text("Cancel")
                                            }
                                        }
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.largeTopAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            scrolledContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                },
                floatingActionButton = {
                    ExtendedFloatingActionButton(
                        onClick = { showSetupDialog = true },
                        icon = { Icon(Icons.Default.Add, contentDescription = "New Match") },
                        text = { Text("New Scoring Sheet") },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                }
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .padding(paddingValues)
                        .fillMaxSize()
                ) {
                    // Quick Navigation (History vs Profiles vs Rules tab row)
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.Transparent,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Game Logs", fontWeight = FontWeight.SemiBold) },
                            icon = { Icon(Icons.Default.History, contentDescription = null) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("Squad Profiles", fontWeight = FontWeight.SemiBold) },
                            icon = { Icon(Icons.Default.People, contentDescription = null) }
                        )
                        Tab(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            text = { Text("Rules & Strategies", fontWeight = FontWeight.SemiBold) },
                            icon = { Icon(Icons.Default.MenuBook, contentDescription = null) }
                        )
                    }

                    if (selectedTab == 0) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        ) {
                            // ACTIVE MATCH RESUMPTION BANNER
                        activeGame?.let { active ->
                            Card(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth()
                                    .clickable { viewModel.selectGame(active.id) },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .background(
                                                    MaterialTheme.colorScheme.primary,
                                                    shape = RoundedCornerShape(12.dp)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = if (active.gameType == "SPADES") Icons.Default.Style else Icons.Default.Favorite,
                                                contentDescription = active.gameType,
                                                tint = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column {
                                            Text(
                                                "Active Match in Progress",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                            val players = MoshiHelper.fromJsonPlayers(active.playersJson)
                                            Text(
                                                "${active.gameType.lowercase().capitalize(Locale.getDefault())} • ${players.joinToString(", ")}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = { viewModel.selectGame(active.id) },
                                        colors = IconButtonDefaults.filledIconButtonColors(
                                            containerColor = MaterialTheme.colorScheme.primary
                                        )
                                    ) {
                                        Icon(
                                            Icons.Default.PlayArrow,
                                            contentDescription = "Resume",
                                            tint = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }
                                }
                            }
                        }

                        // LOBBY / ARCHIVED GAMES HISTORY
                        Text(
                            text = "Scoring Sheets Archive",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (allGames.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 64.dp, horizontal = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        Icons.Default.Notes,
                                        contentDescription = "No data",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.size(72.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        "No games recorded yet",
                                        fontWeight = FontWeight.SemiBold,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        "Start a new scoring session when playing cards with family & friends!",
                                        textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(horizontal = 32.dp).padding(top = 8.dp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(
                                        onClick = { showSetupDialog = true },
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Launch Fast Setup")
                                    }
                                }
                            }
                        } else {
                            allGames.forEach { match ->
                                HistoricallyRecordedGameCard(
                                    game = match,
                                    profileMap = profileMap,
                                    onSelect = { viewModel.selectGame(match.id) },
                                    onDelete = { viewModel.deleteGame(match.id) }
                                )
                            }
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }
                    } else if (selectedTab == 1) {
                        PlayerProfilesScreen(viewModel = viewModel)
                    } else {
                        // CARD GAMES RULES REFERENCE SHEET (Gives rich context to application)
                        RuleReferenceScreen()
                        Spacer(modifier = Modifier.height(100.dp))
                    }
                }
            }
        }

        // NEW GAME SETUP BOTTOM SHEET / DIALOG
        if (showSetupDialog) {
            GameSetupDialog(
                viewModel = viewModel,
                onDismiss = { showSetupDialog = false },
                onLaunch = { gameId ->
                    showSetupDialog = false
                    viewModel.selectGame(gameId)
                }
            )
        }
    }
}

@Composable
fun HistoricallyRecordedGameCard(
    game: GameEntity,
    profileMap: Map<String, PlayerProfile>,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    val players = remember(game.playersJson) { MoshiHelper.fromJsonPlayers(game.playersJson) }
    val formatter = remember { SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault()) }
    val formattedDate = remember(game.createdAt) { formatter.format(Date(game.createdAt)) }
    val isSpades = game.gameType == "SPADES"

    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
            .clickable { onSelect() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isSpades) Icons.Default.Style else Icons.Default.Favorite,
                        contentDescription = game.gameType,
                        tint = if (isSpades) Color(0xFF38BDF8) else Color(0xFFF43F5E),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isSpades) "Spades Score Ledger" else "Hearts Score Ledger",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row {
                    Box(
                        modifier = Modifier
                            .background(
                                color = if (game.status == "ACTIVE") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = game.status,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (game.status == "ACTIVE") MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = "Delete Game",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Player list & scores summary
            val rounds = remember(game.roundsJson) { MoshiHelper.fromJsonRounds(game.roundsJson) }
            val totalScores = remember(rounds, players.size, game.gameType) {
                GameRepository.calculateCumulativeScores(players.size, rounds, game.gameType)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                players.forEachIndexed { idx, name ->
                    val profile = profileMap[name.lowercase().trim()]
                    val avatar = profile?.avatarEmoji ?: "👤"
                    val bColor = profile?.colorHex?.let { parseProfileColor(it) } ?: MaterialTheme.colorScheme.surfaceVariant

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(bColor, shape = CircleShape)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = avatar, fontSize = 16.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${totalScores.getOrElse(idx) { 0 }} pts",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Divider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$formattedDate • ${rounds.size} rounds",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )

                if (game.status == "COMPLETED" && game.winnerName != null) {
                    Text(
                        text = "🏆 Winner: ${game.winnerName}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF59E0B)
                    )
                } else {
                    Text(
                        text = "Goal: ${game.targetScore} pts",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Scoring Sheet") },
            text = { Text("Are you sure you want to delete this scoring history? This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// FULL MATCH SCORING CONTROL PANEL
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameDetailScreen(
    game: GameEntity,
    viewModel: GameViewModel,
    onBack: () -> Unit
) {
    val players = remember(game.playersJson) { MoshiHelper.fromJsonPlayers(game.playersJson) }
    val formatter = remember { SimpleDateFormat("MMMM d, h:mm a", Locale.getDefault()) }
    val formattedDate = remember(game.createdAt) { formatter.format(Date(game.createdAt)) }
    val isSpades = game.gameType == "SPADES"

    val rounds = remember(game.roundsJson) { MoshiHelper.fromJsonRounds(game.roundsJson) }
    val userScores = remember(rounds, players.size, game.gameType) {
        GameRepository.calculateCumulativeScores(players.size, rounds, game.gameType)
    }
    val userBags = remember(rounds, players.size) {
        if (isSpades) GameRepository.calculateCumulativeBags(players.size, rounds) else emptyList()
    }

    val profiles by viewModel.allPlayerProfiles.collectAsStateWithLifecycle()
    val profileMap = remember(profiles) { profiles.associateBy { it.name.lowercase().trim() } }

    var selectedSectionTab by remember { mutableStateOf("Scoreboard") } // "Scoreboard", "Stats & Trends"

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (isSpades) "Spades Match" else "Hearts Match",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = formattedDate,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (game.status == "ACTIVE") {
                        Button(
                            onClick = { viewModel.finishGameManual(game.id) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("End Game")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // MAIN SCOREBOARD CARD - Shows tabletop ranking
            TabletopRankingsCard(
                players = players,
                scores = userScores,
                bags = userBags,
                targetScore = game.targetScore,
                isCompleted = game.status == "COMPLETED",
                winnerName = game.winnerName,
                isSpades = isSpades,
                profileMap = profileMap
            )

            // Dynamic Tabs: "Scoreboard" vs "Stats & Trends"
            TabRow(
                selectedTabIndex = if (selectedSectionTab == "Scoreboard") 0 else 1,
                containerColor = Color.Transparent,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Tab(
                    selected = selectedSectionTab == "Scoreboard",
                    onClick = { selectedSectionTab = "Scoreboard" },
                    text = { Text("Active Ledger") }
                )
                Tab(
                    selected = selectedSectionTab == "Stats & Trends",
                    onClick = { selectedSectionTab = "Stats & Trends" },
                    text = { Text("Trends & Analytics") }
                )
            }

            if (selectedSectionTab == "Scoreboard") {
                // ADD ROUND SCORE FORM (Only if Game is active)
                if (game.status == "ACTIVE") {
                    Card(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Add Round ${rounds.size + 1} Scores",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Box(
                                    modifier = Modifier
                                        .background(
                                            MaterialTheme.colorScheme.inverseSurface,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        "Validation Helper",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.inverseOnSurface
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            if (isSpades) {
                                SpadesScoreEntryView(
                                    players = players,
                                    viewModel = viewModel,
                                    gameId = game.id
                                )
                            } else {
                                HeartsScoreEntryView(
                                    players = players,
                                    viewModel = viewModel,
                                    gameId = game.id
                                )
                            }
                        }
                    }
                }

                // HISTORIC ROUND LOGTABLE
                Text(
                    text = "Round-by-Round Breakdown",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
                )

                if (rounds.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No rounds recorded yet. Fill the inputs above to append scorecards.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    // Scorecard grid
                    Card(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column {
                            // Table Header Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "Rnd",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall,
                                    modifier = Modifier.width(36.dp),
                                    textAlign = TextAlign.Center
                                )
                                players.forEach { name ->
                                    Text(
                                        text = name,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleSmall,
                                        modifier = Modifier.weight(1f),
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.width(32.dp)) // space for delete action
                            }

                            // Table Rows
                            rounds.forEachIndexed { roundIdx, round ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            if (roundIdx % 2 == 0) Color.Transparent
                                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                                        )
                                        .padding(vertical = 10.dp, horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "#${round.roundNumber}",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.width(36.dp),
                                        textAlign = TextAlign.Center
                                    )
                                    round.scores.forEachIndexed { pIdx, score ->
                                        // Detail markers for better game summary context
                                        val detailMarker = if (isSpades) {
                                            val d = round.spadesDetails?.getOrNull(pIdx)
                                            if (d != null) {
                                                val bidString = when (d.bid) {
                                                    -1 -> "Nil"
                                                    -2 -> "BNil"
                                                    else -> "${d.bid}b"
                                                }
                                                "$score\n($bidString/${d.tricksWon}t)"
                                            } else "$score"
                                        } else {
                                            val d = round.heartsDetails?.getOrNull(pIdx)
                                            if (d != null && d.shotTheMoon) "$score\n(🌕)"
                                            else "$score"
                                        }

                                        Text(
                                            text = detailMarker,
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.weight(1f),
                                            textAlign = TextAlign.Center,
                                            overflow = TextOverflow.Clip,
                                            fontWeight = FontWeight.Medium,
                                            lineHeight = 14.sp
                                        )
                                    }
                                    
                                    // Delete/Undo individual round
                                    IconButton(
                                        onClick = { viewModel.deleteRound(game.id, roundIdx) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.RemoveCircleOutline,
                                            contentDescription = "Remove Round",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(80.dp))
                }
            } else {
                // TRENDS & ANALYTICS SECTION
                Text(
                    text = "Score Tracker Trendlines",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp)
                )
                Text(
                    text = if (isSpades) "Spades Goal: High Score Winning" else "Hearts Goal: Low Score Winning (Avoid points!)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp, bottom = 16.dp)
                )

                Card(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        ScoreTrendChart(
                            playerNames = players,
                            rounds = rounds,
                            gameType = game.gameType,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .padding(vertical = 12.dp),
                            profileMap = profileMap
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Chart Legend
                        val playerColors = listOf(
                            Color(0xFFEF4444), // Red
                            Color(0xFF3B82F6), // Blue
                            Color(0xFF10B981), // Emerald
                            Color(0xFFF59E0B), // Amber
                            Color(0xFF8B5CF6), // Purple
                            Color(0xFFEC4899)  // Pink
                        )

                        Text(
                            "Ledger Legend:",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            players.forEachIndexed { idx, name ->
                                val profile = profileMap[name.lowercase().trim()]
                                val legendColor = profile?.colorHex?.let { parseProfileColor(it) } ?: playerColors.getOrElse(idx) { Color.Gray }
                                val legendEmoji = profile?.avatarEmoji

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .background(legendColor, shape = CircleShape)
                                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(legendEmoji ?: "👤", fontSize = 10.sp)
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(120.dp))
            }
        }
    }
}

// Visual layout on active rankings
@Composable
fun TabletopRankingsCard(
    players: List<String>,
    scores: List<Int>,
    bags: List<Int>,
    targetScore: Int,
    isCompleted: Boolean,
    winnerName: String?,
    isSpades: Boolean,
    profileMap: Map<String, PlayerProfile> = emptyMap()
) {
    val playerColors = listOf(
        Color(0xFFEF4444),
        Color(0xFF3B82F6),
        Color(0xFF10B981),
        Color(0xFFF59E0B),
        Color(0xFF8B5CF6),
        Color(0xFFEC4899)
    )

    // Compute indices ranked by winner metric
    // Hearts -> lower is better. Spades -> higher is better.
    val rankedIndices = remember(scores, isSpades) {
        players.indices.sortedWith { a, b ->
            val scoreA = scores.getOrElse(a) { 0 }
            val scoreB = scores.getOrElse(b) { 0 }
            if (isSpades) scoreB.compareTo(scoreA) else scoreA.compareTo(scoreB)
        }
    }

    Card(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tabletop Leaderboard",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )

                Box(
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Target: $targetScore pt",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Ranks list
            rankedIndices.forEachIndexed { rank, playerIdx ->
                val name = players.getOrElse(playerIdx) { "Unknown" }
                val score = scores.getOrElse(playerIdx) { 0 }
                val profile = profileMap[name.lowercase().trim()]
                val badgeColor = profile?.colorHex?.let { parseProfileColor(it) } ?: playerColors.getOrElse(playerIdx) { Color.Gray }
                val avatarEmoji = profile?.avatarEmoji
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .background(
                            if (rank == 0) MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                            else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Rank Index Dot / Number
                        Text(
                            text = "${rank + 1}.",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.width(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        // Rank Avatar Badge
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(badgeColor, shape = CircleShape)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = avatarEmoji ?: "👤",
                                fontSize = 16.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (rank == 0) FontWeight.Bold else FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        if (rank == 0 && !isCompleted) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "👑 Leader",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFF59E0B),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (isCompleted && name == winnerName) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "🏆 Champion",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFF59E0B),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "$score pts",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )

                        // If Spades details, show active bags
                        if (isSpades) {
                            val rawBags = bags.getOrElse(playerIdx) { 0 }
                            val activeBags = rawBags % 10
                            Text(
                                text = "$activeBags/10 Overtricks",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (activeBags >= 8) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }
        }
    }
}

// SPADES AD-HOC ROUND DOCK
@Composable
fun SpadesScoreEntryView(
    players: List<String>,
    viewModel: GameViewModel,
    gameId: Long
) {
    val bids by viewModel.spadesBids.collectAsStateWithLifecycle()
    val tricks by viewModel.spadesTricks.collectAsStateWithLifecycle()

    var activeInputIdx by remember { mutableIntStateOf(0) } // Tracks player we are inputting for

    // Verify constraints: spade totals
    val totalTricksWon = tricks.values.sum()
    val isValidTricks = totalTricksWon == 13

    Column {
        // Player carousel tabs
        ScrollableTabRow(
            selectedTabIndex = activeInputIdx,
            containerColor = Color.Transparent,
            edgePadding = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            players.forEachIndexed { index, name ->
                val playerBid = bids[index]
                val playerBidString = if (playerBid == null) "?" else when (playerBid) {
                    -1 -> "Nil"
                    -2 -> "BNil"
                    else -> "$playerBid"
                }

                val playerTrick = tricks[index]
                val playerTricksString = playerTrick?.toString() ?: "?"

                Tab(
                    selected = activeInputIdx == index,
                    onClick = { activeInputIdx = index },
                    text = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(name, fontWeight = FontWeight.Bold)
                            Text(
                                "Bid: $playerBidString | Trk: $playerTricksString",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Large active control cards
        val activeName = players.getOrElse(activeInputIdx) { "" }
        val currentBid = bids[activeInputIdx] ?: 1
        val currentTrick = tricks[activeInputIdx] ?: 0

        Text(
            text = "Set Bidding & Actual Tricks for: $activeName",
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Bid control spinner
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Trick Bid", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        IconButton(onClick = { 
                            if (currentBid > -2) viewModel.updateSpadesBid(activeInputIdx, currentBid - 1) 
                        }) {
                            Icon(Icons.Default.Remove, contentDescription = "Decrease")
                        }
                        
                        val bidLabel = when (currentBid) {
                            -1 -> "Nil"
                            -2 -> "Blind Nil"
                            else -> "$currentBid"
                        }
                        Text(
                            text = bidLabel,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(80.dp),
                            textAlign = TextAlign.Center
                        )

                        IconButton(onClick = { 
                            if (currentBid < 13) viewModel.updateSpadesBid(activeInputIdx, currentBid + 1) 
                        }) {
                            Icon(Icons.Default.Add, contentDescription = "Increase")
                        }
                    }
                }
            }

            // Tricks control spinner
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Tricks Won", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        IconButton(onClick = { 
                            if (currentTrick > 0) viewModel.updateSpadesTricks(activeInputIdx, currentTrick - 1) 
                        }) {
                            Icon(Icons.Default.Remove, contentDescription = "Decrease")
                        }

                        Text(
                            text = "$currentTrick",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(50.dp),
                            textAlign = TextAlign.Center
                        )

                        IconButton(onClick = { 
                            if (currentTrick < 13) viewModel.updateSpadesTricks(activeInputIdx, currentTrick + 1) 
                        }) {
                            Icon(Icons.Default.Add, contentDescription = "Increase")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tricks sum integrity validation check
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (isValidTricks) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Tricks Tally (Must equal 13):",
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "$totalTricksWon / 13",
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.titleMedium,
                color = if (isValidTricks) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
        }

        if (!isValidTricks) {
            Text(
                "Warning: High confidence score calculations rely on exactly 13 tricks being distributed in physical spade decks.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 4.dp, start = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Next player button
            OutlinedButton(
                onClick = { 
                    activeInputIdx = (activeInputIdx + 1) % players.size
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Next Player")
            }

            Button(
                onClick = { viewModel.submitSpadesRound(gameId) },
                enabled = true, // We allow submitting anyway to support custom house rules where tricks might be skipped/penalized
                modifier = Modifier.weight(1.5f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Log Round Result")
            }
        }
    }
}

// HEARTS AD-HOC ROUND DOCK
@Composable
fun HeartsScoreEntryView(
    players: List<String>,
    viewModel: GameViewModel,
    gameId: Long
) {
    val scores by viewModel.heartsEntryScores.collectAsStateWithLifecycle()
    val shooterIdx by viewModel.heartsShooterIdx.collectAsStateWithLifecycle()
    val moonRuleIsOthersPenalize by viewModel.heartsMoonPenalizeOthers.collectAsStateWithLifecycle()

    var activeInputIdx by remember { mutableIntStateOf(0) }

    // Validation: hearts round must total exactly 26 (13 hearts + 13 for Qs) unless someone shot the moon
    val totalRoundPtsVal = scores.values.sum()
    val isMoonShotActive = shooterIdx != null
    val isValidHeartsSum = totalRoundPtsVal == 26 || isMoonShotActive

    Column {
        // Shoot moon banner option
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Shoot the Moon Scenario",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (moonRuleIsOthersPenalize) "+26 to Opponents" else "-26 to Shooter",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(onClick = { viewModel.toggleMoonScoringOption() }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.SwapCalls, contentDescription = "Swap rule", modifier = Modifier.size(16.dp))
                }
            }
        }

        // Horizontal Carousel for entering scores
        ScrollableTabRow(
            selectedTabIndex = activeInputIdx,
            containerColor = Color.Transparent,
            edgePadding = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            players.forEachIndexed { index, name ->
                val score = scores[index] ?: 0
                val tag = if (shooterIdx == index) "🌕 Moon" else "$score pt"
                
                Tab(
                    selected = activeInputIdx == index,
                    onClick = { activeInputIdx = index },
                    text = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(name, fontWeight = FontWeight.Bold)
                            Text(tag, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Controls
        val activeName = players.getOrElse(activeInputIdx) { "" }
        val currentScore = scores[activeInputIdx] ?: 0

        Text(
            text = "Set Round Points for: $activeName",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Outlined control
            Card(
                modifier = Modifier.weight(1.5f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.incrementHeartsScore(activeInputIdx, -1) }) {
                        Icon(Icons.Default.Remove, contentDescription = "Minus 1")
                    }

                    Text(
                        text = "$currentScore pts",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )

                    IconButton(onClick = { viewModel.incrementHeartsScore(activeInputIdx, 1) }) {
                        Icon(Icons.Default.Add, contentDescription = "Plus 1")
                    }
                }
            }

            // Shoot the Moon Action button
            Button(
                onClick = { viewModel.shootTheMoon(activeInputIdx) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (shooterIdx == activeInputIdx) Color(0xFFF59E0B) else MaterialTheme.colorScheme.secondary
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1.2f)
            ) {
                Icon(
                    Icons.Default.Brightness2, 
                    contentDescription = null, 
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (shooterIdx == activeInputIdx) "Shooting!" else "Shot Moon",
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Fast Preset tags for quick entry (Q of Spades = 13, Hearts = 1 each)
        Text(
            "Quick Assist Increments:",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AssistChip(
                onClick = { viewModel.updateHeartsScore(activeInputIdx, 13) },
                label = { Text("♠️ Q (13 pts)") }
            )
            AssistChip(
                onClick = { viewModel.incrementHeartsScore(activeInputIdx, 1) },
                label = { Text("❤️ Heart (+1)") }
            )
            AssistChip(
                onClick = { viewModel.updateHeartsScore(activeInputIdx, 0) },
                label = { Text("Clean (0 pts)") }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Hearts validation Tally Box
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (isValidHeartsSum) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Total Points Logged (Normally 26):",
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = if (isMoonShotActive) "Shot Moon Active" else "$totalRoundPtsVal / 26",
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.titleMedium,
                color = if (isValidHeartsSum) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
        }

        if (!isValidHeartsSum) {
            Text(
                "A standard round has exactly 26 points (13 Hearts + 13 value for Queen of Spades). Double check scores to prevent entry typo errors.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, start = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = { activeInputIdx = (activeInputIdx + 1) % players.size },
                modifier = Modifier.weight(1f)
            ) {
                Text("Next Player")
            }

            Button(
                onClick = { viewModel.submitHeartsRound(gameId) },
                modifier = Modifier.weight(1.5f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Log Round Result")
            }
        }
    }
}

// FAST CUSTOM GAME CONFIG DIALOG
@Composable
fun GameSetupDialog(
    viewModel: GameViewModel,
    onDismiss: () -> Unit,
    onLaunch: (Long) -> Unit
) {
    var selectedType by remember { mutableStateOf(GameType.HEARTS) }
    var targetScoreText by remember { mutableStateOf("100") }
    val playersList by viewModel.setupPlayersList.collectAsStateWithLifecycle()
    val profiles by viewModel.allPlayerProfiles.collectAsStateWithLifecycle()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Configure Scoring Sheet",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    "Select Game Type:",
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                selectedType = GameType.HEARTS
                                targetScoreText = "100"
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedType == GameType.HEARTS) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(
                            2.dp,
                            if (selectedType == GameType.HEARTS) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Favorite, contentDescription = "Hearts", tint = Color(0xFFEF4444))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Hearts", fontWeight = FontWeight.Bold)
                            Text("Low Score Wins", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                selectedType = GameType.SPADES
                                targetScoreText = "250"
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedType == GameType.SPADES) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(
                            2.dp,
                            if (selectedType == GameType.SPADES) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Style, contentDescription = "Spades", tint = Color(0xFF38BDF8))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Spades", fontWeight = FontWeight.Bold)
                            Text("Bids & Bags", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Target Score Field
                OutlinedTextField(
                    value = targetScoreText,
                    onValueChange = { targetScoreText = it.filter { char -> char.isDigit() } },
                    label = { Text("Winning / End Score Limit") },
                    placeholder = { Text(if (selectedType == GameType.HEARTS) "e.g. 100" else "e.g. 250") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Players Setup (${playersList.size}):",
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    if (playersList.size < 6) {
                        IconButton(onClick = { viewModel.addSetupPlayer() }) {
                            Icon(Icons.Default.AddCircle, contentDescription = "Add Player", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                playersList.forEachIndexed { idx, name ->
                    var showDropdown by remember { mutableStateOf(false) }
                    val profile = remember(profiles, name) {
                        profiles.firstOrNull { it.name.lowercase().trim() == name.lowercase().trim() }
                    }
                    val avatar = profile?.avatarEmoji ?: "👤"
                    val bColor = profile?.colorHex?.let { parseProfileColor(it) } ?: MaterialTheme.colorScheme.surfaceVariant

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box {
                            Box(
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .size(44.dp)
                                    .background(bColor, shape = CircleShape)
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                    .clickable { showDropdown = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(avatar, fontSize = 20.sp)
                            }

                            if (profiles.isNotEmpty()) {
                                DropdownMenu(
                                    expanded = showDropdown,
                                    onDismissRequest = { showDropdown = false }
                                ) {
                                    Text(
                                        "Assign Squad Profile",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    HorizontalDivider()
                                    profiles.forEach { p ->
                                        DropdownMenuItem(
                                            text = {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(24.dp)
                                                            .background(parseProfileColor(p.colorHex), shape = CircleShape),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(p.avatarEmoji, fontSize = 12.sp)
                                                    }
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(p.name, fontWeight = FontWeight.Bold)
                                                }
                                            },
                                            onClick = {
                                                viewModel.updateSetupPlayerName(idx, p.name)
                                                showDropdown = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = name,
                            onValueChange = { viewModel.updateSetupPlayerName(idx, it) },
                            placeholder = { Text("Player Name") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                        )
                        if (playersList.size > 2) {
                            IconButton(onClick = { viewModel.removeSetupPlayer(idx) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove Player", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val limit = targetScoreText.toIntOrNull() ?: if (selectedType == GameType.HEARTS) 100 else 250
                    viewModel.startNewGame(selectedType, limit, onLaunch)
                }
            ) {
                Text("Start Game")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// CUSTOM LINE CHART CANVAS GRAPH
@Composable
fun ScoreTrendChart(
    playerNames: List<String>,
    rounds: List<GameRound>,
    gameType: String,
    modifier: Modifier = Modifier,
    profileMap: Map<String, PlayerProfile> = emptyMap()
) {
    if (rounds.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(
                "No rounds log recorded yet. Graphs will render after round log entries.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp)
            )
        }
        return
    }

    val playerColors = listOf(
        Color(0xFFEF4444), // Crimson
        Color(0xFF3B82F6), // Azure Blue
        Color(0xFF10B981), // Emerald
        Color(0xFFF59E0B), // Amber
        Color(0xFF8B5CF6), // Amethyst Purple
        Color(0xFFEC4899)  // Hot Pink
    )

    // Calculate lists of cumulative scores for each round
    val roundPoints = remember(rounds, playerNames.size) {
        val pointsList = List(playerNames.size) { mutableListOf<Float>() }
        
        // Match start: score is 0
        playerNames.indices.forEach { pointsList[it].add(0f) }
        
        // Accumulate
        val cumulative = MutableList(playerNames.size) { 0f }
        for (round in rounds) {
            playerNames.indices.forEach { idx ->
                cumulative[idx] += round.scores.getOrElse(idx) { 0 }.toFloat()
                pointsList[idx].add(cumulative[idx])
            }
        }
        pointsList
    }

    val maxScore = remember(roundPoints) {
        val maxVal = roundPoints.flatMap { it }.maxOrNull() ?: 100f
        val minVal = roundPoints.flatMap { it }.minOrNull() ?: 0f
        maxOf(maxVal, abs(minVal), 10f)
    }

    val minScore = remember(roundPoints) {
        val minVal = roundPoints.flatMap { it }.minOrNull() ?: 0f
        minOf(minVal, 0f)
    }

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val paddingLeft = 30f
        val paddingRight = 30f
        val paddingTop = 20f
        val paddingBottom = 20f

        val usableWidth = width - (paddingLeft + paddingRight)
        val usableHeight = height - (paddingTop + paddingBottom)

        val totalPointsCount = rounds.size + 1
        val xStep = usableWidth / (totalPointsCount - 1).coerceAtLeast(1)
        val yRange = if (maxScore - minScore == 0f) 10f else maxScore - minScore

        // Draw grid lines
        val gridLines = 4
        for (i in 0..gridLines) {
            val ratio = i.toFloat() / gridLines
            val y = paddingTop + usableHeight - (ratio * usableHeight)
            drawLine(
                color = Color.LightGray.copy(alpha = 0.3f),
                start = Offset(paddingLeft, y),
                end = Offset(width - paddingRight, y),
                strokeWidth = 1.5f
            )
        }

        // Draw lines and legends
        playerNames.indices.forEach { playerIdx ->
            val name = playerNames.getOrElse(playerIdx) { "" }
            val profile = profileMap[name.lowercase().trim()]
            val color = profile?.colorHex?.let { parseProfileColor(it) } ?: playerColors.getOrElse(playerIdx) { Color.Gray }
            val points = roundPoints[playerIdx]
            
            val path = androidx.compose.ui.graphics.Path().apply {
                points.forEachIndexed { ptIdx, score ->
                    val x = paddingLeft + (ptIdx * xStep)
                    val y = paddingTop + usableHeight - (((score - minScore) / yRange) * usableHeight)
                    if (ptIdx == 0) {
                        moveTo(x, y)
                    } else {
                        lineTo(x, y)
                    }
                }
            }

            // Draw line Path
            drawPath(
                path = path,
                color = color,
                style = Stroke(width = 5f, cap = StrokeCap.Round)
            )

            // Draw circular vertices
            points.forEachIndexed { ptIdx, score ->
                val x = paddingLeft + (ptIdx * xStep)
                val y = paddingTop + usableHeight - (((score - minScore) / yRange) * usableHeight)
                
                drawCircle(
                    color = color,
                    radius = 8f,
                    center = Offset(x, y)
                )
                drawCircle(
                    color = Color.White,
                    radius = 3.5f,
                    center = Offset(x, y)
                )
            }
        }
    }
}

// RULES EXPLANATION AND CARD SCORE ASSISTANCE
@Composable
fun RuleReferenceScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            "Card Scoring Helper & Guide",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Hearts guide
        Card(
            modifier = Modifier
                .padding(vertical = 8.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Favorite, contentDescription = null, tint = Color(0xFFEF4444))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Hearts Scoring Rules", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                BulletText("Objective: Score as FEW points as possible. Game ends when a player hits the target score (e.g. 100). The player with the lowest score wins!")
                BulletText("Penalty Cards: Every Heart taken in tricks = +1 point. Queen of Spades (♠️ Q) = +13 points.")
                BulletText("Shoot the Moon Option: If a single player takes ALL 13 Hearts and the Queen of Spades in a single round. That player scores 0 points while all other opponents score +26 points! Or, they can choose to subtract -26 points from their own total.")
            }
        }

        // Spades guide
        Card(
            modifier = Modifier
                .padding(vertical = 8.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Style, contentDescription = null, tint = Color(0xFF38BDF8))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Spades Scoring Rules", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                BulletText("Objective: Accumulate points by bidding and winning tricks. Target score is typically 250 or 500. Highest score wins!")
                BulletText("Success Score: If tricks won >= bid: Score = 10 * bid + (tricks - bid). Overtricks are earned as 'Bags' (e.g., bid 4, won 5 tricks = +41 points and +1 bag).")
                BulletText("Set (Failed) Score: If tricks won < bid: Score = -10 * bid (e.g., bid 5, won 4 tricks = -50 points).")
                BulletText("Nil scoring: Bidding Nil (0 tricks) yields +100 points on success, or -100 points if failed. Bidding Blind Nil yields +200 on success, or -200 if failed.")
                BulletText("10 Bags Rule: Bag management is vital! Accumulating 10 bags triggers a penalty: -100 points, resetting your active bag count by -10. Our automatic companion ledger takes care of this math instantly.")
            }
        }
    }
}

@Composable
fun BulletText(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text("•", fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 8.dp), color = MaterialTheme.colorScheme.primary)
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// FlowRow copy implementation for Compose layout compatibility
@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    androidx.compose.ui.layout.Layout(
        content = content,
        modifier = modifier
    ) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints) }
        val layoutWidth = constraints.maxWidth
        
        var currentX = 0
        var currentY = 0
        var maxRowHeight = 0
        
        // Coordinates for placing components
        val positions = mutableListOf<Pair<Int, Int>>()
        
        placeables.forEach { placeable ->
            if (currentX + placeable.width > layoutWidth) {
                currentX = 0
                currentY += maxRowHeight + verticalArrangement.let { 8.dp.roundToPx() }
                maxRowHeight = 0
            }
            positions.add(currentX to currentY)
            currentX += placeable.width + horizontalArrangement.let { 16.dp.roundToPx() }
            maxRowHeight = maxOf(maxRowHeight, placeable.height)
        }
        
        layout(layoutWidth, currentY + maxRowHeight) {
            placeables.forEachIndexed { idx, placeable ->
                val (x, y) = positions[idx]
                placeable.placeRelative(x, y)
            }
        }
    }
}

// String extension capitalization utility
fun String.capitalize(locale: Locale): String {
    return replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
}
