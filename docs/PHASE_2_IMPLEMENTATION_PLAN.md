# PHASE 2: SAFE IMPLEMENTATION PLAN
**Prepared for:** ZingZing Backend Refactor  
**Strategy:** Minimal-risk, fix-first approach  
**Execution Sequence:** Module-by-module, preserving working functionality  

---

## OVERVIEW

This plan prioritizes:
1. **Fixing existing modules** (not rewriting)
2. **Minimal database changes** (additive migrations)
3. **Backward compatibility** (no breaking changes)
4. **Phased rollout** (module-by-module completion)
5. **Testing gates** (validate each phase before proceeding)

---

## PHASE 2A: SCHEMA CLEANUP & PREPARATION
**Risk Level:** 🟡 MEDIUM (Database changes, but additive)  
**Effort:** 8 hours  
**Blocks:** All subsequent phases

### Step 1: Create V2 Migration (Add Missing Tables)
**File:** `src/main/resources/db/migration/V2__add_missing_tables.sql`

**Tables to add:**
1. `social_accounts` - Creator social media links
2. `content_previews` - Creator portfolio items
3. `package_analytics` - Package performance metrics
4. `transactions` - Earnings/withdrawal history
5. `wallets` - Creator balance tracking
6. `payout_methods` - Creator withdrawal destinations
7. `withdrawal_requests` - Withdrawal requests
8. `ambassador_applications` - Ambassador program applications
9. `ambassador_scores` - Computed ambassador tier scores
10. `saved_creators` - Brand bookmarks
11. `notification_preferences` - User notification settings
12. `quick_deal_offers` - Standalone offers (separate from Message)
13. `profile_views` - Creator profile view tracking

**Execution:**
- Use Flyway-compatible SQL (PostgreSQL syntax)
- Add proper foreign keys with ON DELETE CASCADE/RESTRICT
- Add indexes on frequently queried columns
- Include default values and constraints per spec

**Validation Gate:**
```bash
# After migration runs:
SELECT table_name FROM information_schema.tables 
WHERE table_schema = 'core' ORDER BY table_name;
# Should list all 23 tables
```

### Step 2: Create V3 Migration (Alter Existing Tables)
**File:** `src/main/resources/db/migration/V3__alter_existing_tables.sql`

**Alterations needed for:**
- users table: Add avatar_url, creator_program_status, deleted_at
- creators table: Add 18+ missing fields per spec
- brands table: Add logo_url, monthly_budget
- packages table: Add deal_type, barter_category, visibility fields
- orders table: Add proper deal_type, status fields
- messages table: Add type, is_read, attachment_url fields
- conversations table: Add unread_count_creator, unread_count_brand
- reviews table: Add order_id, creator_id, brand_id, comment fields

**Validation Gate:**
```bash
./gradlew compileJava  # Should pass with no errors
```

---

## PHASE 2B: ENTITY LAYER FIXES
**Risk Level:** 🟢 LOW (Code-only changes)  
**Effort:** 6 hours  
**Blocks:** Service/Controller updates

### New Entities to Create (12):
- SocialAccount.java
- ContentPreview.java
- PackageAnalytics.java
- Transaction.java
- Wallet.java
- PayoutMethod.java
- WithdrawalRequest.java
- AmbassadorApplication.java
- AmbassadorScore.java
- SavedCreator.java
- NotificationPreference.java
- QuickDealOffer.java

### Enums to Create (11):
- BarterCategory.java
- City.java
- PackageStatus.java
- PackageVisibility.java
- AmbassadorAppStatus.java
- AmbassadorTier.java
- TransactionType.java
- TransactionStatus.java
- WithdrawalStatus.java
- PayoutMethodType.java
- OfferStatus.java

### Entities to Update:
- Creator.java - Add 18+ missing fields
- Brand.java - Add logoUrl, monthlyBudget
- Package.java (ServicePackage) - Add dealType, visibility, analytics relationship
- Order.java - Fix dealType field
- Review.java - Add orderId, creatorId, brandId, comment
- Conversation.java - Fix unread count tracking
- Message.java - Remove embedded offers, add proper relationship

**Validation Gate:**
```bash
./gradlew compileJava  # Must pass
```

---

## PHASE 2C: REPOSITORY LAYER FIXES
**Risk Level:** 🟢 LOW (Query definitions)  
**Effort:** 4 hours  
**Blocks:** Service updates

### New Repositories (12):
- SocialAccountRepository.java
- ContentPreviewRepository.java
- PackageAnalyticsRepository.java
- TransactionRepository.java
- WalletRepository.java
- PayoutMethodRepository.java
- WithdrawalRequestRepository.java
- AmbassadorApplicationRepository.java
- AmbassadorScoreRepository.java
- SavedCreatorRepository.java
- NotificationPreferenceRepository.java
- QuickDealOfferRepository.java

### Existing Repositories - Add Custom Queries:
- CreatorRepository: findByIsTrendingTrue, findByAcceptsBarterTrue, etc.
- PackageRepository: findByCreatorIdAndStatus, findByDealType, etc.
- TransactionRepository: sumByCreatorIdAndType, etc.
- OrderRepository: findByCreatorIdAndStatusIn, etc.

**Validation Gate:**
```bash
./gradlew compileJava  # Must pass
```

---

## PHASE 2D: SERVICE LAYER UPDATES
**Risk Level:** 🟡 MEDIUM (Business logic)  
**Effort:** 12 hours  
**Blocks:** Controller updates

### New Services (9):
- EarningsService.java
- WithdrawalService.java
- PayoutMethodService.java
- AmbassadorService.java
- AnalyticsService.java
- QuickDealService.java
- SavedCreatorService.java
- FileUploadService.java
- NotificationService.java

### Existing Services - Fix/Enhance:
- OrderService: Implement proper status transition validation
- CreatorService: Add social account, content preview, preference management
- MessageService: Implement proper message types, read receipts
- ConversationService: Fix unread count tracking
- ReviewService: Change design to use orderId

**Validation Gate:**
```bash
./gradlew compileJava  # Must pass
```

---

## PHASE 2E: CONTROLLER/API ENDPOINT FIXES
**Risk Level:** 🟡 MEDIUM (API contracts)  
**Effort:** 10 hours  
**Blocks:** Frontend integration

### Existing Controllers - Add Endpoints:
- CreatorController: +7 endpoints (trending, barter-friendly, social accounts, preferences, etc.)
- PackageController: +5 endpoints (update, status, duplicate, analytics, etc.)
- OrderController: +5 endpoints (get by id, progress, deliverables, etc.)
- MessageController: +5 endpoints (messages, attachments, offers, read, etc.)
- ReviewController: +1 endpoint (get by creator)

### New Controllers (6):
- EarningsController.java (+3 endpoints)
- PayoutMethodController.java (+4 endpoints)
- WithdrawalController.java (+2 endpoints)
- QuickDealController.java (+2 endpoints)
- SavedCreatorController.java (+3 endpoints)
- AmbassadorController.java (+7 endpoints)
- AnalyticsController.java (+5 endpoints)
- FileUploadController.java (+6 endpoints)

**Validation Gate:**
```bash
./gradlew build  # Must pass compilation
```

---

## PHASE 2F: DTO UPDATES
**Risk Level:** 🟢 LOW (Data structures)  
**Effort:** 5 hours  
**Blocks:** API contracts

### New DTOs:
- earnings/ folder: EarningsSummaryResponse, TransactionResponse
- withdrawal/ folder: WithdrawalResponse, PayoutMethodResponse
- ambassador/ folder: AmbassadorApplicationResponse, AmbassadorScoreResponse
- analytics/ folder: DashboardResponse, InsightsResponse, PerformanceResponse
- quickdeal/ folder: QuickDealRequest, QuickDealResponse
- upload/ folder: FileUploadResponse
- saved/ folder: SavedCreatorResponse

### Existing DTOs - Update:
- CreatorResponse: Add 14+ missing fields
- PackageResponse: Add dealType, barterCategory, visibility, analytics
- OrderResponse: Add dealType, amount, barterDetails, deliverables
- ReviewResponse: Change from packageId to orderId, add creatorId/brandId

**Validation Gate:**
```bash
./gradlew compileJava  # Must pass
```

---

## PHASE 2G: UTILITY & HELPER UPDATES
**Risk Level:** 🟢 LOW (Support code)  
**Effort:** 3 hours

### Updates:
- ApiResponse.java: Ensure consistent usage
- PageResponse.java: Ensure proper pagination
- Add: ValidationUtil.java, PaginationUtil.java, EnumUtil.java

---

## PHASE 2H: CONFIGURATION UPDATES
**Risk Level:** 🟢 LOW (Configuration)  
**Effort:** 2 hours

### Dependencies to Add:
- AWS S3: `software.amazon.awssdk:s3:2.20.0`
- WebSocket: `org.springframework.boot:spring-boot-starter-websocket`
- Notifications: SendGrid, Twilio SDKs
- Scheduling: `org.springframework.boot:spring-boot-starter-quartz`
- Export: Apache Commons CSV, iText PDF

### New Configuration Classes:
- S3Config.java
- WebSocketConfig.java
- SchedulingConfig.java

### Updates:
- AppConfig.java: Add S3 client, WebSocket config
- SecurityConfig.java: Add role-based endpoint protection
- build.gradle: Add dependencies

---

## PHASE 2I: DEPLOYMENT STRATEGY

### Day-by-Day Execution:
**Day 1:** Schema (Phase 2A) + compile tests
**Day 2:** Entities & Repositories (Phase 2B, 2C) + compile tests
**Day 3:** Services & Controllers (Phase 2D, 2E) + integration tests
**Day 4:** DTOs & Configuration (Phase 2F-2H) + full test suite
**Day 5:** Bug fixes, performance tuning, documentation

### Validation Gates:
- After Phase 2A: `./gradlew compileJava`
- After Phase 2B: `./gradlew compileJava`
- After Phase 2C: `./gradlew compileJava`
- After Phase 2D: `./gradlew compileJava`
- After Phase 2E: `./gradlew build`
- After Phase 2F: `./gradlew compileJava`
- After Phase 2H: Full `./gradlew build` + test suite

### Rollback Plan:
- Code failures: Git rollback to last working commit
- Schema failures: Restore from backup, fix migration, reapply

---

## SUMMARY

**Total Effort:** 50 hours  
**Timeline:** 5-6 business days  
**Risk:** Low (additive approach)  
**Breaking Changes:** Minimal & documented

Ready for **PHASE 3: IMPLEMENTATION** upon approval.

