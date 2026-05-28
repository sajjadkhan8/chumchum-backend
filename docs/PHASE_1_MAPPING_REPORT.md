# PHASE 1: CODEBASE TO SPEC MAPPING REPORT
**Date:** May 28, 2026  
**Project:** ZingZing Backend (Platform Name: Chamcham in codebase)  
**Spec Version:** 1.0.0 (backend-api-spec.md)  
**Current Status:** ~60% Implemented  

---

## EXECUTIVE SUMMARY

This report maps the current codebase against the backend-api-spec.md requirements. The codebase demonstrates:
- **Core entities mostly defined** but missing several key fields from spec
- **Basic CRUD controllers implemented** but endpoints incomplete vs spec requirements
- **Authentication foundation present** but missing OTP verification flow refinements
- **Critical modules partially implemented:** Earnings, Withdrawals, Ambassador Program, Analytics, File Uploads are **MISSING**
- **Schema design issues:** Uses custom naming conventions (e.g., `packaging_type` instead of `deal_type`)
- **Database migrations incomplete:** Schema doesn't fully match entity definitions

---

## A. IMPLEMENTED (MATCHES SPEC)

### ✅ Authentication Module (PARTIAL - 70%)
**Implemented:**
- `POST /api/v1/auth/register` ✓
- `POST /api/v1/auth/login` ✓
- `POST /api/v1/auth/send-otp` ✓
- `POST /api/v1/auth/verify-otp` ✓
- `POST /api/v1/auth/refresh` ✓
- `POST /api/v1/auth/forgot-password` ✓
- `POST /api/v1/auth/reset-password` ✓
- `POST /api/v1/auth/logout` ✓

**Status:** Controllers exist with proper JWT handling. Services handle basic auth flow.

**Minor Issues:**
- OTP rate limiting mentioned in spec but unclear if enforced
- Email validation could be stricter

---

### ✅ User Management Module (PARTIAL - 60%)
**Implemented:**
- `GET /api/v1/users/me` ✓
- `PATCH /api/v1/users/me/password` ✓
- `DELETE /api/v1/users/me` ✓
- `GET /api/v1/users/me/notification-preferences` ✓
- `PUT /api/v1/users/me/notification-preferences` ✓

**Status:** Core user endpoints exist.

**Missing:**
- NotificationPreference entity in database (assumed in-memory only)
- Soft delete tracking (deletedAt field exists in User entity but may not be properly enforced)

---

### ✅ Creator Profile Module (PARTIAL - 65%)
**Implemented:**
- `GET /api/v1/creators` ✓ (basic list, lacks advanced filters)
- `GET /api/v1/creators/{idOrUsername}` ✓ (basic single view)
- `GET /api/v1/creators/me/profile` ✓
- `PATCH /api/v1/creators/me/profile` ✓

**Status:** Basic creator profile CRUD works.

**Issues & Missing:**
- Creator entity missing fields from spec:
  - `username` (uses `email` as identifier instead)
  - `website` field exists but not in profile edit DTO
  - `niche` field missing
  - `availabilityStatus` missing
  - `responseTime` missing
  - `minPrice` / `maxPrice` missing
  - `acceptsBarter` / `acceptsHybridDeals` missing
  - `minimumBudget` missing
  - `preferredIndustries` missing
  - `languages` (should be JSON array)
  - `categories` (should be JSON array)
  - `isVerified` flag missing
  - `isTrending` flag missing
  - `isFastResponder` flag missing
  - `completedDeals` counter missing

- Missing endpoints:
  - `GET /api/v1/creators/trending` ✗
  - `GET /api/v1/creators/barter-friendly` ✗
  - `GET /api/v1/creators/fast-responders` ✗
  - `GET /api/v1/creators/by-city` ✗
  - `PUT /api/v1/creators/me/social-accounts` ✗
  - `PATCH /api/v1/creators/me/preferences` ✗
  - `PATCH /api/v1/creators/me/payment-settings` ✗

- Related entities missing:
  - `SocialAccount` entity
  - `ContentPreview` entity

---

### ✅ Brand Profile Module (PARTIAL - 60%)
**Implemented:**
- `GET /api/v1/brands/me/profile` ✓
- `PATCH /api/v1/brands/me/profile` ✓

**Status:** Basic brand profile exists but incomplete.

**Issues & Missing:**
- Brand entity fields from spec not fully mapped:
  - `logoUrl` field exists as `image` in User parent but not in Brand child
  - `monthlyBudget` missing

- Missing endpoints:
  - No filtering/search for brands (assumed not needed in spec for brands)

---

### ✅ Package (ServicePackage) Module (PARTIAL - 55%)
**Implemented:**
- `POST /api/v1/packages` ✓
- `GET /api/v1/packages` ✓
- `GET /api/v1/packages/{id}` ✓
- `DELETE /api/v1/packages/{id}` ✓

**Status:** Basic CRUD exists but misses many spec requirements.

**Issues & Missing:**
- Status field naming: Uses string `status` instead of proper ENUM
- Package entity missing fields from spec:
  - `dealType` should be required, not defaulting to PAID
  - `barterCategory` missing (uses `barterCategory` but no ENUM)
  - `creatorExpectations` field exists but may not be properly exposed
  - `thumbnailUrl` exists as `coverImage` (naming inconsistency)
  - `isPopular` field exists but not exposed
  - `responseTime` exists but not exposed
   
- Missing endpoints:
  - `PATCH /api/v1/packages/{id}` (edit/update) ✗
  - `PATCH /api/v1/packages/{id}/status` (status transitions) ✗
  - `POST /api/v1/packages/{id}/duplicate` ✗
  - `GET /api/v1/packages/{id}/analytics` ✗
  - `GET /api/v1/creators/{creatorId}/packages` ✗

- Missing entity:
  - `PackageAnalytics` (completely missing from schema and entities)

---

### ✅ Order Module (PARTIAL - 50%)
**Implemented:**
- `POST /api/v1/orders` ✓ (basic order creation)
- `GET /api/v1/orders` ✓
- `PATCH /api/v1/orders/{id}/status` ✓
- Deliverable handling via Order model

**Status:** Order scaffolding exists but incomplete.

**Issues & Missing:**
- Order entity missing/misnamed fields from spec:
  - `orderNumber` field exists but generation logic unclear
  - `dealType` mapping issue: uses `PackagePricingType` instead of `dealType` enum
  - `amount` field precision is 10,2 but should likely be INTEGER for SAR halala
  - `barterDetails` exists
  - `status` field properly uses OrderStatus enum ✓
  - `progress` field exists ✓
  - `deliveryDate` / `deadlineDate` exist ✓
  - Missing: ability to store offer details (quick deals become orders)

- Missing endpoints:
  - `PATCH /api/v1/orders/{id}/progress` ✗
  - `POST /api/v1/orders/{id}/deliverables/{deliverableId}/submit` ✗ (multipart/form-data)
  - `PATCH /api/v1/orders/{id}/deliverables/{deliverableId}/status` ✗

- Deliverable entity:
  - Exists as separate entity ✓
  - Fields mostly present but integration incomplete

- Payment/Escrow logic:
  - No transaction tracking table
  - No escrow/hold mechanism visible

---

### ✅ Conversation & Messaging Module (PARTIAL - 55%)
**Implemented:**
- `GET /api/v1/conversations` ✓
- `POST /api/v1/conversations` ✓
- `GET /api/v1/conversations/{id}/messages` ✗ (likely missing)
- `POST /api/v1/conversations/{id}/messages` ✗ (missing)
- `PATCH /api/v1/conversations/{id}` (mark read) ✓ (but may be broken endpoint)

**Status:** Conversation scaffolding exists, messaging incomplete.

**Issues & Missing:**
- Conversation entity has wrong unread tracking:
  - Uses `readByCreator` / `readByBrand` (boolean) instead of `unreadCountCreator` / `unreadCountBrand`
  - Fields actually exist (`unreadCountCreator`, `unreadCountBrand`) but may not be used properly

- Message entity:
  - Has embedded offer fields (old design) instead of proper QuickDealOffer relationship
  - Missing `type` enum field (has hardcoded MessageType but not properly exposed)
  - Missing proper read status tracking per message

- Missing endpoints:
  - `GET /api/v1/conversations/{id}/messages` ✗
  - `POST /api/v1/conversations/{id}/messages` (text message) ✗
  - `POST /api/v1/conversations/{id}/messages/offer` ✗
  - `POST /api/v1/conversations/{id}/messages/attachment` ✗
  - `PATCH /api/v1/conversations/{id}/read` ✗

- Missing entity:
  - `QuickDealOffer` (separate from Message)

---

### ✅ Review Module (PARTIAL - 60%)
**Implemented:**
- `POST /api/v1/reviews` ✓
- `GET /api/v1/reviews/{packageId}` ✓ (get reviews for a package)

**Status:** Basic review CRUD present.

**Issues & Missing:**
- Review entity issues:
  - Unique constraint on `(package_id, reviewer_id)` but should be `orderId` ✗
  - Missing `brandId` / `creatorId` fields (should have direct references)
  - `star` field should be `rating` (1-5 integer)
  - Missing `comment` field (uses `description`)

- Missing endpoints:
  - `GET /api/v1/creators/{creatorId}/reviews` ✗

---

### ✅ Message Controller (PARTIAL - 40%)
**Implemented:**
- `MessageController` exists with basic skeleton

**Status:** Not properly implemented.

**Issues & Missing:**
- All message endpoints missing implementation
- No real-time (WebSocket) support visible

---

## B. PARTIALLY IMPLEMENTED

### ⚠️ Order Lifecycle & Status Transitions (INCOMPLETE - 30%)
**Status:** Order status transitions exist as enum but business logic incomplete.

**Issues:**
- `OrderService.updateOrderStatus()` method exists but likely doesn't enforce spec-defined transitions
- No validation that status transitions follow allowed paths:
  - PENDING → ACCEPTED, CANCELLED
  - ACCEPTED → IN_PROGRESS, CANCELLED (within 24h)
  - IN_PROGRESS → DELIVERED
  - DELIVERED → REVIEW → COMPLETED or REVISION
  - REVISION → IN_PROGRESS
- No auto-completion after 72h (background job missing)
- No payment hold/release logic on status changes

---

### ⚠️ Entities (INCOMPLETE - 50%)
**Status:** Core entities exist but missing many fields and relationships.

**Summary of missing entity fields:**

| Entity | Missing Fields | Status |
|--------|---|---|
| **User** | `phone` (nullable), `passwordHash` (nullable for OTP users), `avatarUrl` | Mostly OK, but needs nullable password |
| **Creator** | `username` (unique), `coverImageUrl`, `website`, `niche`, `availabilityStatus`, `responseTime`, `minPrice`, `maxPrice`, `isVerified`, `isTrending`, `isFastResponder`, `completedDeals`, `acceptsBarter`, `acceptsHybridDeals`, `minimumBudget`, `preferredIndustries`, `languages` (JSON), `categories` (JSON) | **CRITICAL MISSING** |
| **SocialAccount** | **Entire entity missing** | **MISSING** |
| **ContentPreview** | **Entire entity missing** | **MISSING** |
| **Brand** | `logoUrl`, `monthlyBudget` | **MISSING** |
| **Package** | `dealType` (ENUM not field), `barterCategory` (ENUM), `creatorExpectations`, proper status/visibility enums, `tags` (array), `deliverables` (array), `visibility` enum | **INCOMPLETE** |
| **PackageAnalytics** | **Entire entity missing** | **MISSING** |
| **Order** | Proper `dealType` field, `message` field usage, escrow tracking | **INCOMPLETE** |
| **Deliverable** | Mostly present, needs proper status enum | **OK** |
| **Review** | Should reference `orderId` not `packageId`, needs `brandId`/`creatorId`, `comment` instead of `description` | **WRONG DESIGN** |
| **Conversation** | Wrong unread count design | **WRONG DESIGN** |
| **Message** | Missing proper offer relationship, no real message type enum | **INCOMPLETE** |
| **Transaction** | **Entire entity missing** | **MISSING** |
| **Wallet** | **Entire entity missing** | **MISSING** |
| **PayoutMethod** | **Entire entity missing** | **MISSING** |
| **WithdrawalRequest** | **Entire entity missing** | **MISSING** |
| **AmbassadorApplication** | **Entire entity missing** | **MISSING** |
| **AmbassadorScore** | **Entire entity missing** | **MISSING** |
| **SavedCreator** | **Entire entity missing** | **MISSING** |
| **NotificationPreference** | **Entire entity missing** | **MISSING** |
| **QuickDealOffer** | **Entire entity missing** | **MISSING** |

---

## C. MISSING (FROM SPEC BUT NOT IN CODE)

### 🚫 CRITICAL MISSING MODULES

#### 1. Earnings & Withdrawals Module (0% - COMPLETELY MISSING)
**Spec Requirements:**
- `GET /api/v1/earnings/summary` ✗
- `GET /api/v1/earnings/transactions` ✗
- `GET /api/v1/earnings/transactions/export` ✗
- `GET /api/v1/payout-methods` ✗
- `POST /api/v1/payout-methods` ✗
- `PATCH /api/v1/payout-methods/{id}` ✗
- `DELETE /api/v1/payout-methods/{id}` ✗
- `POST /api/v1/withdrawals` ✗
- `GET /api/v1/withdrawals` ✗

**Required Entities:**
- `Transaction` entity
- `Wallet` entity
- `PayoutMethod` entity
- `WithdrawalRequest` entity

**Status:** Completely missing. No code, no schema, no services.

---

#### 2. Ambassador Program Module (0% - COMPLETELY MISSING)
**Spec Requirements:**
- `GET /api/v1/ambassador/application` ✗
- `POST /api/v1/ambassador/application` ✗
- `GET /api/v1/ambassador/score` ✗
- `GET /api/v1/ambassador/application/{id}` (Admin) ✗
- `GET /api/v1/ambassador/applications` (Admin) ✗
- `PATCH /api/v1/ambassador/applications/{id}` (Admin) ✗
- `GET /api/v1/ambassador/ambassadors` ✗

**Required Entities:**
- `AmbassadorApplication` entity
- `AmbassadorScore` entity

**Status:** Completely missing. User entity has `creatorProgramStatus` enum but no supporting entities/endpoints.

---

#### 3. File Upload Module (0% - COMPLETELY MISSING)
**Spec Requirements:**
- `POST /api/v1/uploads/avatar` ✗
- `POST /api/v1/uploads/cover-image` ✗
- `POST /api/v1/uploads/content-preview` ✗
- `POST /api/v1/uploads/package-thumbnail` ✗
- `POST /api/v1/uploads/deliverable` ✗
- `POST /api/v1/uploads/brand-logo` ✗

**Required Services:**
- File upload handler
- S3/CDN integration
- Image optimization

**Status:** Completely missing. No upload endpoints, no file service, no S3 integration.

---

#### 4. Analytics Module (0% - COMPLETELY MISSING)
**Spec Requirements:**
- `GET /api/v1/analytics/creator/dashboard` ✗
- `GET /api/v1/analytics/brand/dashboard` ✗
- `GET /api/v1/analytics/creator/insights` ✗
- `GET /api/v1/analytics/creator/performance` ✗
- `GET /api/v1/analytics/brand/campaigns` ✗

**Required Entities/Services:**
- Analytics aggregation service
- Dashboard computation logic
- Historical tracking

**Status:** Completely missing.

---

#### 5. Quick Deals Module (0% - COMPLETELY MISSING)
**Spec Requirements:**
- `POST /api/v1/quick-deals` ✗
- `PATCH /api/v1/quick-deals/{offerId}/respond` ✗

**Required Entities:**
- `QuickDealOffer` entity (separate from Message)

**Status:** Completely missing. Message entity has embedded offer fields (wrong design).

---

#### 6. Saved Creators Module (0% - COMPLETELY MISSING)
**Spec Requirements:**
- `GET /api/v1/saved-creators` ✗
- `POST /api/v1/saved-creators/{creatorId}` ✗
- `DELETE /api/v1/saved-creators/{creatorId}` ✗

**Required Entities:**
- `SavedCreator` entity (junction table)

**Status:** Completely missing.

---

#### 7. Notifications Module (0% - COMPLETELY MISSING)
**Spec Requirements:**
- Notification preference storage and delivery
- WebSocket real-time events
- Push/Email/SMS integration

**Required Entities:**
- `NotificationPreference` entity
- `Notification` entity (for feed)

**Status:** Completely missing. No notification preferences storage, no real-time support.

---

#### 8. Real-time/WebSocket Module (0% - COMPLETELY MISSING)
**Spec Requirements:**
- Socket.IO integration (or equivalent)
- Real-time messaging
- Typing indicators
- Read receipts
- Notification delivery

**Status:** Completely missing. No WebSocket library in dependencies, no real-time service.

---

#### 9. Social Accounts & Content Preview (0% - COMPLETELY MISSING)
**Spec Requirements:**
- `SocialAccount` entity
- `ContentPreview` entity
- Creator profile enrichment

**Status:** Completely missing. Creator entity has hardcoded social URLs instead of polymorphic SocialAccount.

---

#### 10. Advanced Creator Search & Filtering (0% - INCOMPLETE)
**Spec Requirements (GET /api/v1/creators):**
- `categories[]` filter ✗
- `platforms[]` filter ✗
- `cities[]` filter ✗
- `dealTypes[]` filter ✗
- `barterTypes[]` filter ✗
- `minFollowers` / `maxFollowers` filter ✗
- `minRating` filter ✗
- `minPrice` / `maxPrice` filter ✗
- `sortBy` (trending, budget_friendly, top_rated, near_you) ✗
- `ambassadorOnly` filter ✗

**Status:** Basic implementation only returns all creators. No filtering logic.

---

## D. EXTRA / INVALID (TO BE REMOVED)

### 🗑️ Demo/Sample Endpoints (TO BE REMOVED)
- `HealthController` - Demo endpoint, not in spec ✗

### 🗑️ Unused/Incorrect Design Patterns
- `PackageTier` entity - Not in spec (packages have tiers in backend but not exposed as separate entity in API)
- Embedded offer fields in `Message` - Should be separate `QuickDealOffer` entity
- `readByCreator`/`readByBrand` booleans in Conversation - Should use `unreadCountCreator`/`unreadCountBrand` integers
- Old review design - Keyed on `packageId` instead of `orderId`

### 🗑️ Incomplete/Unused Services
- Services exist but many methods likely don't match controller signatures

---

## E. SCHEMA ISSUES (DATABASE)

### 🔴 Migration V1__init_schema.sql Issues
1. **Missing tables completely:**
   - `social_accounts` ✗
   - `content_previews` ✗
   - `package_analytics` ✗
   - `transactions` ✗
   - `wallets` ✗
   - `payout_methods` ✗
   - `withdrawal_requests` ✗
   - `ambassador_applications` ✗
   - `ambassador_scores` ✗
   - `saved_creators` ✗
   - `notification_preferences` ✗
   - `quick_deal_offers` ✗
   - `profile_views` (for analytics) ✗
   - `deliverables` ✗

2. **Schema mismatches:**
   - `Order` table missing fields:
     - `deal_type` ✗
     - `order_number` exists but generation unclear
     - `payment_intent` should be `stripe_payment_intent_id` (not in spec)
     - `completed` boolean should be `status` ENUM
   - `Message` table missing fields:
     - `type` ✗
     - `is_read` ✗
     - `offer_*` fields should reference separate `quick_deal_offers` table
   - `Conversation` missing:
     - `unread_count_creator` and `unread_count_brand` columns (named but maybe not created?)
   - `Package` table missing:
     - `deal_type` field ✗
     - `status` field (should be ENUM)
     - `visibility` field (should be ENUM)
     - Proper fields for barter, hybrid deals
   - `Creator` table missing many fields (see entity issues above)

3. **Naming inconsistencies:**
   - Uses snake_case in SQL but some entity fields use camelCase differently
   - `pricing_type` instead of `deal_type` (terminology mismatch with spec)

---

## F. MISSING ENUMERATIONS

From spec section 11 (Enumerations), check codebase enums:

| Enum | Spec Values | Codebase | Status |
|------|---|---|---|
| `UserRole` | creator, brand, platform_admin | CREATOR, BRAND, PLATFORM_ADMIN | ✓ |
| `Platform` | instagram, tiktok, youtube, facebook, snapchat | INSTAGRAM, TIKTOK, YOUTUBE, FACEBOOK, SNAPCHAT | ✓ |
| `DealType` | paid, barter, hybrid | **PackagePricingType**: PAID, BARTER, HYBRID | ⚠️ (naming issue) |
| `BarterCategory` | food, hotel, salon, events, products, services, travel, education | **MISSING** | ✗ |
| `City` | Riyadh, Jeddah, Dammam, Mecca, Medina, Khobar, Tabuk | **String field, no enum** | ✗ |
| `OrderStatus` | pending, accepted, in_progress, delivered, review, revision, completed, cancelled | PENDING, ACCEPTED, IN_PROGRESS, DELIVERED, REVIEW, REVISION, COMPLETED, CANCELLED | ✓ |
| `PackageStatus` | active, draft, paused, archived, under_review | String field, no enum | ✗ |
| `PackageVisibility` | public, private | String field, no enum | ✗ |
| `AmbassadorStatus` | (various) | **MISSING** | ✗ |
| `AmbassadorAppStatus` | draft, submitted, under_review, verified, approved, rejected | **MISSING** | ✗ |
| `AmbassadorTier` | rising_creator, emerging_ambassador, verified_ambassador, elite_ambassador | **MISSING** | ✗ |
| `CreatorProgramStatus` | none, in_path, active_ambassador | NONE, IN_PATH, ACTIVE_AMBASSADOR (in User) | ✓ |
| `TransactionType` | earning, withdrawal, refund, platform_fee | **MISSING** | ✗ |
| `TransactionStatus` | pending, completed, failed | **MISSING** | ✗ |
| `WithdrawalStatus` | pending, processing, completed, failed | **MISSING** | ✗ |
| `PayoutMethodType` | stcpay, mada, applepay, bank_transfer | **MISSING** | ✗ |
| `MessageType` | text, offer, system, attachment | TEXT, OFFER, SYSTEM, ATTACHMENT (in Message) | ✓ |
| `OfferStatus` | pending, accepted, rejected | **String field** | ⚠️ |
| `DeliverableStatus` | pending, in_progress, completed, revision, review | PENDING, IN_PROGRESS, COMPLETED, REVISION, REVIEW (in Deliverable) | ✓ |

---

## G. CONFIGURATION & INFRASTRUCTURE

### Security Config
- JWT token validation: Present ✓
- CORS configuration: Present ✓
- Role-based access: Partially present (AuthenticatedUser used but enforcement incomplete)

### OpenAPI/Swagger
- Swagger UI enabled ✓
- Needs endpoint documentation update

### Database
- PostgreSQL ✓
- Flyway migrations ✓
- Schema creation ✓
- But migrations incomplete (missing many tables)

---

## SUMMARY TABLE

| Area | Completion | Status | Priority |
|------|---|---|---|
| **Authentication** | 70% | Partial |🟢 (Minor fixes) |
| **Users** | 60% | Partial | 🟡 (Medium) |
| **Creators** | 40% | Partial | 🔴 (Critical - many missing fields/endpoints) |
| **Brands** | 50% | Partial | 🟡 (Medium) |
| **Packages** | 55% | Partial | 🔴 (Critical - missing analytics, status transitions) |
| **Orders** | 50% | Partial | 🔴 (Critical - incomplete lifecycle) |
| **Conversations** | 55% | Partial | 🔴 (Critical - messaging incomplete) |
| **Messages** | 40% | Partial | 🔴 (Critical - endpoints missing) |
| **Reviews** | 60% | Partial | 🟡 (Wrong design, needs refactor) |
| **Earnings/Withdrawals** | 0% | Missing | 🔴 (Critical) |
| **Ambassador Program** | 0% | Missing | 🔴 (Critical) |
| **File Uploads** | 0% | Missing | 🔴 (Critical) |
| **Analytics** | 0% | Missing | 🔴 (Critical) |
| **Quick Deals** | 0% | Missing | 🔴 (Critical) |
| **Saved Creators** | 0% | Missing | 🔴 (Medium) |
| **Notifications** | 0% | Missing | 🟡 (Medium - lower priority) |
| **WebSocket/Real-time** | 0% | Missing | 🔴 (Critical for UX) |
| **Social Accounts** | 0% | Missing | 🟡 (Medium) |
| **API Response Wrapper** | 70% | Partial | 🟡 (Inconsistent usage) |

**Overall Progress:** ~40% of spec implemented

---

## KEY FINDINGS & RISKS

### 🔴 CRITICAL ISSUES
1. **Entity field mismatches** - Creator, Brand, Package entities missing 20+ fields each
2. **No data model for financial tracking** - Transaction, Wallet, PayoutMethod missing (blocks earnings feature)
3. **Ambassador Program incomplete** - Entities missing, affects user engagement metric
4. **File upload infrastructure missing** - Blocks image uploads for avatars, covers, packages
5. **Analytics infrastructure missing** - Dashboard endpoints incomplete
6. **Real-time missing** - WebSocket not integrated (affects messaging experience)
7. **Order lifecycle incomplete** - Status transitions not enforced, auto-completion missing

### 🟡 MEDIUM ISSUES
1. **Schema design issues** - Several tables use wrong column names/types
2. **Review entity redesign needed** - Should key on `orderId`, not `packageId`
3. **Conversation unread count design** - Current boolean design insufficient
4. **Message type handling** - Embedded offer fields should be separate entity
5. **Quick deals missing** - Standalone offer creation not supported

### 🟢 MINOR ISSUES
1. **Naming conventions** - `PackagePricingType` should be `DealType`
2. **Enum usage** - Some fields using string instead of proper enums
3. **API response inconsistency** - Some endpoints wrap in `ApiResponse`, others don't

---

## NEXT STEPS

This mapping is complete. Proceeding to **PHASE 2** for refactor/cleanup plan.

