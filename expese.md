# 💸 PayLens — Personal Expense Tracker
### Complete End-to-End Implementation Plan for AI Agent

---

## Table of Contents

1. [Product Vision & Philosophy](#1-product-vision--philosophy)
2. [Tech Stack & Architecture](#2-tech-stack--architecture)
3. [Data Architecture (Room Schema)](#3-data-architecture-room-schema)
4. [App Module Structure](#4-app-module-structure)
5. [Detection Layer — Accessibility + Notification + SMS](#5-detection-layer)
6. [Transaction Intelligence Engine](#6-transaction-intelligence-engine)
7. [Screen-by-Screen Wireframes & UI Spec](#7-screen-by-screen-wireframes--ui-spec)
8. [Navigation Architecture](#8-navigation-architecture)
9. [Koin Dependency Injection Map](#9-koin-dependency-injection-map)
10. [Implementation Phases (AI Agent Steps)](#10-implementation-phases-for-ai-agent)
11. [Edge Cases & Resilience Patterns](#11-edge-cases--resilience-patterns)
12. [Analytics Engine Spec](#12-analytics-engine-spec)

---

## 1. Product Vision & Philosophy

**PayLens** is a zero-friction personal UPI expense tracker that:
- Automatically captures every UPI payment via Accessibility Service + Notification Listener + SMS
- Classifies transactions intelligently using contact/merchant mapping
- Lets users curate, group, and analyze their spending
- Never reads banking data directly — it reads the *shadow of money* on screen

**Core Design Principles:**
- Capture first, classify later — never block a log because classification is incomplete
- Every transaction is an independent entity; UI grouping is just a view
- User classification always overrides system classification
- Local-first, no cloud dependency

---

## 2. Tech Stack & Architecture

```
Language         : Kotlin
UI               : Jetpack Compose (Material 3)
Architecture     : MVVM + Clean Architecture (3-layer)
DI               : Koin (with KSP annotations via koin-annotations)
DB               : Room (KSP processor)
Async            : Kotlin Coroutines + Flow
Navigation       : Compose Navigation (type-safe via KSP)
Background       : WorkManager (reconciliation), Foreground Service (accessibility wrapper)
Charts           : Vico (Compose-native chart library)
Date/Time        : kotlinx-datetime
Preferences      : DataStore (Proto)
Permissions      : Accompanist Permissions
Testing          : JUnit5, MockK, Turbine (Flow testing)
Build            : Gradle KTS + Version Catalog (libs.versions.toml)
```

**Architecture Layers:**
```
presentation/     ← Compose Screens, ViewModels
domain/           ← UseCases, Domain Models, Repository Interfaces  
data/             ← Room DAOs, Entities, Repository Implementations
                  ← Accessibility Service, Notification Listener, SMS Reader
```

---

## 3. Data Architecture (Room Schema)

### 3.1 Entity: `TransactionEntity`

```kotlin
@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String,               // UUID
    val amount: Double,
    val timestamp: Long,                       // epoch millis
    val upiId: String?,                        // raw UPI VPA if extracted
    val contactName: String?,                  // name from UPI success screen / SMS
    val rawDescription: String,                // full raw text captured
    val source: String,                        // "ACCESSIBILITY" | "NOTIFICATION" | "SMS"
    val sourceApp: String?,                    // "com.google.android.apps.nbu.paisa.user"
    val direction: String,                     // "DEBIT" | "CREDIT"
    val status: String,                        // "SUCCESS" | "PENDING" | "FAILED"
    val groupId: String?,                      // FK → GroupEntity.id (null = unclassified)
    val entityId: String?,                     // FK → TrackedEntityEntity.id (null = unknown)
    val isUserClassified: Boolean = false,     // true = user manually set group/entity
    val note: String? = null,                  // user added note
    val createdAt: Long = System.currentTimeMillis()
)
```

### 3.2 Entity: `TrackedEntityEntity`
> A "tracked entity" is a person, merchant, or service that money flows to/from.

```kotlin
@Entity(tableName = "tracked_entities")
data class TrackedEntityEntity(
    @PrimaryKey val id: String,               // UUID
    val displayName: String,                  // "Prafull", "Zomato", "Electricity Board"
    val type: String,                         // "PERSON" | "MERCHANT" | "SERVICE" | "UNKNOWN"
    val upiIds: String,                       // JSON array of known UPI VPAs for this entity
    val phoneNumbers: String,                 // JSON array
    val aliases: String,                      // JSON array — "Prafull bhai", "P bhai"
    val defaultGroupId: String?,              // auto-assign this group when matched
    val avatarColor: Int,                     // generated color for avatar
    val isUserCreated: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val lastTransactionAt: Long? = null
)
```

### 3.3 Entity: `GroupEntity`
> Groups are user-defined spending categories. System creates defaults, user can edit/add.

```kotlin
@Entity(tableName = "groups")
data class GroupEntity(
    @PrimaryKey val id: String,
    val name: String,                         // "Food", "Transport", "Friends", "EMI"
    val icon: String,                         // Material icon name as string
    val color: Int,                           // ARGB
    val type: String,                         // "EXPENSE" | "INCOME" | "TRANSFER"
    val isSystem: Boolean = false,            // System groups can't be deleted
    val sortOrder: Int = 0,
    val monthlyBudget: Double? = null         // optional budget cap
)
```

### 3.4 Entity: `SmsLogEntity`
> Deduplication log for SMS-sourced transactions.

```kotlin
@Entity(tableName = "sms_log")
data class SmsLogEntity(
    @PrimaryKey val smsId: Long,
    val processedAt: Long,
    val transactionId: String?
)
```

### 3.5 Entity: `AccessibilityLogEntity`
> Rolling window dedup store — auto-purge after 24h.

```kotlin
@Entity(tableName = "accessibility_log")
data class AccessibilityLogEntity(
    @PrimaryKey val fingerprint: String,      // hash(amount+timestamp_minute+sourceApp)
    val capturedAt: Long,
    val transactionId: String?
)
```

### 3.6 DAOs

```kotlin
// TransactionDao
@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE groupId = :groupId ORDER BY timestamp DESC")
    fun observeByGroup(groupId: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE entityId = :entityId ORDER BY timestamp DESC")
    fun observeByEntity(entityId: String): Flow<List<TransactionEntity>>

    @Query("""
        SELECT * FROM transactions 
        WHERE timestamp BETWEEN :from AND :to 
        ORDER BY timestamp DESC
    """)
    fun observeInRange(from: Long, to: Long): Flow<List<TransactionEntity>>

    @Query("SELECT SUM(amount) FROM transactions WHERE direction='DEBIT' AND timestamp BETWEEN :from AND :to")
    fun totalDebitsInRange(from: Long, to: Long): Flow<Double?>

    @Query("SELECT groupId, SUM(amount) as total FROM transactions WHERE direction='DEBIT' AND timestamp BETWEEN :from AND :to GROUP BY groupId")
    fun spendingByGroup(from: Long, to: Long): Flow<List<GroupSpendingTuple>>

    @Upsert
    suspend fun upsert(transaction: TransactionEntity)

    @Query("UPDATE transactions SET groupId = :groupId, entityId = :entityId, isUserClassified = :userClassified WHERE id = :id")
    suspend fun updateClassification(id: String, groupId: String?, entityId: String?, userClassified: Boolean)
}

// TrackedEntityDao
@Dao
interface TrackedEntityDao {
    @Query("SELECT * FROM tracked_entities ORDER BY lastTransactionAt DESC")
    fun observeAll(): Flow<List<TrackedEntityEntity>>

    @Query("SELECT * FROM tracked_entities WHERE id = :id")
    suspend fun getById(id: String): TrackedEntityEntity?

    // Used by matcher — find entity by UPI VPA match
    @Query("SELECT * FROM tracked_entities WHERE upiIds LIKE '%' || :upiId || '%'")
    suspend fun findByUpiId(upiId: String): TrackedEntityEntity?

    // Used by matcher — fuzzy name match via LIKE
    @Query("SELECT * FROM tracked_entities WHERE displayName LIKE '%' || :name || '%' OR aliases LIKE '%' || :name || '%'")
    suspend fun findByName(name: String): List<TrackedEntityEntity>

    @Upsert
    suspend fun upsert(entity: TrackedEntityEntity)

    @Query("UPDATE tracked_entities SET lastTransactionAt = :ts WHERE id = :id")
    suspend fun updateLastSeen(id: String, ts: Long)
}
```

### 3.7 Default System Groups (seeded on first launch)

| ID | Name | Icon | Type |
|----|------|------|------|
| sys_food | Food & Dining | restaurant | EXPENSE |
| sys_transport | Transport | directions_car | EXPENSE |
| sys_shopping | Shopping | shopping_bag | EXPENSE |
| sys_entertainment | Entertainment | movie | EXPENSE |
| sys_health | Health | medical_services | EXPENSE |
| sys_utilities | Bills & Utilities | receipt | EXPENSE |
| sys_education | Education | school | EXPENSE |
| sys_friends | Friends & Family | people | EXPENSE |
| sys_income | Income | payments | INCOME |
| sys_transfer | Self Transfer | swap_horiz | TRANSFER |
| sys_uncategorized | Uncategorized | help_outline | EXPENSE |

---

## 4. App Module Structure

```
app/
├── src/main/
│   ├── AndroidManifest.xml
│   ├── kotlin/com/paylens/
│   │   ├── PayLensApp.kt                    ← Application class, Koin init
│   │   │
│   │   ├── data/
│   │   │   ├── db/
│   │   │   │   ├── PayLensDatabase.kt
│   │   │   │   ├── entities/                ← All @Entity classes
│   │   │   │   ├── dao/                     ← All @Dao interfaces
│   │   │   │   └── converters/              ← TypeConverters
│   │   │   ├── repository/
│   │   │   │   ├── TransactionRepositoryImpl.kt
│   │   │   │   ├── EntityRepositoryImpl.kt
│   │   │   │   └── GroupRepositoryImpl.kt
│   │   │   ├── detection/
│   │   │   │   ├── accessibility/
│   │   │   │   │   ├── PayLensAccessibilityService.kt
│   │   │   │   │   ├── NodeTreeParser.kt
│   │   │   │   │   └── UpiPatternMatcher.kt
│   │   │   │   ├── notification/
│   │   │   │   │   └── PayLensNotificationListener.kt
│   │   │   │   └── sms/
│   │   │   │       ├── SmsReceiver.kt
│   │   │   │       └── SmsParser.kt
│   │   │   ├── intelligence/
│   │   │   │   ├── TransactionClassifier.kt
│   │   │   │   ├── EntityMatcher.kt
│   │   │   │   └── DeduplicationEngine.kt
│   │   │   └── datastore/
│   │   │       └── AppPreferencesDataStore.kt
│   │   │
│   │   ├── domain/
│   │   │   ├── model/                       ← Domain models (separate from entities)
│   │   │   │   ├── Transaction.kt
│   │   │   │   ├── TrackedEntity.kt
│   │   │   │   └── Group.kt
│   │   │   ├── repository/                  ← Interfaces
│   │   │   │   ├── TransactionRepository.kt
│   │   │   │   ├── EntityRepository.kt
│   │   │   │   └── GroupRepository.kt
│   │   │   └── usecase/
│   │   │       ├── transaction/
│   │   │       │   ├── ObserveTransactionsUseCase.kt
│   │   │       │   ├── LogTransactionUseCase.kt
│   │   │       │   ├── ClassifyTransactionUseCase.kt
│   │   │       │   └── GetSpendingAnalyticsUseCase.kt
│   │   │       ├── entity/
│   │   │       │   ├── ObserveEntitiesUseCase.kt
│   │   │       │   ├── CreateEntityUseCase.kt
│   │   │       │   └── MergeEntitiesUseCase.kt
│   │   │       └── group/
│   │   │           ├── ObserveGroupsUseCase.kt
│   │   │           └── ManageGroupUseCase.kt
│   │   │
│   │   └── presentation/
│   │       ├── MainActivity.kt
│   │       ├── navigation/
│   │       │   ├── NavGraph.kt
│   │       │   └── Routes.kt
│   │       ├── screens/
│   │       │   ├── onboarding/
│   │       │   │   ├── OnboardingScreen.kt
│   │       │   │   └── OnboardingViewModel.kt
│   │       │   ├── home/
│   │       │   │   ├── HomeScreen.kt
│   │       │   │   └── HomeViewModel.kt
│   │       │   ├── transactions/
│   │       │   │   ├── TransactionListScreen.kt
│   │       │   │   ├── TransactionDetailScreen.kt
│   │       │   │   └── TransactionViewModel.kt
│   │       │   ├── entities/
│   │       │   │   ├── EntityListScreen.kt
│   │       │   │   ├── EntityDetailScreen.kt
│   │       │   │   └── EntityViewModel.kt
│   │       │   ├── groups/
│   │       │   │   ├── GroupListScreen.kt
│   │       │   │   ├── GroupDetailScreen.kt
│   │       │   │   └── GroupViewModel.kt
│   │       │   ├── analytics/
│   │       │   │   ├── AnalyticsScreen.kt
│   │       │   │   └── AnalyticsViewModel.kt
│   │       │   └── settings/
│   │       │       ├── SettingsScreen.kt
│   │       │       └── SettingsViewModel.kt
│   │       ├── components/                  ← Reusable Composables
│   │       │   ├── TransactionCard.kt
│   │       │   ├── EntityAvatar.kt
│   │       │   ├── GroupChip.kt
│   │       │   ├── AmountText.kt
│   │       │   ├── SectionHeader.kt
│   │       │   └── EmptyState.kt
│   │       └── theme/
│   │           ├── Theme.kt
│   │           ├── Color.kt
│   │           └── Type.kt
│   │
│   └── res/
│       └── xml/
│           ├── accessibility_service_config.xml
│           └── notification_listener_config.xml      (not needed, use manifest)
│
└── build.gradle.kts
```

---

## 5. Detection Layer

### 5.1 Accessibility Service

**Manifest Declaration:**
```xml
<service
    android:name=".data.detection.accessibility.PayLensAccessibilityService"
    android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE"
    android:exported="true">
    <intent-filter>
        <action android:name="android.accessibilityservice.AccessibilityService"/>
    </intent-filter>
    <meta-data
        android:name="android.accessibilityservice"
        android:resource="@xml/accessibility_service_config"/>
</service>
```

**accessibility_service_config.xml:**
```xml
<accessibility-service
    android:accessibilityEventTypes="typeWindowContentChanged|typeWindowStateChanged"
    android:accessibilityFeedbackType="feedbackGeneric"
    android:accessibilityFlags="flagReportViewIds|flagRetrieveInteractiveWindows"
    android:canRetrieveWindowContent="true"
    android:notificationTimeout="100"
    android:packageNames="com.google.android.apps.nbu.paisa.user,
                          net.one97.paytm,
                          com.phonepe.app,
                          in.org.npci.upiapp,
                          com.amazon.mShop.android.shopping,
                          com.freecharge.android,
                          com.mobikwik_new"/>
```

**PayLensAccessibilityService.kt — Core Logic:**
```kotlin
class PayLensAccessibilityService : AccessibilityService() {

    private val parser by inject<NodeTreeParser>()
    private val logTransaction by inject<LogTransactionUseCase>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val pkg = event.packageName?.toString() ?: return
        if (!WATCHED_PACKAGES.contains(pkg)) return

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                val root = rootInActiveWindow ?: return
                scope.launch {
                    parser.extractTransaction(root, pkg)?.let { raw ->
                        logTransaction(raw)
                    }
                    root.recycle()
                }
            }
        }
    }

    override fun onInterrupt() {}
}
```

**NodeTreeParser.kt — Resilient Tree Traversal:**
```kotlin
class NodeTreeParser(private val dedup: DeduplicationEngine) {

    // Success indicators across apps (keep updated)
    private val successKeywords = setOf(
        "payment successful", "paid", "money sent", "sent successfully",
        "payment done", "transaction successful", "debit", "debited"
    )
    private val failureKeywords = setOf("failed", "declined", "cancelled", "timeout")

    // Amount regex: handles ₹500, ₹1,500.00, Rs. 500
    private val amountRegex = Regex("""[₹Rs.]+\s*(\d{1,3}(?:,\d{3})*(?:\.\d{1,2})?)""")

    // UPI VPA regex
    private val upiVpaRegex = Regex("""[\w.\-+]+@[\w]+""")

    suspend fun extractTransaction(
        root: AccessibilityNodeInfo,
        sourcePackage: String
    ): RawTransactionData? {

        val allText = mutableListOf<String>()
        collectTextNodes(root, allText)

        val fullText = allText.joinToString(" ").lowercase()

        // Only proceed on success screens
        val hasSuccess = successKeywords.any { fullText.contains(it) }
        val hasFailure = failureKeywords.any { fullText.contains(it) }
        if (!hasSuccess || hasFailure) return null

        val amount = extractAmount(allText) ?: return null
        val upiId = extractUpiVpa(allText)
        val contactName = extractContactName(allText, upiId)

        val fingerprint = dedup.fingerprint(amount, sourcePackage)
        if (dedup.isDuplicate(fingerprint)) return null

        return RawTransactionData(
            amount = amount,
            upiId = upiId,
            contactName = contactName,
            rawText = allText.joinToString("|"),
            source = DetectionSource.ACCESSIBILITY,
            sourceApp = sourcePackage,
            direction = TransactionDirection.DEBIT,
            fingerprint = fingerprint,
            timestamp = System.currentTimeMillis()
        )
    }

    private fun collectTextNodes(node: AccessibilityNodeInfo, out: MutableList<String>) {
        node.text?.toString()?.trim()?.takeIf { it.isNotBlank() }?.let { out.add(it) }
        node.contentDescription?.toString()?.trim()?.takeIf { it.isNotBlank() }?.let { out.add(it) }
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                collectTextNodes(child, out)
                child.recycle()
            }
        }
    }

    private fun extractAmount(texts: List<String>): Double? {
        for (text in texts) {
            amountRegex.find(text)?.let { match ->
                return match.groupValues[1].replace(",", "").toDoubleOrNull()
            }
        }
        return null
    }

    private fun extractUpiVpa(texts: List<String>): String? {
        for (text in texts) {
            upiVpaRegex.find(text)?.let { return it.value }
        }
        return null
    }

    private fun extractContactName(texts: List<String>, upiId: String?): String? {
        // "Paid to Prafull Kumar" → "Prafull Kumar"
        val paidToRegex = Regex("""(?:paid to|sent to|to)\s+([A-Z][a-zA-Z\s]+)""", RegexOption.IGNORE_CASE)
        for (text in texts) {
            paidToRegex.find(text)?.let { return it.groupValues[1].trim() }
        }
        // Fallback: extract from UPI VPA prefix
        return upiId?.substringBefore("@")?.replace(Regex("[^a-zA-Z]"), " ")?.trim()
    }
}
```

### 5.2 Notification Listener

```kotlin
class PayLensNotificationListener : NotificationListenerService() {

    private val logTransaction by inject<LogTransactionUseCase>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val pkg = sbn.packageName
        if (!WATCHED_PACKAGES.contains(pkg)) return

        val extras = sbn.notification?.extras ?: return
        val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
        val text = extras.getString(Notification.EXTRA_TEXT) ?: ""
        val bigText = extras.getString(Notification.EXTRA_BIG_TEXT) ?: ""

        val combinedText = "$title $text $bigText"
        scope.launch {
            parseNotificationText(combinedText, pkg)?.let {
                logTransaction(it)
            }
        }
    }

    // Same parsing logic as NodeTreeParser but operating on notification strings
    private fun parseNotificationText(text: String, pkg: String): RawTransactionData? { ... }
}
```

### 5.3 SMS Parser

```kotlin
class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        messages.forEach { sms ->
            if (isBankSms(sms.originatingAddress)) {
                SmsParser.parse(sms)?.let { raw ->
                    // Inject and call LogTransactionUseCase
                }
            }
        }
    }

    private fun isBankSms(address: String?): Boolean {
        val bankSenders = setOf("HDFCBK", "ICICIB", "SBIINB", "PAYTMB", "GPAY", "AXISBK")
        return bankSenders.any { address?.uppercase()?.contains(it) == true }
    }
}
```

**SMS Patterns:**
```kotlin
object SmsParser {
    // HDFC: "Rs.500.00 debited from ac XX1234 on 01-06-25 to VPA prafull@upi"
    // SBI:  "INR 500.00 debited from SB A/C XXXXXX1234. UPI Ref No 123456789"
    // ICICI:"Dear Customer, INR 500.00 has been debited..."

    private val patterns = listOf(
        Regex("""(?:Rs\.|INR|₹)\s*(\d+(?:\.\d{2})?)\s+debited.*?(?:to VPA\s+([\w.@]+))?""", RegexOption.IGNORE_CASE),
        Regex("""debited.*?(?:Rs\.|INR)\s*(\d+(?:\.\d{2})?)""", RegexOption.IGNORE_CASE)
    )

    fun parse(sms: SmsMessage): RawTransactionData? { ... }
}
```

---

## 6. Transaction Intelligence Engine

### 6.1 DeduplicationEngine

```kotlin
class DeduplicationEngine(
    private val accessibilityLogDao: AccessibilityLogDao,
    private val smsLogDao: SmsLogDao
) {
    // Time window: 2 transactions from same app with same amount within 90 seconds = duplicate
    private val DEDUP_WINDOW_MS = 90_000L

    fun fingerprint(amount: Double, sourceApp: String): String {
        val windowedTimestamp = System.currentTimeMillis() / DEDUP_WINDOW_MS
        return "$amount|$sourceApp|$windowedTimestamp".sha256()
    }

    suspend fun isDuplicate(fingerprint: String): Boolean {
        return accessibilityLogDao.existsByFingerprint(fingerprint)
    }

    suspend fun markProcessed(fingerprint: String, transactionId: String) {
        accessibilityLogDao.insert(AccessibilityLogEntity(fingerprint, System.currentTimeMillis(), transactionId))
    }

    // Nightly cleanup job (via WorkManager)
    suspend fun purgeOldEntries() {
        val cutoff = System.currentTimeMillis() - 24 * 60 * 60 * 1000L
        accessibilityLogDao.deleteOlderThan(cutoff)
    }
}
```

### 6.2 EntityMatcher

```kotlin
class EntityMatcher(private val entityDao: TrackedEntityDao) {

    suspend fun findBestMatch(raw: RawTransactionData): TrackedEntityEntity? {
        // Strategy 1: Exact UPI VPA match (highest confidence)
        raw.upiId?.let { vpa ->
            entityDao.findByUpiId(vpa)?.let { return it }
        }

        // Strategy 2: Name similarity (Levenshtein distance ≤ 2)
        raw.contactName?.let { name ->
            val candidates = entityDao.findByName(name)
            if (candidates.isNotEmpty()) return candidates.first()

            // Fuzzy: check each word token of contactName
            name.split(" ").forEach { token ->
                if (token.length > 2) {
                    entityDao.findByName(token).firstOrNull()?.let { return it }
                }
            }
        }

        return null
    }
}
```

### 6.3 TransactionClassifier

```kotlin
class TransactionClassifier(
    private val entityMatcher: EntityMatcher,
    private val groupDao: GroupDao
) {
    suspend fun classify(raw: RawTransactionData): ClassificationResult {
        val entity = entityMatcher.findBestMatch(raw)

        // If entity found AND has a defaultGroupId → use it
        val groupId = entity?.defaultGroupId
            ?: inferGroupFromText(raw.rawText)
            ?: "sys_uncategorized"

        return ClassificationResult(
            entityId = entity?.id,
            groupId = groupId,
            confidence = if (entity != null) Confidence.HIGH else Confidence.LOW
        )
    }

    // Simple keyword-based group inference when no entity match
    private fun inferGroupFromText(text: String): String? {
        val lower = text.lowercase()
        return when {
            swiggyZomatoKeywords.any { lower.contains(it) } -> "sys_food"
            transportKeywords.any { lower.contains(it) } -> "sys_transport"
            entertainmentKeywords.any { lower.contains(it) } -> "sys_entertainment"
            utilityKeywords.any { lower.contains(it) } -> "sys_utilities"
            else -> null
        }
    }

    private val swiggyZomatoKeywords = setOf("swiggy", "zomato", "blinkit", "restaurant", "cafe")
    private val transportKeywords = setOf("uber", "ola", "rapido", "metro", "irctc", "fuel")
    private val entertainmentKeywords = setOf("netflix", "spotify", "bookmyshow", "prime")
    private val utilityKeywords = setOf("electricity", "jio", "airtel", "broadband", "gas")
}
```

### 6.4 LogTransactionUseCase (Orchestrator)

```kotlin
class LogTransactionUseCase(
    private val dedup: DeduplicationEngine,
    private val classifier: TransactionClassifier,
    private val transactionRepo: TransactionRepository,
    private val entityRepo: EntityRepository
) {
    suspend operator fun invoke(raw: RawTransactionData) {
        // 1. Deduplication check
        if (dedup.isDuplicate(raw.fingerprint)) return

        // 2. Classify
        val classification = classifier.classify(raw)

        // 3. Build entity
        val transaction = Transaction(
            id = UUID.randomUUID().toString(),
            amount = raw.amount,
            timestamp = raw.timestamp,
            upiId = raw.upiId,
            contactName = raw.contactName,
            rawDescription = raw.rawText,
            source = raw.source,
            sourceApp = raw.sourceApp,
            direction = raw.direction,
            status = TransactionStatus.SUCCESS,
            groupId = classification.groupId,
            entityId = classification.entityId,
            isUserClassified = false
        )

        // 4. Persist
        transactionRepo.save(transaction)

        // 5. Update entity lastTransactionAt
        classification.entityId?.let {
            entityRepo.updateLastSeen(it, raw.timestamp)
        }

        // 6. Mark dedup
        dedup.markProcessed(raw.fingerprint, transaction.id)
    }
}
```

---

## 7. Screen-by-Screen Wireframes & UI Spec

### Screen 1: Onboarding (First Launch Only)

```
┌─────────────────────────────────┐
│                                 │
│         [App Logo]              │
│         PayLens                 │
│   Track every rupee you spend   │
│                                 │
│  ┌─────────────────────────────┐│
│  │ ● Accessibility Service     ││
│  │   Watch UPI payment screens ││
│  │              [Enable →]     ││
│  └─────────────────────────────┘│
│  ┌─────────────────────────────┐│
│  │ ○ Notification Access       ││
│  │   Redundancy layer          ││
│  │              [Enable →]     ││
│  └─────────────────────────────┘│
│  ┌─────────────────────────────┐│
│  │ ○ SMS Permission            ││
│  │   Backup + bank alerts      ││
│  │              [Enable →]     ││
│  └─────────────────────────────┘│
│                                 │
│     [Continue with enabled →]   │
│   (skip note in small text)     │
└─────────────────────────────────┘
```

**State Management:**
- Each permission card shows green check when granted
- "Continue" becomes active when at least Accessibility OR Notification is enabled
- DataStore saves onboarding completion flag

---

### Screen 2: Home Dashboard

```
┌─────────────────────────────────┐
│  PayLens          [🔔] [⚙️]     │
│                                 │
│  ┌─────────────────────────────┐│
│  │  This Month                 ││
│  │  ₹12,450  spent             ││
│  │  ████████████░░░  83% of    ││
│  │  ₹15,000 budget             ││
│  └─────────────────────────────┘│
│                                 │
│  [All] [Week] [Month] [Custom]  │  ← Filter chips
│                                 │
│  RECENT TRANSACTIONS            │
│  ┌─────────────────────────────┐│
│  │ 🟢 Prafull     ₹500   Food  ││
│  │    2 min ago          [•••] ││
│  ├─────────────────────────────┤│
│  │ 🔵 Zomato      ₹340   Food  ││
│  │    Today 1:30PM       [•••] ││
│  ├─────────────────────────────┤│
│  │ 🟣 Uber        ₹180  Trans  ││
│  │    Yesterday          [•••] ││
│  └─────────────────────────────┘│
│         [See all transactions]  │
│                                 │
│  TOP SPENDS THIS MONTH          │
│  Food         ₹4,200  ████░     │
│  Transport    ₹2,100  ██░░░     │
│  Shopping     ₹1,800  █░░░░     │
│                                 │
│ ┌───┐ ┌──────┐ ┌──────┐ ┌────┐ │
│ │🏠 │ │ 💳   │ │  👥  │ │📊  │ │
│ │Home│ │Trans │ │People│ │Stats│ │
└─────────────────────────────────┘
```

**HomeViewModel State:**
```kotlin
data class HomeUiState(
    val monthlyTotal: Double = 0.0,
    val monthlyBudget: Double? = null,
    val recentTransactions: List<Transaction> = emptyList(),
    val topSpendingGroups: List<GroupSpending> = emptyList(),
    val selectedFilter: DateFilter = DateFilter.MONTH,
    val isLoading: Boolean = false
)
```

---

### Screen 3: Transaction List

```
┌─────────────────────────────────┐
│  ← Transactions    [🔍] [⚙️]   │
│                                 │
│  [🔍 Search transactions...]    │
│  [All ▼] [Group ▼] [Person ▼]  │
│                                 │
│  TODAY · ₹840                   │
│  ┌─────────────────────────────┐│
│  │ [P] Prafull    ₹500  13:24 ││
│  │     Friends • GPay    [→]  ││
│  ├─────────────────────────────┤│
│  │ [Z] Zomato     ₹340  12:10 ││
│  │     Food • PhonePe    [→]  ││
│  └─────────────────────────────┘│
│                                 │
│  YESTERDAY · ₹520               │
│  ┌─────────────────────────────┐│
│  │ [U] Uber       ₹180  18:45 ││
│  │     Transport • GPay  [→]  ││
│  ├─────────────────────────────┤│
│  │ [N] Netflix    ₹340  10:00 ││
│  │     Entertainment     [→]  ││
│  └─────────────────────────────┘│
│                                 │
│        ⚠️ 3 unclassified        │ ← yellow banner
│        [Classify now →]         │
└─────────────────────────────────┘
```

**Key behaviors:**
- Date-sectioned list using `LazyColumn` with `stickyHeader`
- Unclassified banner when `groupId == sys_uncategorized && !isUserClassified`
- Long press → quick actions (Edit group, Add note, Delete)
- Pull to refresh triggers SMS re-scan for last 24h

---

### Screen 4: Transaction Detail

```
┌─────────────────────────────────┐
│  ← Transaction Detail           │
│                                 │
│         ₹500.00                 │  ← Large amount
│        DEBITED                  │  ← direction chip
│     Jun 1, 2025 · 1:24 PM      │
│                                 │
│  ┌─────────────────────────────┐│
│  │  To                         ││
│  │  [P] Prafull Kumar          ││
│  │  prafull@okaxis             ││
│  │              [View Profile] ││
│  └─────────────────────────────┘│
│                                 │
│  ┌─────────────────────────────┐│
│  │  Category                   ││
│  │  [👥 Friends & Family  ▼]   ││  ← Tappable dropdown
│  └─────────────────────────────┘│
│                                 │
│  ┌─────────────────────────────┐│
│  │  Note (tap to add)          ││
│  │  "Lunch split"              ││
│  └─────────────────────────────┘│
│                                 │
│  ──── Detection Info ────       │
│  Source: Google Pay (Screen)    │
│  Confidence: High               │
│  UPI Ref: N/A (screen capture) │
│                                 │
│          [Save Changes]         │
│     [Delete Transaction 🗑️]    │
└─────────────────────────────────┘
```

**Interactions:**
- Category dropdown opens a BottomSheet with all groups + search
- Changing category sets `isUserClassified = true`
- "To" section shows entity card if matched; shows "Unknown Person" + [+ Create Contact] if not

---

### Screen 5: People / Entities Screen

```
┌─────────────────────────────────┐
│  People & Merchants    [+ Add]  │
│                                 │
│  [🔍 Search...]                 │
│  [All] [People] [Merchants]     │
│                                 │
│  PEOPLE                         │
│  ┌─────────────────────────────┐│
│  │ [P] Prafull Kumar           ││
│  │     ₹2,400 total · 8 txns  ││
│  │     Last: 2 min ago   [→]   ││
│  ├─────────────────────────────┤│
│  │ [A] Amit Sharma             ││
│  │     ₹800 total · 3 txns    ││
│  │     Last: Yesterday   [→]   ││
│  └─────────────────────────────┘│
│                                 │
│  MERCHANTS                      │
│  ┌─────────────────────────────┐│
│  │ [Z] Zomato                  ││
│  │     ₹3,200 total · 12 txns ││
│  │     Food                    ││
│  ├─────────────────────────────┤│
│  │ [U] Uber                    ││
│  │     ₹1,400 total · 7 txns  ││
│  │     Transport         [→]   ││
│  └─────────────────────────────┘│
└─────────────────────────────────┘
```

---

### Screen 6: Entity Detail

```
┌─────────────────────────────────┐
│  ← Prafull Kumar         [✏️]  │
│                                 │
│         [P]                     │  ← colored avatar
│      Prafull Kumar              │
│      PERSON                     │
│                                 │
│  UPI IDs:                       │
│  prafull@okaxis  [+ Add UPI]    │
│                                 │
│  Default Group: [Friends ▼]     │
│  (all future payments auto-     │
│   assigned to this group)       │
│                                 │
│  ──── Summary ────              │
│  Total Sent     ₹2,400          │
│  Transactions   8               │
│  Avg per txn    ₹300            │
│                                 │
│  ──── Transactions ────         │
│  [Transaction list filtered     │
│   by this entity]               │
│                                 │
│  ─── Merge Entities ───        │
│  [🔗 Merge with another person] │
└─────────────────────────────────┘
```

**Merge Flow:**
- Tap "Merge" → search/select another entity → confirm
- All transactions of merged entity get reassigned to primary
- Merged entity's UPI IDs and aliases added to primary

---

### Screen 7: Groups Screen

```
┌─────────────────────────────────┐
│  Groups              [+ New]   │
│                                 │
│  THIS MONTH                     │
│  ┌─────────────────────────────┐│
│  │ 🍽️ Food & Dining    ₹4,200 ││
│  │   ████████████░░  Budget:  ││
│  │   ₹5,000                   ││
│  ├─────────────────────────────┤│
│  │ 🚗 Transport        ₹2,100 ││
│  │   ██████░░░░░       [→]    ││
│  ├─────────────────────────────┤│
│  │ 🛍️ Shopping         ₹1,800 ││
│  ├─────────────────────────────┤│
│  │ 🎬 Entertainment    ₹1,400 ││
│  ├─────────────────────────────┤│
│  │ 👥 Friends & Family ₹2,400 ││
│  └─────────────────────────────┘│
│                                 │
│  ❓ Uncategorized (3)    ₹720  │  ← orange badge
│  [Classify transactions →]      │
└─────────────────────────────────┘
```

---

### Screen 8: Group Detail

```
┌─────────────────────────────────┐
│  ← Food & Dining          [✏️] │
│                                 │
│  ┌──── Budget ────┐             │
│  │ ₹4,200 / ₹5,000             │
│  │ ████████████░░░  84%         │
│  │ ₹800 remaining               │
│  │         [Edit Budget]        │
│  └──────────────────┘           │
│                                 │
│  ── Monthly Trend ──            │
│  [Bar chart: last 6 months]     │
│  Jan Feb Mar Apr May Jun        │
│  ▄   ██  ▄█  ██  ▄▄  ██       │
│                                 │
│  ── Top People ──               │
│  Zomato   ₹2,400  57%          │
│  Swiggy   ₹1,200  29%          │
│  Other    ₹600    14%          │
│                                 │
│  ── Transactions ──             │
│  [Filtered transaction list]    │
└─────────────────────────────────┘
```

---

### Screen 9: Analytics Screen

```
┌─────────────────────────────────┐
│  Analytics                      │
│                                 │
│  [Jun 2025 ◀ ▶]                │
│                                 │
│  TOTAL SPENT                    │
│  ₹12,450                        │
│  ↑ 12% vs last month            │
│                                 │
│  ── Spending Breakdown ──       │
│  [Donut chart]                  │
│   🍽️ Food      34%             │
│   🚗 Transport 17%              │
│   👥 People    19%              │
│   🛍️ Shopping  15%             │
│   Others       15%              │
│                                 │
│  ── Daily Trend ──              │
│  [Line chart: daily spend]      │
│                                 │
│  ── Insights ──                 │
│  💡 You spent 34% more on food  │
│     this month than April       │
│  💡 Prafull is your top payee   │
│     this month (₹800)           │
│                                 │
│  ── Biggest Transactions ──     │
│  [Top 5 transactions this month]│
└─────────────────────────────────┘
```

---

### Screen 10: Settings

```
┌─────────────────────────────────┐
│  ← Settings                    │
│                                 │
│  ── Detection ──                │
│  Accessibility Service  [✅ ON] │
│  Notification Listener  [✅ ON] │
│  SMS Permission         [✅ ON] │
│                                 │
│  ── Preferences ──              │
│  Monthly Budget    ₹15,000 [>] │
│  Currency          INR ₹    [>] │
│  Week starts on    Monday   [>] │
│                                 │
│  ── Data ──                     │
│  Export to CSV              [>] │
│  Export to JSON             [>] │
│  Clear all data             [>] │
│                                 │
│  ── About ──                    │
│  Version 1.0.0                  │
│  Privacy Policy             [>] │
│  How detection works        [>] │
└─────────────────────────────────┘
```

---

### Screen 11: Add/Edit Entity (Bottom Sheet)

```
┌─────────────────────────────────┐
│  New Contact            [✕]    │
│                                 │
│  Name *                         │
│  [___________________________]  │
│                                 │
│  Type                           │
│  [Person] [Merchant] [Service]  │
│                                 │
│  UPI IDs (optional)             │
│  [____________] [+ Add]         │
│                                 │
│  Phone Numbers (optional)       │
│  [____________] [+ Add]         │
│                                 │
│  Aliases (optional)             │
│  [Prafull bhai] [+ Add]         │
│                                 │
│  Default Category               │
│  [Friends & Family ▼]           │
│                                 │
│         [Save Contact]          │
└─────────────────────────────────┘
```

---

### Screen 12: Classify Unclassified (Batch Mode)

> Triggered from "Unclassified" banner

```
┌─────────────────────────────────┐
│  Classify Transactions  (3)    │
│  Help PayLens learn your habits │
│                                 │
│  ┌─────────────────────────────┐│
│  │  ₹200 · Unknown Person      ││
│  │  Jun 1 · GPay               ││
│  │  "Paid to 9876543210@upi"   ││
│  │                             ││
│  │  Who is this?               ││
│  │  [Search contacts...]       ││
│  │  [+ Create new contact]     ││
│  │                             ││
│  │  Category:                  ││
│  │  [Select group ▼]           ││
│  │                             ││
│  │     [Skip] [Save →]         ││
│  └─────────────────────────────┘│
│                                 │
│  1 of 3                         │
└─────────────────────────────────┘
```

---

## 8. Navigation Architecture

```kotlin
// Routes.kt
sealed class Route(val path: String) {
    object Onboarding : Route("onboarding")
    object Home : Route("home")
    object Transactions : Route("transactions")
    object TransactionDetail : Route("transaction/{id}") {
        fun create(id: String) = "transaction/$id"
    }
    object Entities : Route("entities")
    object EntityDetail : Route("entity/{id}") {
        fun create(id: String) = "entity/$id"
    }
    object Groups : Route("groups")
    object GroupDetail : Route("group/{id}") {
        fun create(id: String) = "group/$id"
    }
    object Analytics : Route("analytics")
    object Settings : Route("settings")
    object Classify : Route("classify")
    object AddEntity : Route("add_entity?transactionId={transactionId}")
}

// NavGraph.kt — bottom nav items
val bottomNavItems = listOf(
    BottomNavItem(Route.Home, Icons.Home, "Home"),
    BottomNavItem(Route.Transactions, Icons.CreditCard, "Transactions"),
    BottomNavItem(Route.Entities, Icons.Group, "People"),
    BottomNavItem(Route.Analytics, Icons.BarChart, "Analytics"),
)
```

**Navigation Flow:**
```
Onboarding → Home (on permission granted)

Home:
  → TransactionDetail (tap transaction)
  → Transactions (see all)
  → Classify (unclassified banner)

Transactions:
  → TransactionDetail (tap)
  → Classify (batch classify)
  → EntityDetail (tap entity name)

TransactionDetail:
  → EntityDetail (tap entity)
  → AddEntity (create new entity)
  → Group selection (bottom sheet, stays in place)

Entities:
  → EntityDetail (tap)
  → AddEntity (FAB)

EntityDetail:
  → Transactions (filtered)
  → Merge flow

Groups:
  → GroupDetail (tap)
  → AddGroup (FAB)

GroupDetail:
  → Transactions (filtered)
```

---

## 9. Koin Dependency Injection Map

```kotlin
// AppModule.kt
val appModule = module {

    // Database
    single {
        Room.databaseBuilder(androidContext(), PayLensDatabase::class.java, "paylens.db")
            .addMigrations(*Migrations.ALL)
            .build()
    }
    single { get<PayLensDatabase>().transactionDao() }
    single { get<PayLensDatabase>().trackedEntityDao() }
    single { get<PayLensDatabase>().groupDao() }
    single { get<PayLensDatabase>().accessibilityLogDao() }
    single { get<PayLensDatabase>().smsLogDao() }

    // DataStore
    single { AppPreferencesDataStore(androidContext()) }
}

val dataModule = module {
    // Repositories
    single<TransactionRepository> { TransactionRepositoryImpl(get()) }
    single<EntityRepository> { EntityRepositoryImpl(get()) }
    single<GroupRepository> { GroupRepositoryImpl(get()) }

    // Intelligence
    single { DeduplicationEngine(get(), get()) }
    single { EntityMatcher(get()) }
    single { TransactionClassifier(get(), get()) }
    single { NodeTreeParser(get()) }
    single { SmsParser() }
}

val domainModule = module {
    factory { LogTransactionUseCase(get(), get(), get(), get()) }
    factory { ObserveTransactionsUseCase(get()) }
    factory { ClassifyTransactionUseCase(get(), get()) }
    factory { GetSpendingAnalyticsUseCase(get()) }
    factory { ObserveEntitiesUseCase(get()) }
    factory { CreateEntityUseCase(get()) }
    factory { MergeEntitiesUseCase(get()) }
    factory { ObserveGroupsUseCase(get()) }
    factory { ManageGroupUseCase(get()) }
}

val presentationModule = module {
    viewModel { HomeViewModel(get(), get(), get()) }
    viewModel { TransactionViewModel(get(), get()) }
    viewModel { (transactionId: String) -> TransactionDetailViewModel(transactionId, get(), get(), get()) }
    viewModel { EntityViewModel(get(), get()) }
    viewModel { (entityId: String) -> EntityDetailViewModel(entityId, get(), get()) }
    viewModel { GroupViewModel(get()) }
    viewModel { (groupId: String) -> GroupDetailViewModel(groupId, get(), get()) }
    viewModel { AnalyticsViewModel(get()) }
    viewModel { SettingsViewModel(get()) }
    viewModel { ClassifyViewModel(get(), get(), get(), get()) }
}
```

---

## 10. Implementation Phases for AI Agent

### PHASE 0 — Project Scaffolding
**Goal:** Compilable, runnable empty app with full structure

1. Create new Android project with Kotlin + Compose
2. Set up `libs.versions.toml` with all dependencies:
   - `compose-bom`, `compose-navigation`, `compose-material3`
   - `koin-android`, `koin-compose`, `koin-annotations`, `koin-ksp`
   - `room`, `room-ktx`, `room-compiler` (KSP)
   - `kotlinx-coroutines-android`
   - `kotlinx-datetime`
   - `datastore-proto`
   - `vico-compose`
   - `accompanist-permissions`
3. Create all package directories as listed in module structure
4. Set up `PayLensApp.kt` with Koin init (empty modules for now)
5. Create `MainActivity.kt` with basic NavHost
6. Create `Theme.kt`, `Color.kt`, `Type.kt` with Material 3 tokens
7. **Verify:** App launches on emulator showing blank home screen

---

### PHASE 1 — Database Layer
**Goal:** All Room entities and DAOs working with sample data

1. Create all `@Entity` data classes (TransactionEntity, TrackedEntityEntity, GroupEntity, SmsLogEntity, AccessibilityLogEntity)
2. Create `TypeConverters` for any complex fields
3. Create all `@Dao` interfaces with every query needed
4. Create `PayLensDatabase` with all entities registered + KSP processor
5. Create `DatabaseSeeder` — inserts default system groups on first launch
6. Write `appModule` Koin module with Room setup
7. Write unit tests for each DAO using in-memory Room db
8. **Verify:** DAOs insert/query correctly in tests

---

### PHASE 2 — Domain Models & Repository Layer
**Goal:** Clean domain layer fully decoupled from Room

1. Create domain models: `Transaction`, `TrackedEntity`, `Group` (mirror entities but without Room annotations)
2. Create repository interfaces: `TransactionRepository`, `EntityRepository`, `GroupRepository`
3. Create `Mappers` — extension functions to convert Entity ↔ Domain model
4. Implement `TransactionRepositoryImpl`, `EntityRepositoryImpl`, `GroupRepositoryImpl`
5. Wire up in `dataModule` Koin
6. **Verify:** Repository emits Flow of domain models correctly

---

### PHASE 3 — Detection Layer
**Goal:** Accessibility Service captures and logs UPI transactions

1. Create `RawTransactionData` data class (intermediate representation before DB)
2. Create `DeduplicationEngine` with fingerprint + isDuplicate logic
3. Create `UpiPatternMatcher` — all regex patterns for amounts, VPAs, names
4. Create `NodeTreeParser` — recursive tree traversal using UpiPatternMatcher
5. Create `PayLensAccessibilityService` — wire parser + LogTransactionUseCase
6. Add accessibility service to `AndroidManifest.xml`
7. Create `accessibility_service_config.xml` with all UPI package names
8. Create `PayLensNotificationListener` — notification text parsing
9. Add notification listener to manifest
10. Create `SmsParser` + `SmsReceiver` — bank SMS patterns
11. Add SMS_RECEIVE permission and receiver to manifest
12. Create `LogTransactionUseCase` (stub for now, just saves raw data)
13. **Verify:** Make a GPay payment on test device, confirm transaction appears in Room Inspector

---

### PHASE 4 — Intelligence Engine
**Goal:** Transactions get auto-classified when logged

1. Create `EntityMatcher` with UPI VPA exact match + name fuzzy match strategies
2. Create `TransactionClassifier` with keyword-based group inference
3. Wire `EntityMatcher` + `TransactionClassifier` into `LogTransactionUseCase`
4. Create `ClassifyTransactionUseCase` for manual reclassification
5. Wire all in `dataModule` + `domainModule` Koin
6. Create all remaining UseCases:
   - `ObserveTransactionsUseCase`
   - `GetSpendingAnalyticsUseCase` (stub)
   - `ObserveEntitiesUseCase`
   - `CreateEntityUseCase`
   - `MergeEntitiesUseCase`
   - `ObserveGroupsUseCase`
   - `ManageGroupUseCase`
7. **Verify:** Payment to "Zomato" UPI auto-classifies to Food group

---

### PHASE 5 — Core UI Screens
**Goal:** Home, Transactions, and Transaction Detail screens functional

1. Create reusable components:
   - `TransactionCard` composable
   - `EntityAvatar` composable (colored circle with initials)
   - `GroupChip` composable
   - `AmountText` composable (red for debit, green for credit)
   - `SectionHeader` composable
   - `EmptyState` composable
   - `BottomNavBar` composable

2. Create `HomeViewModel` + `HomeScreen`:
   - Show monthly total
   - Show recent 5 transactions
   - Show top spending groups
   - Date filter chips (All / Week / Month)

3. Create `TransactionViewModel` + `TransactionListScreen`:
   - Date-sectioned lazy column
   - Search bar
   - Group/Person filter chips
   - Unclassified banner

4. Create `TransactionDetailViewModel` + `TransactionDetailScreen`:
   - Show all transaction fields
   - Editable group (bottom sheet group picker)
   - Editable note
   - Link to entity

5. Wire navigation: Home ↔ Transactions ↔ TransactionDetail
6. **Verify:** Can view and edit transactions via UI

---

### PHASE 6 — Entities & Groups Screens
**Goal:** Full people and merchant management

1. Create `EntityViewModel` + `EntityListScreen`:
   - People/Merchant tabs
   - Search
   - Total spend per entity

2. Create `EntityDetailViewModel` + `EntityDetailScreen`:
   - UPI ID management
   - Default group assignment
   - Transaction history filtered to entity
   - Merge entity flow

3. Create `AddEditEntitySheet` (Bottom Sheet):
   - Name, type, UPI IDs, phone numbers, aliases, default group

4. Create `GroupViewModel` + `GroupListScreen`:
   - All groups with monthly totals
   - Budget progress bars

5. Create `GroupDetailViewModel` + `GroupDetailScreen`:
   - Budget editing
   - Monthly trend bar chart (Vico)
   - Top entities in group
   - Filtered transactions

6. Create `AddEditGroupDialog`
7. Wire all navigation
8. **Verify:** Can create entity, assign UPI, all future payments auto-classify

---

### PHASE 7 — Analytics Screen
**Goal:** Full spending analytics with charts

1. Create `GetSpendingAnalyticsUseCase` (full implementation):
   - Monthly totals
   - Group breakdown with percentages
   - Daily trend data
   - Month-over-month comparison
   - Insight generation (top payee, biggest increase, etc.)

2. Create `AnalyticsViewModel` + `AnalyticsScreen`:
   - Month picker
   - Donut chart (Vico) for group breakdown
   - Line chart for daily trend
   - Insight cards
   - Top transactions list

3. **Verify:** Charts render correctly with real data

---

### PHASE 8 — Onboarding & Settings
**Goal:** Complete user journey from install to first transaction

1. Create `OnboardingViewModel` + `OnboardingScreen`:
   - Permission check cards (Accessibility, Notification, SMS)
   - Deep link to system settings for each
   - Continuous permission polling while screen is open
   - DataStore flag for onboarding completion

2. Create `SettingsViewModel` + `SettingsScreen`:
   - Detection status toggles (deep link to system settings)
   - Monthly budget setting
   - Export CSV/JSON
   - Clear data (confirmation dialog)
   - "How it works" explainer screen

3. Add start destination logic: Onboarding if first launch, Home otherwise
4. **Verify:** Fresh install → onboarding → grant permissions → home screen

---

### PHASE 9 — Batch Classify Flow
**Goal:** Users can quickly classify all unclassified transactions

1. Create `ClassifyViewModel` + `ClassifyScreen`:
   - Card-by-card UX for unclassified transactions
   - Contact search/create inline
   - Group selection
   - Skip support

2. Wire unclassified count badge on Home + Groups screen
3. **Verify:** Can classify 5 unknown transactions in one flow

---

### PHASE 10 — Polish & Edge Cases
**Goal:** Production-ready quality

1. Add WorkManager job for:
   - Nightly dedup cache purge
   - Nightly SMS re-scan for past 24h (catches missed transactions)

2. Add Foreground Service wrapper for Accessibility (optional — shows persistent notification so Android doesn't kill it)

3. Implement proper conflict resolution:
   - If SMS and Accessibility both log same transaction → merge, prefer SMS for UPI ref

4. Add `ReconciliationWorker` — weekly job that re-runs classification on isUserClassified=false transactions using latest entity/group rules

5. Add export functionality (CSV + JSON)

6. Add empty states for all screens

7. Add proper error states (Room errors, service not enabled warnings)

8. Performance: use `key {}` in all LazyColumn items, add `@Stable` annotations to domain models

9. Accessibility: content descriptions on all icon-only buttons, proper semantics

10. Final testing on multiple devices + Android versions (8.0+)

---

## 11. Edge Cases & Resilience Patterns

### Detection Edge Cases

| Scenario | Handling |
|----------|----------|
| GPay redesigns success screen | NodeTreeParser extracts ALL text and runs pattern matching — layout-agnostic |
| Same payment opened multiple times | Dedup fingerprint: amount + app + 90-second window |
| Payment in background | Notification Listener catches it; SMS as final fallback |
| Multi-language UI (Hindi GPay) | Amount regex handles ₹ symbol + digits regardless of surrounding language |
| Partial text render during animation | TYPE_WINDOW_CONTENT_CHANGED fires multiple times; dedup prevents duplicate logging |
| Two payments of same amount within 90s | Accept as two transactions; fingerprint includes minute bucket — edge case user can manually delete |
| SMS arrives after accessibility captured it | SmsLogEntity prevents double logging; SMS adds UPI ref number if accessibility missed it |
| App force-stopped | WorkManager restarts reconciliation; SMS BroadcastReceiver still fires |
| UPI credit (received money) | Direction detection: "credited", "received", "added to wallet" keywords → CREDIT direction |

### Classification Edge Cases

| Scenario | Handling |
|----------|----------|
| "Prafull" paid via different UPI ID | EntityMatcher tries name match after VPA fails |
| New UPI ID for existing entity | User adds UPI ID on EntityDetail → all future payments auto-match |
| Merchant with changing VPA (Zomato has multiple) | Add all known VPAs to entity's `upiIds` JSON array |
| Ambiguous name match (two contacts named "Rahul") | Return both candidates → prompt user to disambiguate → save choice |
| Contact changes their UPI ID | Old transactions keep old entityId; new transactions get matched by new VPA added to same entity |

---

## 12. Analytics Engine Spec

### Data Structures

```kotlin
data class MonthlyAnalytics(
    val year: Int,
    val month: Int,
    val totalDebit: Double,
    val totalCredit: Double,
    val transactionCount: Int,
    val groupBreakdown: List<GroupSpending>,     // sorted by amount desc
    val dailyTrend: List<DailySpend>,            // 1..31 days
    val topEntities: List<EntitySpending>,
    val insights: List<Insight>,
    val prevMonthTotal: Double?                  // for MoM comparison
)

data class GroupSpending(
    val group: Group,
    val amount: Double,
    val percentage: Double,
    val transactionCount: Int
)

data class DailySpend(val day: Int, val amount: Double)

data class Insight(
    val type: InsightType,
    val message: String,
    val data: Map<String, Any>
)

enum class InsightType {
    TOP_PAYEE, BIGGEST_INCREASE, BIGGEST_DECREASE,
    BUDGET_WARNING, BUDGET_EXCEEDED, STREAK_DAYS_ACTIVE
}
```

### Insight Generation Rules

```kotlin
object InsightGenerator {
    fun generate(current: MonthlyAnalytics, previous: MonthlyAnalytics?): List<Insight> {
        val insights = mutableListOf<Insight>()

        // Top payee this month
        current.topEntities.firstOrNull()?.let {
            insights += Insight(InsightType.TOP_PAYEE,
                "${it.entity.displayName} is your top payee (₹${it.amount.formatted()})", ...)
        }

        // MoM comparison per group
        if (previous != null) {
            current.groupBreakdown.forEach { curr ->
                val prev = previous.groupBreakdown.find { it.group.id == curr.group.id }
                if (prev != null && prev.amount > 0) {
                    val change = (curr.amount - prev.amount) / prev.amount * 100
                    if (change > 30) insights += Insight(InsightType.BIGGEST_INCREASE,
                        "You spent ${change.toInt()}% more on ${curr.group.name} this month", ...)
                }
            }
        }

        // Budget warnings
        current.groupBreakdown.forEach { spending ->
            spending.group.monthlyBudget?.let { budget ->
                val pct = spending.amount / budget
                when {
                    pct >= 1.0 -> insights += Insight(InsightType.BUDGET_EXCEEDED, ...)
                    pct >= 0.85 -> insights += Insight(InsightType.BUDGET_WARNING, ...)
                }
            }
        }

        return insights.take(5) // max 5 insights per month
    }
}
```

---

## Appendix: Manifest Summary

```xml
<!-- Permissions -->
<uses-permission android:name="android.permission.RECEIVE_SMS"/>
<uses-permission android:name="android.permission.READ_SMS"/>
<uses-permission android:name="android.permission.FOREGROUND_SERVICE"/>
<uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>

<!-- Services -->
<service android:name=".data.detection.accessibility.PayLensAccessibilityService"
    android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE" android:exported="true">
    <intent-filter><action android:name="android.accessibilityservice.AccessibilityService"/></intent-filter>
    <meta-data android:name="android.accessibilityservice" android:resource="@xml/accessibility_service_config"/>
</service>

<service android:name=".data.detection.notification.PayLensNotificationListener"
    android:permission="android.permission.BIND_NOTIFICATION_LISTENER_SERVICE" android:exported="true">
    <intent-filter><action android:name="android.service.notification.NotificationListenerService"/></intent-filter>
</service>

<!-- Receivers -->
<receiver android:name=".data.detection.sms.SmsReceiver" android:exported="true">
    <intent-filter android:priority="999">
        <action android:name="android.provider.Telephony.SMS_RECEIVED"/>
    </intent-filter>
</receiver>
```

---

## Appendix: Key Design Decisions Summary

| Decision | Choice | Reason |
|----------|--------|--------|
| Transaction ↔ Entity relationship | Optional FK, not enforced | Entity may not exist at log time; classified later |
| Group assignment | Soft default on Entity, hard on Transaction | Entity default is a suggestion; Transaction group is the truth |
| User vs system classification | `isUserClassified` flag | User override always wins; system can re-classify only if false |
| Dedup strategy | Time-windowed fingerprint | Prevents double-logging without requiring unique UPI ref (which accessibility doesn't capture) |
| Entity matching | VPA exact → name fuzzy cascade | VPA is cryptographically reliable; name is fallback |
| SMS + Accessibility | Both active, dedup handles conflicts | Redundancy > simplicity for critical data capture |
| No cloud sync | Local-only Room | Privacy-first; financial data never leaves device |
| Chart library | Vico | Only fully Compose-native chart library; no View interop |

---

*End of PayLens implementation plan. This document is designed to be given to an AI coding agent sequentially — each phase builds on the previous and has a clear verification checkpoint before proceeding.*