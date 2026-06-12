package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.api.FactCheckResult
import com.example.data.db.AppDatabase
import com.example.data.db.SavedFactCheck
import com.example.data.repository.FactCheckRepository
import com.example.data.repository.LiveFactFeedItem
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.*
import com.example.ui.viewmodel.FactViewModel
import com.example.ui.viewmodel.FactViewModelFactory
import com.example.ui.viewmodel.LiveCheckState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Init database and repository
        val database = AppDatabase.getDatabase(this)
        val repository = FactCheckRepository(database.factCheckDao())
        
        // Instantiate ViewModel
        val viewModel: FactViewModel by viewModels { FactViewModelFactory(repository) }

        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("main_scaffold"),
                    contentWindowInsets = WindowInsets.safeDrawing
                ) { innerPadding ->
                    FactLiveAppContent(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun FactLiveAppContent(
    viewModel: FactViewModel,
    modifier: Modifier = Modifier
) {
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CharcoalBlack)
    ) {
        // --- Top Brand Banner Header ---
        HeaderSection()

        // --- Navigation Tab Row Section ---
        TabSection(
            selectedTab = selectedTab,
            onTabSelected = { viewModel.setTab(it) }
        )

        // --- Core Tab Routing content switcher ---
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (selectedTab) {
                0 -> PulseFeedScreen(viewModel = viewModel)
                1 -> AiVerifierScreen(viewModel = viewModel)
                2 -> SavedTruthsScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun HeaderSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Live Status indicator
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(Color.Red)
            )
            Spacer(modifier = Modifier.width(8.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "FACT",
                    color = CrimsonRed,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = "LIVE",
                    color = MutedSlate,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Light,
                    fontFamily = FontFamily.SansSerif,
                    letterSpacing = (-0.5).sp
                )
            }

            // Portal Tag badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(CrimsonRed.copy(alpha = 0.15f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "WWW.FACTLIVE.IN",
                    color = PastelRed,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Kerala's Premium News Verification Portal. Exposing online hoaxes and validating claims using AI models.",
            color = MutedSlate,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = BorderSlate, thickness = 1.dp)
    }
}

@Composable
fun TabSection(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    TabRow(
        selectedTabIndex = selectedTab,
        containerColor = CharcoalBlack,
        contentColor = CrimsonRed,
        indicator = { tabPositions ->
            TabRowDefaults.SecondaryIndicator(
                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                color = CrimsonRed,
                height = 3.dp
            )
        },
        divider = {
            HorizontalDivider(color = BorderSlate)
        }
    ) {
        Tab(
            selected = selectedTab == 0,
            onClick = { onTabSelected(0) },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.FactCheck,
                        contentDescription = "Pulse icon",
                        tint = if (selectedTab == 0) CrimsonRed else MutedSlate,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Pulse Feed",
                        fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                        color = if (selectedTab == 0) PureWhite else MutedSlate,
                        fontSize = 13.sp
                    )
                }
            },
            modifier = Modifier.testTag("tab_pulse")
        )
        Tab(
            selected = selectedTab == 1,
            onClick = { onTabSelected(1) },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Verify icon",
                        tint = if (selectedTab == 1) CrimsonRed else MutedSlate,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "AI Desk",
                        fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                        color = if (selectedTab == 1) PureWhite else MutedSlate,
                        fontSize = 13.sp
                    )
                }
            },
            modifier = Modifier.testTag("tab_verifier")
        )
        Tab(
            selected = selectedTab == 2,
            onClick = { onTabSelected(2) },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Bookmark,
                        contentDescription = "Saved icon",
                        tint = if (selectedTab == 2) CrimsonRed else MutedSlate,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Saved Truths",
                        fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal,
                        color = if (selectedTab == 2) PureWhite else MutedSlate,
                        fontSize = 13.sp
                    )
                }
            },
            modifier = Modifier.testTag("tab_saved")
        )
    }
}

// ============================================
// SCREEN 1: PULSE FEED / PRESETS DIRECTORY
// ============================================
@Composable
fun PulseFeedScreen(viewModel: FactViewModel) {
    val searchVal by viewModel.searchQuery.collectAsStateWithLifecycle()
    val presets by viewModel.filteredPresets.collectAsStateWithLifecycle()
    val expandedId by viewModel.expandedFeedId.collectAsStateWithLifecycle()
    val savedChecks by viewModel.savedFactChecks.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // Search Bar for Presets
        OutlinedTextField(
            value = searchVal,
            onValueChange = { viewModel.updateSearchQuery(it) },
            placeholder = { Text("Search debunked rumors & social claims...", color = MutedSlate, fontSize = 14.sp) },
            leadingIcon = { Icon(Icons.Default.Search, "Search Icon", tint = MutedSlate) },
            trailingIcon = {
                if (searchVal.isNotEmpty()) {
                    IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                        Icon(Icons.Default.Close, "Clear Icon", tint = MutedSlate)
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CrimsonRed,
                unfocusedBorderColor = BorderSlate,
                focusedTextColor = PureWhite,
                unfocusedTextColor = PureWhite,
                focusedContainerColor = DeepSlate,
                unfocusedContainerColor = DeepSlate
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("presets_search_input")
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "RECENT FACT CHECKS FROM CORRESPONDENTS",
            color = MutedSlate,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (presets.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Empty feeds",
                        tint = MutedSlate,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No claims match your query.",
                        color = MutedSlate,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(presets, key = { it.id }) { item ->
                    val isBookmarked = savedChecks.any { it.claim == item.claim }
                    PresetFeedItemCard(
                        item = item,
                        isExpanded = expandedId == item.id,
                        isBookmarked = isBookmarked,
                        onToggleExpand = { viewModel.toggleFeedExpansion(item.id) },
                        onToggleBookmark = { viewModel.toggleBookmarkPreset(item) }
                    )
                }
            }
        }
    }
}

@Composable
fun PresetFeedItemCard(
    item: LiveFactFeedItem,
    isExpanded: Boolean,
    isBookmarked: Boolean,
    onToggleExpand: () -> Unit,
    onToggleBookmark: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DeepSlate),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleExpand() }
            .testTag("preset_card_${item.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Category & Date Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.category.uppercase(),
                    color = TechTeal,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.sp,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = item.date,
                    color = MutedSlate,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Claim Heading
            Text(
                text = item.claim,
                color = PureWhite,
                fontSize = 16.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = if (item.isRegional) FontFamily.SansSerif else FontFamily.Default
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Verdict status pill + bookmark button row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                VerdictPill(verdict = item.verdict)

                Spacer(modifier = Modifier.weight(1f))

                // Bookmark icon
                IconButton(
                    onClick = { onToggleBookmark() },
                    modifier = Modifier.testTag("bookmark_preset_${item.id}")
                ) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "Bookmark button",
                        tint = if (isBookmarked) CrimsonRed else MutedSlate,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Expand indicator
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = "Expand icon",
                    tint = MutedSlate,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Expanded news details
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(animationSpec = tween(300)) + fadeIn(),
                exit = shrinkVertically(animationSpec = tween(300)) + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp)
                ) {
                    HorizontalDivider(color = BorderSlate, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "DEBUNK SUMMARY:",
                        color = CrimsonRed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.summary,
                        color = PureWhite,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "DETAILED VERIFIED TRUTH:",
                        color = CrimsonRed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.details,
                        color = PureWhite.copy(alpha = 0.9f),
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "ORIGIN & CONTEXT:",
                        color = WarningAmber,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.context,
                        color = MutedSlate,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "OFFICIAL SOURCE REFERENCES:",
                        color = TruthGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.references,
                        color = MutedSlate,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

// ============================================
// SCREEN 2: ACTIVE AI VERIFICATION SANDBOX
// ============================================
@Composable
fun AiVerifierScreen(viewModel: FactViewModel) {
    val inputClaim by viewModel.liveClaimInput.collectAsStateWithLifecycle()
    val verifState by viewModel.liveCheckState.collectAsStateWithLifecycle()
    val isSaved by viewModel.isCurrentClaimSaved.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val listState = remember { androidx.compose.foundation.lazy.LazyListState() }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(10.dp))
            
            // Explanation panel
            Card(
                colors = CardDefaults.cardColors(containerColor = RaisedSlate),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "AI desk icon",
                        tint = WarningAmber,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "AI-Driven Rumor Buster",
                            color = PureWhite,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Enter any suspicious claim, social post, or WhatsApp rumor below. Our AI scans archives, patterns, and context to verify.",
                            color = MutedSlate,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Multi-line Input Field
            OutlinedTextField(
                value = inputClaim,
                onValueChange = { viewModel.updateLiveClaimInput(it) },
                label = { Text("What claim or news link do you want to verify?", color = MutedSlate) },
                placeholder = { Text("Paste questionable statements, forwards, or news titles...", color = MutedSlate.copy(alpha = 0.5f)) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CrimsonRed,
                    unfocusedBorderColor = BorderSlate,
                    focusedTextColor = PureWhite,
                    unfocusedTextColor = PureWhite,
                    focusedContainerColor = DeepSlate,
                    unfocusedContainerColor = DeepSlate
                ),
                shape = RoundedCornerShape(12.dp),
                minLines = 4,
                maxLines = 6,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ai_claim_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Actions: Paste + Clear
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Clipboard Paste Button
                OutlinedButton(
                    onClick = {
                        val sysClip = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clipString = sysClip.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
                        if (clipString.isNotBlank()) {
                            viewModel.updateLiveClaimInput(clipString)
                            Toast.makeText(context, "Pasted claim from clipboard", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "No text in clipboard to paste", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MutedSlate),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "clipboard", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Paste Claim", fontSize = 12.sp)
                }

                // Clear button
                if (inputClaim.isNotEmpty()) {
                    OutlinedButton(
                        onClick = { viewModel.clearLiveVerification() },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MutedSlate),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Clear, contentDescription = "clear text", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Clear All", fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Verification Trigger button
            Button(
                onClick = { viewModel.verifyClaimLive() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = CrimsonRed,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                enabled = verifState !is LiveCheckState.Loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("ai_verify_button")
            ) {
                if (verifState is LiveCheckState.Loading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "sparkle icon",
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Dehoax Using Fact Live AI",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Short warning notice as required by Security Mandate
            Text(
                text = "Notice: Submissions are fact-analysed in real time using serverside Gemini models. Fact-checking relies on verified online records.",
                color = MutedSlate,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // --- Real Verification State Outputs ---
            AnimatedContent(
                targetState = verifState,
                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) }
            ) { state ->
                when (state) {
                    is LiveCheckState.Idle -> {
                        // Quick Suggestions / Trending myths to tap and try out
                        SuggestedMythsRow(onSelect = { viewModel.updateLiveClaimInput(it) })
                    }
                    is LiveCheckState.Loading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = TechTeal)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Cross-referencing claims databases...",
                                    color = TechTeal,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Analyzing image metadata and syntax checks.",
                                    color = MutedSlate,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                    is LiveCheckState.Success -> {
                        val result = state.result
                        LiveFactResultCard(
                            result = result,
                            claimText = inputClaim,
                            isSaved = isSaved,
                            onToggleSave = { viewModel.toggleBookmarkLive() }
                        )
                    }
                    is LiveCheckState.Error -> {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.Red.copy(alpha = 0.15f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Cancel, contentDescription = "Error", tint = Color.Red)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = state.message,
                                    color = PureWhite,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SuggestedMythsRow(onSelect: (String) -> Unit) {
    val suggestions = listOf(
        "WhatsApp forward claiming that drinking lukewarm water cures coronavirus variants instantly.",
        "NASA warning claiming 3 days of earth darkness due to cosmic storms on June 20, 2026.",
        "Deepfake audio video of political speech circulating on Facebook."
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "TAP A EXAMPLE MYTH TO TEST LIVE:",
            color = MutedSlate,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(10.dp))
        suggestions.forEach { suggestion ->
            Card(
                colors = CardDefaults.cardColors(containerColor = RaisedSlate),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { onSelect(suggestion) }
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = "suggest",
                        tint = WarningAmber,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = suggestion,
                        color = PureWhite.copy(alpha = 0.85f),
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun LiveFactResultCard(
    result: FactCheckResult,
    claimText: String,
    isSaved: Boolean,
    onToggleSave: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DeepSlate),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ai_result_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Verdict row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "AI FINAL VERDICT",
                    color = MutedSlate,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.weight(1f)
                )

                // Save truth bookmark
                IconButton(onClick = onToggleSave) {
                    Icon(
                        imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "Save verification",
                        tint = if (isSaved) CrimsonRed else MutedSlate,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Main claim title
            Text(
                text = "\"$claimText\"",
                color = PureWhite,
                fontSize = 15.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.SemiBold,
                fontStyle = FontStyle.Italic
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Traffic light status verdict indicators
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                VerdictPill(verdict = result.verdict)
                Spacer(modifier = Modifier.width(16.dp))
                
                // Confidence rating indicator
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "Confidence Score: ", color = MutedSlate, fontSize = 11.sp)
                        Text(
                            text = "${result.confidence}%",
                            color = when {
                                result.confidence >= 80 -> TruthGreen
                                result.confidence >= 50 -> WarningAmber
                                else -> CrimsonRed
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    LinearProgressIndicator(
                        progress = { result.confidence / 100f },
                        color = when {
                            result.confidence >= 80 -> TruthGreen
                            result.confidence >= 50 -> WarningAmber
                            else -> CrimsonRed
                        },
                        trackColor = BorderSlate,
                        modifier = Modifier
                            .width(120.dp)
                            .height(6.dp)
                            .padding(top = 4.dp)
                            .clip(CircleShape)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = BorderSlate, thickness = 1.dp)
            Spacer(modifier = Modifier.height(14.dp))

            // Deep analysis block
            Text(
                text = "INVESTIGATIVE ANALYSIS:",
                color = CrimsonRed,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = result.analysis,
                color = PureWhite,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Context block
            Text(
                text = "ORIGINAL SOURCE CONTEXT:",
                color = WarningAmber,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = result.context,
                color = MutedSlate,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Live reference links checked
            Text(
                text = "SOURCES & CITATIONS CHECKED:",
                color = TruthGreen,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = result.references,
                color = TechTeal.copy(alpha = 0.9f),
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

// ============================================
// SCREEN 3: LOCAL ARCHIVES / BOOKMARKS
// ============================================
@Composable
fun SavedTruthsScreen(viewModel: FactViewModel) {
    val savedItems by viewModel.savedFactChecks.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "YOUR LOCAL VERIFICATION STASH",
            color = MutedSlate,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (savedItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.BookmarkBorder,
                        contentDescription = "Empty icon",
                        tint = BorderSlate,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Stash is empty.",
                        color = PureWhite,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Bookmarked news feed items and AI-verified rumors will appear here for local offline reference.",
                        color = MutedSlate,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(savedItems, key = { it.id }) { item ->
                    SavedClaimCard(
                        item = item,
                        onDelete = { viewModel.deleteSavedCheckById(item.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun SavedClaimCard(
    item: SavedFactCheck,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = DeepSlate),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Verdict status pill + Delete button row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                VerdictPill(verdict = item.verdict)
                
                Spacer(modifier = Modifier.width(10.dp))
                
                if (item.confidence > 0) {
                    Text(
                        text = "${item.confidence}% Conf.",
                        color = MutedSlate,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Delete bookmark button
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete bookmark",
                        tint = MutedSlate,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Claim text
            Text(
                text = item.claim,
                color = PureWhite,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 20.sp
            )

            // Expansion details
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    HorizontalDivider(color = BorderSlate, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "ANALYSIS:",
                        color = CrimsonRed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.analysis,
                        color = PureWhite.copy(alpha = 0.9f),
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    if (item.context.isNotBlank()) {
                        Text(
                            text = "CONTEXT & ORIGIN:",
                            color = WarningAmber,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = item.context,
                            color = MutedSlate,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    if (item.references.isNotBlank()) {
                        Text(
                            text = "CITATIONS CHECKED:",
                            color = TruthGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = item.references,
                            color = TechTeal.copy(alpha = 0.85f),
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

// --- Common UI Components ---

@Composable
fun VerdictPill(verdict: String) {
    val cleanVerdict = verdict.trim()
    val bgColor = when (cleanVerdict) {
        "False" -> FalseRose.copy(alpha = 0.12f)
        "True" -> TruthGreen.copy(alpha = 0.12f)
        "Misleading" -> WarningAmber.copy(alpha = 0.12f)
        "Partially True" -> WarningAmber.copy(alpha = 0.12f)
        else -> BorderSlate.copy(alpha = 0.2f)
    }
    
    val textColor = when (cleanVerdict) {
        "False" -> FalseRose
        "True" -> TruthGreen
        "Misleading" -> WarningAmber
        "Partially True" -> WarningAmber
        else -> MutedSlate
    }

    val icon = when (cleanVerdict) {
        "False" -> Icons.Default.Cancel
        "True" -> Icons.Default.CheckCircle
        "Misleading" -> Icons.Default.Warning
        "Partially True" -> Icons.Default.Warning
        else -> Icons.Default.Info
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "verdict icon",
            tint = textColor,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = cleanVerdict.uppercase(),
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.5.sp
        )
    }
}
