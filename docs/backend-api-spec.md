# backend-api-spec.md

# ZingZing – Backend API Specification
**Version:** 1.0.0  
**Date:** 2026-05-26  
**Base URL:** `/api/v1`  
**Platform:** Saudi Arabia's Influencer Marketplace (KSA-focused)  
**Currency:** SAR (Saudi Riyal, stored as integer halala or decimal SAR)

---

# 1. Application Overview

## Purpose
ZingZing is a two-sided influencer marketplace connecting **Creators** (influencers) with **Brands** in Saudi Arabia. The platform supports three deal types:
- **Paid** – cash payment for content
- **Barter** – product/service exchange
- **Hybrid** – cash + barter combination

It also includes a first-party **Platform Ambassador Program** where top creators undergo a multi-step verification and earn guaranteed monthly income and premium brand access.

## User Roles
| Role | Description |
|------|-------------|
| `CREATOR` | Influencer who creates packages, fulfills orders, and earns money |
| `BRAND` | Company that discovers creators, sends deals/orders, and tracks campaigns |
| `PLATFORM_ADMIN` | Internal team managing ambassador applications, moderation, and analytics |

## Major Workflows
1. **Authentication** – Email+password or Phone+OTP login, role-based signup
2. **Creator Discovery** – Brands search/filter creators with rich faceted filters
3. **Quick Deal** – Brand sends an offer directly from a creator's card
4. **Package Ordering** – Brand places an order on a creator's listed package
5. **Order Lifecycle** – pending → accepted → in_progress → delivered/review → completed/revision → cancelled
6. **Messaging** – Real-time conversation with embedded deal offers
7. **Ambassador Program** – Creator applies, platform reviews through 4 verification steps, approval/rejection
8. **Creator Earnings** – Transaction history, balance management, multi-channel withdrawal
9. **Creator Packages** – CRUD for packages with analytics, status management
10. **Analytics/Dashboards** – Aggregated stats for both roles

## Detected Modules
1. Authentication & OTP
2. Users (Creator & Brand profiles)
3. Creators (extended profile, social links, search)
4. Packages (CRUD, analytics, status management)
5. Orders (lifecycle management, deliverables)
6. Conversations & Messages (real-time)
7. Quick Deals / Offers
8. Reviews & Ratings
9. Earnings & Withdrawals
10. Payout Methods
11. Ambassador Program (applications, scoring, tiers)
12. Notifications
13. Analytics & Statistics
14. File Uploads
15. Saved Creators

---

# 2. Detected User Roles

## CREATOR
- **Accessible modules:** Dashboard, Packages, Orders, Messages, Earnings, Insights, Performance, Settings, Ambassador Program
- **Capabilities:**
  - Create/manage packages (paid, barter, hybrid)
  - Accept/decline/deliver orders
  - Submit deliverables (file uploads)
  - Request withdrawals from available balance
  - Manage social account connections
  - Apply to Platform Ambassador Program
  - View own ambassador score and tier
  - Configure notification preferences, payout methods
  - Toggle barter/hybrid deal acceptance in preferences

## BRAND
- **Accessible modules:** Dashboard, Explore, Ambassadors, Orders, Messages, Analytics, Saved Creators, Settings
- **Capabilities:**
  - Search/filter creators with rich facets
  - Send quick deal offers
  - Place package orders
  - Approve/request revision on deliveries
  - Leave reviews on completed orders
  - Save/unsave creators
  - View campaign analytics
  - Message creators in real-time

## PLATFORM_ADMIN *(inferred)*
- **Accessible modules:** Ambassador Applications, Creator Moderation, Platform Analytics
- **Capabilities:**
  - Review/approve/reject ambassador applications
  - Update verification step statuses
  - Set commission structures
  - View platform-wide statistics

---

# 3. Detected Backend Modules

1. **Auth** – login, phone OTP, signup, forgot password, refresh token
2. **Users** – profile, settings, notifications preferences
3. **Creators** – profile, social stats, content previews, search/filter
4. **Brands** – profile, industry, settings
5. **Packages** – CRUD, status transitions, analytics aggregation
6. **Orders** – place, status transitions, deliverables, revisions
7. **Conversations** – list, create, read with unread counts
8. **Messages** – send text, send offer, mark read, file attachments
9. **QuickDeals** – standalone offer creation (outside existing conversation)
10. **Reviews** – post-completion review by brand
11. **Earnings** – balance, transaction history, export
12. **Withdrawals** – request, status, payout method management
13. **AmbassadorProgram** – application submission, status tracking, verification steps
14. **AmbassadorScoring** – calculated score, tier, percentile (server-side)
15. **Saved** – save/unsave creators per brand user
16. **Notifications** – push/email/SMS preferences, delivery
17. **Analytics** – creator insights, brand campaign analytics, dashboard aggregates
18. **Uploads** – avatar, cover image, content previews, deliverable files

---

# 4. Detected Entities & Relationships

## User
```
id:                    UUID (PK)
email:                 VARCHAR(255), unique, nullable
phone:                 VARCHAR(20), unique, nullable
passwordHash:          VARCHAR (nullable if phone-only user)
role:                  ENUM('creator', 'brand')
name:                  VARCHAR(100)
avatarUrl:             VARCHAR(500), nullable
creatorProgramStatus:  ENUM('none', 'in_path', 'active_ambassador') default 'none'
isActive:              BOOLEAN default true
createdAt:             DATETIME
updatedAt:             DATETIME
deletedAt:             DATETIME nullable (soft delete)
```
- One User → One Creator OR One Brand (polymorphic profile)

## Creator
```
id:                    UUID (PK)
userId:                UUID (FK → User)
username:              VARCHAR(50) unique
bio:                   TEXT nullable
city:                  ENUM(City)
coverImageUrl:         VARCHAR(500) nullable
website:               VARCHAR(300) nullable
niche:                 VARCHAR(100) nullable
availabilityStatus:    VARCHAR(100) nullable
collaborationPrefs:    TEXT nullable
responseTime:          VARCHAR(50)
minPrice:              INTEGER nullable (SAR in halalas or decimal)
maxPrice:              INTEGER nullable
isVerified:            BOOLEAN default false
isTrending:            BOOLEAN default false
isFastResponder:       BOOLEAN default false
rating:                DECIMAL(3,2) default 0
totalReviews:          INTEGER default 0
completedDeals:        INTEGER default 0
acceptsBarter:         BOOLEAN default true
acceptsHybridDeals:    BOOLEAN default true
minimumBudget:         INTEGER nullable
preferredIndustries:   TEXT nullable
languages:             JSON (string[])
categories:            JSON (string[])
createdAt:             DATETIME
updatedAt:             DATETIME
```

## SocialAccount
```
id:                    UUID (PK)
creatorId:             UUID (FK → Creator)
platform:              ENUM('instagram','tiktok','youtube','facebook','snapchat')
username:              VARCHAR(100)
profileUrl:            VARCHAR(500)
followers:             INTEGER
avgViews:              INTEGER nullable
engagementRate:        DECIMAL(5,2)
isVerified:            BOOLEAN default false
createdAt:             DATETIME
updatedAt:             DATETIME
```

## ContentPreview
```
id:                    UUID (PK)
creatorId:             UUID (FK → Creator)
type:                  ENUM('image','video')
thumbnailUrl:          VARCHAR(500)
mediaUrl:              VARCHAR(500)
platform:              ENUM(Platform)
views:                 INTEGER nullable
likes:                 INTEGER nullable
createdAt:             DATETIME
```

## Brand
```
id:                    UUID (PK)
userId:                UUID (FK → User)
name:                  VARCHAR(100)
logoUrl:               VARCHAR(500)
industry:              VARCHAR(100)
website:               VARCHAR(300) nullable
city:                  ENUM(City)
description:           TEXT nullable
monthlyBudget:         INTEGER nullable
createdAt:             DATETIME
updatedAt:             DATETIME
```

## Package
```
id:                    UUID (PK)
creatorId:             UUID (FK → Creator)
title:                 VARCHAR(200)
shortDescription:      VARCHAR(300)
fullDescription:       TEXT
category:              VARCHAR(100)
platform:              ENUM(Platform)
dealType:              ENUM('paid','barter','hybrid')
price:                 INTEGER nullable (SAR, required if paid/hybrid)
barterValue:           VARCHAR(300) nullable
barterDescription:     TEXT nullable
barterCategory:        ENUM(BarterCategory) nullable
estimatedBarterValue:  INTEGER nullable
hybridCashAmount:      INTEGER nullable
hybridBarterValue:     INTEGER nullable
creatorExpectations:   TEXT nullable
deliverables:          JSON (string[])
deliveryDays:          INTEGER
revisions:             INTEGER nullable default 1
tags:                  JSON (string[])
thumbnailUrl:          VARCHAR(500)
status:                ENUM('active','draft','paused','archived','under_review') default 'draft'
visibility:            ENUM('public','private') default 'public'
isPopular:             BOOLEAN default false
ordersCompleted:       INTEGER default 0
responseTime:          VARCHAR(50)
createdAt:             DATETIME
updatedAt:             DATETIME
```

## PackageAnalytics *(aggregated, may be materialized)*
```
packageId:             UUID (FK → Package)
views:                 INTEGER default 0
clicks:                INTEGER default 0
inquiries:             INTEGER default 0
conversionRate:        DECIMAL(5,2) default 0
completionRate:        DECIMAL(5,2) default 0
repeatBrands:          INTEGER default 0
engagementPerformance: DECIMAL(5,2) default 0
updatedAt:             DATETIME
```

## Order
```
id:                    UUID (PK)
orderNumber:           VARCHAR(20) unique (e.g. ORD-00123)
packageId:             UUID (FK → Package)
creatorId:             UUID (FK → Creator)
brandId:               UUID (FK → Brand)
dealType:              ENUM('paid','barter','hybrid')
amount:                INTEGER nullable (SAR)
barterDetails:         TEXT nullable
message:               TEXT
status:                ENUM('pending','accepted','in_progress','delivered','review','revision','completed','cancelled')
progress:              INTEGER default 0 (0-100)
deliveryDate:          DATE nullable
deadlineDate:          DATE nullable
createdAt:             DATETIME
updatedAt:             DATETIME
```

## Deliverable
```
id:                    UUID (PK)
orderId:               UUID (FK → Order)
name:                  VARCHAR(200)
status:                ENUM('pending','in_progress','completed','revision','review')
fileUrl:               VARCHAR(500) nullable
submittedAt:           DATETIME nullable
createdAt:             DATETIME
updatedAt:             DATETIME
```

## Review
```
id:                    UUID (PK)
orderId:               UUID (FK → Order) unique
creatorId:             UUID (FK → Creator)
brandId:               UUID (FK → Brand)
rating:                INTEGER (1-5)
comment:               TEXT nullable
createdAt:             DATETIME
```

## Conversation
```
id:                    UUID (PK)
creatorId:             UUID (FK → Creator)
brandId:               UUID (FK → Brand)
unreadCountCreator:    INTEGER default 0
unreadCountBrand:      INTEGER default 0
lastMessageId:         UUID (FK → Message) nullable
updatedAt:             DATETIME
createdAt:             DATETIME
```
- Unique constraint: (creatorId, brandId)

## Message
```
id:                    UUID (PK)
conversationId:        UUID (FK → Conversation)
senderId:              UUID (FK → User)
senderType:            ENUM('creator','brand')
content:               TEXT nullable
type:                  ENUM('text','offer','system','attachment')
isRead:                BOOLEAN default false
attachmentUrl:         VARCHAR(500) nullable
createdAt:             DATETIME
```

## QuickDealOffer *(embedded in Message or standalone)*
```
id:                    UUID (PK)
messageId:             UUID (FK → Message) nullable
conversationId:        UUID (FK → Conversation)
dealType:              ENUM('paid','barter','hybrid')
amount:                INTEGER nullable
barterDetails:         TEXT nullable
barterCategory:        ENUM(BarterCategory) nullable
estimatedBarterValue:  INTEGER nullable
creatorExpectation:    TEXT nullable
message:               TEXT
status:                ENUM('pending','accepted','rejected') default 'pending'
createdAt:             DATETIME
updatedAt:             DATETIME
```

## Transaction
```
id:                    UUID (PK)
creatorId:             UUID (FK → Creator)
orderId:               UUID (FK → Order) nullable
type:                  ENUM('earning','withdrawal','refund','platform_fee')
amount:                INTEGER (positive for credit, negative for debit – OR separate sign column)
description:           VARCHAR(300)
status:                ENUM('pending','completed','failed')
createdAt:             DATETIME
```

## Wallet *(per creator)*
```
creatorId:             UUID (PK, FK → Creator)
totalEarned:           INTEGER default 0
availableBalance:      INTEGER default 0
pendingBalance:        INTEGER default 0
updatedAt:             DATETIME
```

## PayoutMethod
```
id:                    UUID (PK)
creatorId:             UUID (FK → Creator)
type:                  ENUM('stcpay','mada','applepay','bank_transfer')
name:                  VARCHAR(100)
accountDetails:        VARCHAR(300) (masked on read)
isDefault:             BOOLEAN default false
createdAt:             DATETIME
```

## WithdrawalRequest
```
id:                    UUID (PK)
creatorId:             UUID (FK → Creator)
payoutMethodId:        UUID (FK → PayoutMethod)
amount:                INTEGER
status:                ENUM('pending','processing','completed','failed')
processedAt:           DATETIME nullable
createdAt:             DATETIME
```

## AmbassadorApplication
```
id:                    UUID (PK)
creatorId:             UUID (FK → Creator) unique
status:                ENUM('draft','submitted','under_review','verified','approved','rejected')
submittedAt:           DATETIME nullable
updatedAt:             DATETIME
identityVerified:      BOOLEAN default false
engagementVerified:    BOOLEAN default false
contentReviewPassed:   BOOLEAN default false
backgroundCheckPassed: BOOLEAN default false
notes:                 TEXT nullable
rejectionReason:       TEXT nullable
approvedAt:            DATETIME nullable
reviewedBy:            UUID (FK → User/Admin) nullable
```

## AmbassadorScore *(computed & persisted for caching)*
```
creatorId:             UUID (PK, FK → Creator)
total:                 INTEGER (0-100)
deliveryScore:         INTEGER
accountAgeScore:       INTEGER
ratingScore:           INTEGER
cancellationScore:     INTEGER
profileCompletenessScore: INTEGER
consistencyScore:      INTEGER
tier:                  ENUM('rising_creator','emerging_ambassador','verified_ambassador','elite_ambassador')
percentileRank:        INTEGER
strengths:             JSON (string[])
improvements:          JSON (string[])
calculatedAt:          DATETIME
```

## SavedCreator
```
brandId:               UUID (FK → Brand)
creatorId:             UUID (FK → Creator)
savedAt:               DATETIME
PK: (brandId, creatorId)
```

## NotificationPreference
```
userId:                UUID (PK, FK → User)
newOrders:             BOOLEAN default true
messages:              BOOLEAN default true
reviews:               BOOLEAN default true
marketing:             BOOLEAN default false
weeklyDigest:          BOOLEAN default true
pushNotifications:     BOOLEAN default true
emailNotifications:    BOOLEAN default true
smsNotifications:      BOOLEAN default false
```

---

# 5. Complete API Specifications

---

## MODULE: AUTHENTICATION

---

### POST /api/v1/auth/register

**Purpose:** Signup page – user selects role then submits email/password/name.

**Auth:** Public

**Request:**
```json
{
  "name": "Faisal Al Harbi",
  "email": "faisal@example.com",
  "password": "StrongPass123!",
  "role": "creator"
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGc...",
    "refreshToken": "eyJhbGc...",
    "user": {
      "id": "uuid",
      "email": "faisal@example.com",
      "name": "Faisal Al Harbi",
      "role": "creator",
      "creatorProgramStatus": "in_path",
      "createdAt": "2026-05-26T10:00:00Z"
    }
  }
}
```

**Validation:**
- `name`: required, 2-100 chars
- `email`: required, valid email, unique
- `password`: required, min 8 chars, must include uppercase + number
- `role`: required, must be `creator` or `brand`

**Error Responses:**
- `400` – Validation errors
- `409` – Email already in use

---

### POST /api/v1/auth/login

**Purpose:** Email+password login from login page.

**Auth:** Public

**Request:**
```json
{
  "email": "creator@test.com",
  "password": "password123"
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGc...",
    "refreshToken": "eyJhbGc...",
    "user": {
      "id": "uuid",
      "email": "creator@test.com",
      "name": "Faisal Al Harbi",
      "role": "creator",
      "avatarUrl": "https://...",
      "creatorProgramStatus": "in_path",
      "createdAt": "2026-05-26T10:00:00Z"
    }
  }
}
```

**Validation:**
- `email`: required, valid email
- `password`: required

**Error Responses:**
- `400` – Missing fields
- `401` – Invalid credentials

---

### POST /api/v1/auth/send-otp

**Purpose:** Phone login – user enters phone number, OTP is sent.

**Auth:** Public

**Request:**
```json
{
  "phone": "+966551234567"
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "message": "OTP sent successfully",
    "expiresIn": 300
  }
}
```

**Validation:**
- `phone`: required, valid Saudi phone number (+966 prefix)
- Rate limit: max 3 OTP sends per phone per 10 minutes

**Error Responses:**
- `400` – Invalid phone format
- `429` – Too many OTP requests

---

### POST /api/v1/auth/verify-otp

**Purpose:** Phone login verification – user submits OTP.

**Auth:** Public

**Request:**
```json
{
  "phone": "+966551234567",
  "otp": "123456"
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGc...",
    "refreshToken": "eyJhbGc...",
    "user": {
      "id": "uuid",
      "phone": "+966551234567",
      "name": "Khalid Al Dosari",
      "role": "creator",
      "creatorProgramStatus": "in_path"
    }
  }
}
```

**Validation:**
- `phone`: required
- `otp`: required, 6-digit string

**Error Responses:**
- `401` – Invalid or expired OTP

---

### POST /api/v1/auth/refresh

**Purpose:** Refresh access token using refresh token.

**Auth:** Public (with valid refresh token)

**Request:**
```json
{
  "refreshToken": "eyJhbGc..."
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGc...",
    "refreshToken": "eyJhbGc..."
  }
}
```

**Error Responses:**
- `401` – Invalid or expired refresh token

---

### POST /api/v1/auth/forgot-password

**Purpose:** Request password reset email.

**Auth:** Public

**Request:**
```json
{
  "email": "user@example.com"
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "message": "If your email exists, a reset link has been sent."
  }
}
```

---

### POST /api/v1/auth/reset-password

**Purpose:** Reset password with token from email.

**Auth:** Public (with valid reset token)

**Request:**
```json
{
  "token": "reset-token-from-email",
  "newPassword": "NewPass456!"
}
```

**Error Responses:**
- `400` – Weak password
- `401` – Invalid/expired reset token

---

### POST /api/v1/auth/logout

**Purpose:** Invalidate refresh token.

**Auth:** Authenticated

**Request:** *(no body, uses Authorization header)*

**Response:**
```json
{ "success": true }
```

---

## MODULE: USERS

---

### GET /api/v1/users/me

**Purpose:** Fetch currently authenticated user with embedded profile.

**Auth:** Authenticated

**Response:**
```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "email": "user@example.com",
    "name": "Faisal Al Harbi",
    "role": "creator",
    "avatarUrl": "https://...",
    "creatorProgramStatus": "in_path",
    "createdAt": "2026-01-01T00:00:00Z",
    "profile": { /* Creator or Brand object */ }
  }
}
```

---

### PATCH /api/v1/users/me/password

**Purpose:** Change password from security settings tab.

**Auth:** Authenticated

**Request:**
```json
{
  "currentPassword": "OldPass123",
  "newPassword": "NewPass456!"
}
```

**Validation:**
- `currentPassword`: required
- `newPassword`: required, min 8 chars, strong

**Error Responses:**
- `401` – Current password incorrect

---

### DELETE /api/v1/users/me

**Purpose:** Delete account (danger zone in settings).

**Auth:** Authenticated

**Request:**
```json
{
  "confirmPassword": "ConfirmPass123"
}
```

*Performs soft delete (sets deletedAt). Data retained for 30 days per platform policy.*

**Error Responses:**
- `403` – Ambassador with active orders cannot delete account

---

### GET /api/v1/users/me/notification-preferences

**Purpose:** Load notification settings tab.

**Auth:** Authenticated

**Response:**
```json
{
  "success": true,
  "data": {
    "newOrders": true,
    "messages": true,
    "reviews": true,
    "marketing": false,
    "weeklyDigest": true,
    "pushNotifications": true,
    "emailNotifications": true,
    "smsNotifications": false
  }
}
```

---

### PUT /api/v1/users/me/notification-preferences

**Purpose:** Save notification preferences.

**Auth:** Authenticated

**Request:**
```json
{
  "newOrders": true,
  "messages": true,
  "reviews": false,
  "marketing": false,
  "weeklyDigest": true,
  "pushNotifications": true,
  "emailNotifications": true,
  "smsNotifications": false
}
```

---

## MODULE: CREATOR PROFILES

---

### GET /api/v1/creators

**Purpose:** Brand Explore page – get filtered and sorted list of creators.

**Auth:** Authenticated (Brand)

**Query Parameters:**
| Param | Type | Description |
|-------|------|-------------|
| `search` | string | Full-text search on name, bio, categories, city |
| `categories` | string[] | e.g. `categories[]=Food&categories[]=Travel` |
| `platforms` | string[] | `instagram`, `tiktok`, `youtube`, `facebook`, `snapchat` |
| `cities` | string[] | KSA cities |
| `dealTypes` | string[] | `paid`, `barter`, `hybrid` |
| `barterTypes` | string[] | Same canonical values as public profile categories, e.g. `FOOD`, `BEAUTY`, `TRAVEL`, `ENTERTAINMENT` |
| `minFollowers` | integer | |
| `maxFollowers` | integer | |
| `minRating` | float | |
| `minPrice` | integer | SAR |
| `maxPrice` | integer | SAR |
| `sortBy` | string | `trending`, `budget_friendly`, `top_rated`, `near_you` |
| `page` | integer | default 1 |
| `limit` | integer | default 20, max 50 |
| `ambassadorOnly` | boolean | Filter to platform ambassadors only |

**Response:**
```json
{
  "success": true,
  "data": {
    "creators": [
      {
        "id": "uuid",
        "username": "saraaesthetix",
        "name": "Sara Al Qahtani",
        "avatarUrl": "https://...",
        "coverImageUrl": "https://...",
        "bio": "...",
        "city": "Riyadh",
        "categories": ["Fashion", "Beauty"],
        "platforms": [
          {
            "platform": "instagram",
            "followers": 245000,
            "engagementRate": 6.2,
            "username": "saraaesthetix"
          }
        ],
        "totalFollowers": 245000,
        "avgEngagementRate": 6.2,
        "dealTypes": ["paid", "barter"],
        "barterTypes": ["BEAUTY", "TRAVEL"],
        "minPrice": 15000,
        "maxPrice": 85000,
        "responseTime": "Within 2 hours",
        "isVerified": true,
        "isTrending": true,
        "isFastResponder": true,
        "rating": 4.9,
        "totalReviews": 89,
        "completedDeals": 143,
        "ambassadorStatus": null
      }
    ],
    "total": 1248,
    "page": 1,
    "limit": 20,
    "totalPages": 63
  }
}
```

---

### GET /api/v1/creators/trending

**Purpose:** Homepage / brand dashboard "Recommended" section.

**Auth:** Public

**Query Parameters:**
- `limit` (integer, default 6)

**Response:** Same as creator list but limited.

---

### GET /api/v1/creators/barter-friendly

**Purpose:** Homepage barter section, explore filter shortcut.

**Auth:** Public

**Query Parameters:**
- `limit` (integer, default 6)

---

### GET /api/v1/creators/fast-responders

**Purpose:** Homepage section.

**Auth:** Public

**Query Parameters:**
- `limit` (integer, default 6)

---

### GET /api/v1/creators/by-city

**Purpose:** Geo-filtered creator section.

**Auth:** Public

**Query Parameters:**
- `city` (string, required)
- `limit` (integer, default 6)

---

### GET /api/v1/creators/:idOrUsername

**Purpose:** Creator public profile page (`/creator/[id]`).

**Auth:** Public (enhanced data for authenticated users)

**Response:**
```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "username": "saraaesthetix",
    "name": "Sara Al Qahtani",
    "avatarUrl": "https://...",
    "coverImageUrl": "https://...",
    "bio": "...",
    "city": "Riyadh",
    "categories": ["Fashion", "Beauty"],
    "platforms": [...],
    "totalFollowers": 245000,
    "avgEngagementRate": 6.2,
    "dealTypes": ["paid", "barter"],
    "barterTypes": ["BEAUTY", "TRAVEL"],
    "minPrice": 15000,
    "maxPrice": 85000,
    "responseTime": "Within 2 hours",
    "isVerified": true,
    "isTrending": true,
    "isFastResponder": true,
    "rating": 4.9,
    "totalReviews": 89,
    "completedDeals": 143,
    "contentPreviews": [...],
    "packages": [...],
    "reviews": [...],
    "ambassadorStatus": "approved",
    "isSaved": false
  }
}
```

---

### GET /api/v1/creators/me/profile

**Purpose:** Creator settings page – load own profile.

**Auth:** Authenticated (Creator)

**Response:** Full Creator object including payment settings, social accounts, preferences.

---

### PATCH /api/v1/creators/me/profile

**Purpose:** Save changes in creator settings Profile tab.

**Auth:** Authenticated (Creator)

**Request:**
```json
{
  "name": "Reem Al Otaibi",
  "username": "reemwellness",
  "bio": "Fashion & lifestyle creator...",
  "city": "Riyadh",
  "website": "https://reemwellness.sa",
  "niche": "Fashion & Lifestyle",
  "availabilityStatus": "Available this week",
  "responseTime": "Within 2 hours",
  "collaborationPrefs": "Fashion hauls, skincare tutorials...",
  "categories": ["Fashion", "Lifestyle"],
  "languages": ["English", "Arabic"]
}
```

**Validation:**
- `bio`: max 300 chars
- `username`: 3-50 chars, alphanumeric + underscore, unique

---

### PUT /api/v1/creators/me/social-accounts

**Purpose:** Creator settings Social tab – update connected accounts.

**Auth:** Authenticated (Creator)

**Request:**
```json
{
  "accounts": [
    {
      "platform": "instagram",
      "username": "reemwellness",
      "profileUrl": "https://instagram.com/reemwellness",
      "followers": 125000,
      "avgViews": 48000,
      "engagementRate": 5.6
    }
  ]
}
```

---

### PATCH /api/v1/creators/me/preferences

**Purpose:** Creator settings Preferences tab.

**Auth:** Authenticated (Creator)

**Request:**
```json
{
  "acceptsBarter": true,
  "acceptsHybridDeals": true,
  "preferredIndustries": "Fashion, Beauty, Wellness",
  "minimumBudget": 25000
}
```

---

### PATCH /api/v1/creators/me/payment-settings

**Purpose:** Creator settings Payments tab.

**Auth:** Authenticated (Creator)

**Request:**
```json
{
  "stcPayNumber": "+966551234567",
  "madaCard": "Mada **** 4582",
  "accountTitle": "Reem Al Otaibi",
  "ibanOrAccount": "SA0380000000608010167519",
  "applePayNumber": "+966551234567",
  "bankTransferIban": "SA0380000000608010167519"
}
```

---

## MODULE: BRAND PROFILES

---

### GET /api/v1/brands/me/profile

**Purpose:** Brand settings – load own profile.

**Auth:** Authenticated (Brand)

---

### PATCH /api/v1/brands/me/profile

**Purpose:** Brand settings – update profile.

**Auth:** Authenticated (Brand)

**Request:**
```json
{
  "name": "Noon Food KSA",
  "industry": "Food & Delivery",
  "website": "https://noon.com",
  "city": "Riyadh",
  "description": "Leading food delivery platform in KSA",
  "monthlyBudget": 1000000
}
```

---

## MODULE: PACKAGES

---

### GET /api/v1/packages

**Purpose:** Creator Package Studio page – list own packages with filters.

**Auth:** Authenticated (Creator)

**Query Parameters:**
| Param | Type | Description |
|-------|------|-------------|
| `search` | string | Title, tags, description search |
| `status` | string | `active`, `draft`, `paused`, `archived`, `under_review` |
| `dealType` | string | `paid`, `barter`, `hybrid` |
| `platform` | string | `instagram`, `tiktok`, `youtube` |
| `performance` | string | `top` (≥8%), `mid` (5-8%), `low` (<5%) conversion |
| `earningsBand` | string | `under25`, `25to50`, `50plus` |
| `sortBy` | string | `recent`, `views`, `conversion`, `orders` |
| `page` | integer | default 1 |
| `limit` | integer | default 20 |

**Response:**
```json
{
  "success": true,
  "data": {
    "packages": [...],
    "summary": {
      "active": 4,
      "drafts": 2,
      "archived": 1,
      "paused": 0,
      "underReview": 1,
      "monthlyProjection": 180000
    },
    "total": 8,
    "page": 1,
    "limit": 20
  }
}
```

---

### GET /api/v1/packages/:id

**Purpose:** Package preview page, brand browsing creator profile.

**Auth:** Public (own packages) / Brand (public packages)

**Response:** Full Package + PackageAnalytics

---

### POST /api/v1/packages

**Purpose:** Creator "Create Package" flow (package wizard).

**Auth:** Authenticated (Creator)

**Request:**
```json
{
  "title": "Instagram Story Pack",
  "shortDescription": "3 engaging Instagram Stories",
  "fullDescription": "Complete story package...",
  "category": "Fashion",
  "platform": "instagram",
  "dealType": "paid",
  "price": 25000,
  "deliverables": ["3 Stories", "Usage rights 30 days"],
  "deliveryDays": 5,
  "revisions": 2,
  "tags": ["fashion", "stories", "instagram"],
  "visibility": "public",
  "status": "draft"
}
```

**Validation:**
- `title`: required, 5-200 chars
- `price`: required if dealType is `paid` or `hybrid`, must be > 0
- `deliveryDays`: required, 1-90
- `platform`: required, valid enum
- `dealType`: required

**Error Responses:**
- `422` – Price required for paid deals

---

### PATCH /api/v1/packages/:id

**Purpose:** Edit package page.

**Auth:** Authenticated (Creator, must own package)

**Request:** Same fields as POST (partial)

---

### PATCH /api/v1/packages/:id/status

**Purpose:** Pause, resume, archive package actions from Package Studio dropdown.

**Auth:** Authenticated (Creator, must own package)

**Request:**
```json
{
  "status": "paused"
}
```

**Valid transitions:**
- `draft` → `active`, `archived`
- `active` → `paused`, `archived`
- `paused` → `active`, `archived`
- `archived` → (no transition)
- `under_review` → read-only until admin action

---

### POST /api/v1/packages/:id/duplicate

**Purpose:** Duplicate button in Package Studio creates a draft copy.

**Auth:** Authenticated (Creator)

**Response:** New package with `status: 'draft'` and title prefixed with "Copy of"

---

### DELETE /api/v1/packages/:id

**Purpose:** Delete package (inferred – not explicitly shown but standard CRUD).

**Auth:** Authenticated (Creator, must own package)

*Soft delete. Cannot delete if active orders reference this package.*

---

### GET /api/v1/packages/:id/analytics

**Purpose:** Package analytics inline display in Package Studio.

**Auth:** Authenticated (Creator, must own package)

**Response:**
```json
{
  "success": true,
  "data": {
    "views": 1240,
    "clicks": 380,
    "inquiries": 47,
    "conversionRate": 12.4,
    "completionRate": 96.0,
    "repeatBrands": 8,
    "engagementPerformance": 7.3,
    "ctr": 30.6,
    "inquiryToClickRate": 12.4
  }
}
```

---

### GET /api/v1/creators/:creatorId/packages

**Purpose:** Creator public profile page – list public packages.

**Auth:** Public

**Query Parameters:**
- `platform` (filter by platform)
- `dealType` (filter by deal type)

---

## MODULE: ORDERS

---

### POST /api/v1/orders

**Purpose:** Brand places an order on a creator package. Also triggered by Quick Deal offer acceptance.

**Auth:** Authenticated (Brand)

**Request:**
```json
{
  "packageId": "uuid",
  "dealType": "paid",
  "amount": 25000,
  "barterDetails": null,
  "message": "Looking forward to working together!",
  "deliveryDate": "2026-06-15"
}
```

**Validation:**
- `packageId`: required, must be active package
- `dealType`: must match package's allowed dealType
- `amount`: required for paid/hybrid, must be ≥ package minPrice

**Error Responses:**
- `404` – Package not found or not active
- `403` – Cannot order own package
- `422` – Invalid deal type for this package

---

### GET /api/v1/orders

**Purpose:** Orders list page for both creator and brand (role-scoped).

**Auth:** Authenticated

**Query Parameters:**
| Param | Type | Description |
|-------|------|-------------|
| `status` | string | Filter by order status |
| `search` | string | Search by creator/brand name or package name |
| `page` | integer | default 1 |
| `limit` | integer | default 20 |

*API automatically scopes to authenticated user's orders (creator sees own, brand sees own).*

**Response:**
```json
{
  "success": true,
  "data": {
    "orders": [
      {
        "id": "uuid",
        "orderNumber": "ORD-001",
        "package": { "id": "...", "title": "Instagram Story Pack" },
        "creator": { "id": "...", "name": "Reem Al Otaibi", "avatarUrl": "..." },
        "brand": { "id": "...", "name": "FreshMart", "logoUrl": "..." },
        "dealType": "paid",
        "amount": 25000,
        "status": "in_progress",
        "progress": 65,
        "deadlineDate": "2026-06-05",
        "createdAt": "2026-05-01T10:00:00Z",
        "deliverables": [...]
      }
    ],
    "counts": {
      "all": 8,
      "pending": 2,
      "in_progress": 3,
      "revision": 1,
      "completed": 2
    },
    "total": 8
  }
}
```

---

### GET /api/v1/orders/:id

**Purpose:** Expanded order detail view.

**Auth:** Authenticated (must be creator or brand on this order)

**Response:** Full order with deliverables, package details, creator/brand profiles

---

### PATCH /api/v1/orders/:id/status

**Purpose:** Status transitions:
- Creator: `pending → accepted`, `accepted → in_progress`, `in_progress → delivered`
- Brand: `delivered → review → completed` or `delivered → revision`
- Either: `→ cancelled` (with restrictions)

**Auth:** Authenticated (role-specific transitions enforced)

**Request:**
```json
{
  "status": "accepted"
}
```

**Valid Transitions:**
```
PENDING     → ACCEPTED (creator), CANCELLED (creator or brand)
ACCEPTED    → IN_PROGRESS (creator), CANCELLED (creator, within 24h)
IN_PROGRESS → DELIVERED (creator)
DELIVERED   → REVIEW (brand)
REVIEW      → COMPLETED (brand, approve delivery) | REVISION (brand)
REVISION    → IN_PROGRESS (creator, after revision)
COMPLETED   → (terminal)
CANCELLED   → (terminal)
```

**Error Responses:**
- `403` – Not authorized for this transition
- `422` – Invalid status transition

---

### PATCH /api/v1/orders/:id/progress

**Purpose:** Update order progress percentage.

**Auth:** Authenticated (Creator on own order)

**Request:**
```json
{ "progress": 75 }
```

---

### POST /api/v1/orders/:id/deliverables/:deliverableId/submit

**Purpose:** Creator submits a deliverable (Upload button in Orders → Submit Work).

**Auth:** Authenticated (Creator)

**Content-Type:** `multipart/form-data`

**Form Fields:**
- `file` (file, required if attachment)
- `note` (string, optional)

**Response:** Updated deliverable with `fileUrl`

---

### PATCH /api/v1/orders/:id/deliverables/:deliverableId/status

**Purpose:** Update individual deliverable status.

**Auth:** Authenticated (Creator on own order / Brand for review)

**Request:**
```json
{ "status": "completed" }
```

---

## MODULE: QUICK DEALS / OFFERS

---

### POST /api/v1/quick-deals

**Purpose:** Brand sends a Quick Deal offer from the creator card modal (`QuickDealModal`). Creates a conversation if none exists and sends an offer message.

**Auth:** Authenticated (Brand)

**Request:**
```json
{
  "creatorId": "uuid",
  "dealType": "barter",
  "amount": null,
  "barterDetails": "Luxury hotel stay for 2 nights in Jeddah",
  "barterCategory": "hotel",
  "estimatedBarterValue": 15000,
  "creatorExpectation": "1 reel, 3 stories, usage rights 30 days",
  "message": "Hi Sara, we'd love to collaborate for our grand opening!"
}
```

**Validation:**
- `creatorId`: required, valid creator
- `dealType`: required
- `amount`: required if dealType is `paid` or `hybrid`
- `barterDetails`: required if dealType is `barter` or `hybrid`
- `message`: required, max 1000 chars

**Response:**
```json
{
  "success": true,
  "data": {
    "conversationId": "uuid",
    "messageId": "uuid",
    "offerId": "uuid"
  }
}
```

---

### PATCH /api/v1/quick-deals/:offerId/respond

**Purpose:** Creator accepts or rejects a deal offer message in chat.

**Auth:** Authenticated (Creator, must be recipient of offer)

**Request:**
```json
{ "action": "accepted" }
```

**Valid actions:** `accepted`, `rejected`

*On acceptance, triggers order creation automatically (or prompts brand to formalize).*

---

## MODULE: CONVERSATIONS & MESSAGES

---

### GET /api/v1/conversations

**Purpose:** Messages page – conversation list (role-scoped).

**Auth:** Authenticated

**Query Parameters:**
- `search` (string, filter by participant name)
- `page` (integer, default 1)
- `limit` (integer, default 30)

**Response:**
```json
{
  "success": true,
  "data": {
    "conversations": [
      {
        "id": "uuid",
        "creator": { "id": "...", "name": "...", "avatarUrl": "...", "username": "..." },
        "brand": { "id": "...", "name": "...", "logoUrl": "..." },
        "lastMessage": {
          "id": "...",
          "content": "Hi! We loved your last post...",
          "type": "text",
          "createdAt": "2026-05-26T08:30:00Z"
        },
        "unreadCount": 2,
        "updatedAt": "2026-05-26T08:30:00Z"
      }
    ],
    "total": 12
  }
}
```

---

### POST /api/v1/conversations

**Purpose:** Create or find existing conversation between creator and brand (used when initiating from creator profile).

**Auth:** Authenticated (Brand)

**Request:**
```json
{ "creatorId": "uuid" }
```

**Response:** Returns existing conversation or creates new one (idempotent by creatorId+brandId).

---

### GET /api/v1/conversations/:id/messages

**Purpose:** Load messages for a selected conversation.

**Auth:** Authenticated (must be participant)

**Query Parameters:**
- `before` (ISO datetime, cursor for pagination)
- `limit` (integer, default 50)

**Response:**
```json
{
  "success": true,
  "data": {
    "messages": [
      {
        "id": "uuid",
        "conversationId": "uuid",
        "senderId": "uuid",
        "senderType": "brand",
        "content": "Hi! We loved your last post.",
        "type": "text",
        "isRead": true,
        "createdAt": "2026-05-26T08:00:00Z"
      },
      {
        "id": "uuid",
        "conversationId": "uuid",
        "senderId": "uuid",
        "senderType": "brand",
        "type": "offer",
        "offer": {
          "dealType": "paid",
          "amount": 35000,
          "message": "We'd like you for a campaign",
          "status": "pending"
        },
        "isRead": false,
        "createdAt": "2026-05-26T08:30:00Z"
      }
    ],
    "hasMore": false
  }
}
```

---

### POST /api/v1/conversations/:id/messages

**Purpose:** Send a text message in chat.

**Auth:** Authenticated (must be participant)

**Request:**
```json
{
  "content": "Sounds great! Let's discuss the campaign details.",
  "type": "text"
}
```

---

### POST /api/v1/conversations/:id/messages/offer

**Purpose:** Send an offer message within existing conversation (in-chat offer card).

**Auth:** Authenticated

**Request:**
```json
{
  "dealType": "hybrid",
  "amount": 20000,
  "barterDetails": "Free product worth SAR 5,000",
  "message": "Updated offer for our Ramadan campaign"
}
```

---

### POST /api/v1/conversations/:id/messages/attachment

**Purpose:** Send a file attachment in chat (Paperclip button).

**Auth:** Authenticated (must be participant)

**Content-Type:** `multipart/form-data`

**Form Fields:**
- `file` (required)

**Response:** Message object with `attachmentUrl`

---

### PATCH /api/v1/conversations/:id/read

**Purpose:** Mark all messages in conversation as read (auto-triggered on conversation open).

**Auth:** Authenticated (must be participant)

---

## MODULE: REVIEWS

---

### POST /api/v1/reviews

**Purpose:** Brand submits a review after order completion (displayed in order expanded view).

**Auth:** Authenticated (Brand)

**Request:**
```json
{
  "orderId": "uuid",
  "rating": 5,
  "comment": "Excellent work! Delivered everything on time."
}
```

**Validation:**
- `orderId`: required, order must be `completed`
- `rating`: required, integer 1-5
- `comment`: optional, max 1000 chars
- One review per order

**Error Responses:**
- `409` – Review already exists for this order
- `422` – Order not completed

---

### GET /api/v1/creators/:creatorId/reviews

**Purpose:** Display reviews on creator public profile.

**Auth:** Public

**Query Parameters:**
- `page`, `limit`

**Response:**
```json
{
  "success": true,
  "data": {
    "reviews": [
      {
        "id": "uuid",
        "brand": { "id": "...", "name": "FreshMart", "logoUrl": "..." },
        "rating": 5,
        "comment": "Outstanding quality!",
        "createdAt": "2026-04-10T00:00:00Z"
      }
    ],
    "averageRating": 4.9,
    "total": 89
  }
}
```

---

## MODULE: EARNINGS & WITHDRAWALS

---

### GET /api/v1/earnings/summary

**Purpose:** Creator Earnings page – top stats cards.

**Auth:** Authenticated (Creator)

**Query Parameters:**
- `period` (string: `7d`, `30d`, `90d`, `1y`)

**Response:**
```json
{
  "success": true,
  "data": {
    "totalEarnings": 485000,
    "availableBalance": 125000,
    "pendingBalance": 85000,
    "thisMonth": 145000,
    "lastMonth": 128000,
    "monthlyChange": 13.28
  }
}
```

---

### GET /api/v1/earnings/transactions

**Purpose:** Transaction History in Earnings page.

**Auth:** Authenticated (Creator)

**Query Parameters:**
- `type` (`earning`, `withdrawal`, `all`)
- `page`, `limit`
- `period` (`7d`, `30d`, `90d`, `1y`)

**Response:**
```json
{
  "success": true,
  "data": {
    "transactions": [
      {
        "id": "uuid",
        "type": "earning",
        "description": "Instagram Story Pack - FreshMart",
        "amount": 25000,
        "status": "completed",
        "createdAt": "2026-05-25T10:00:00Z"
      },
      {
        "id": "uuid",
        "type": "withdrawal",
        "description": "Withdrawal - STC Pay",
        "amount": -50000,
        "status": "completed",
        "createdAt": "2026-05-23T10:00:00Z"
      }
    ],
    "total": 24,
    "page": 1
  }
}
```

---

### GET /api/v1/earnings/transactions/export

**Purpose:** Export button on Earnings page.

**Auth:** Authenticated (Creator)

**Query Parameters:**
- `period` (`30d`, `90d`, `1y`)
- `format` (`csv`, `pdf`)  *(assume CSV default)*

**Response:** File download (CSV/PDF)

---

### GET /api/v1/payout-methods

**Purpose:** Load payout methods in Earnings page and creator settings Payments tab.

**Auth:** Authenticated (Creator)

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "id": "uuid",
      "type": "stcpay",
      "name": "STC Pay",
      "details": "**** 1234",
      "isDefault": true
    }
  ]
}
```

---

### POST /api/v1/payout-methods

**Purpose:** Add New payout method button.

**Auth:** Authenticated (Creator)

**Request:**
```json
{
  "type": "bank_transfer",
  "name": "Al Rajhi Bank",
  "accountDetails": "SA0380000000608010167519",
  "isDefault": false
}
```

---

### PATCH /api/v1/payout-methods/:id

**Purpose:** Update or set default payout method.

**Auth:** Authenticated (Creator, must own method)

---

### DELETE /api/v1/payout-methods/:id

**Purpose:** Remove payout method.

**Auth:** Authenticated (Creator)

**Error Responses:**
- `422` – Cannot delete default method with pending withdrawals

---

### POST /api/v1/withdrawals

**Purpose:** "Withdraw" button in Earnings page modal.

**Auth:** Authenticated (Creator)

**Request:**
```json
{
  "amount": 50000,
  "payoutMethodId": "uuid"
}
```

**Validation:**
- `amount`: required, must be ≤ availableBalance, min 1000 SAR
- `payoutMethodId`: required, must belong to creator

**Error Responses:**
- `422` – Insufficient available balance
- `422` – Amount below minimum withdrawal

---

### GET /api/v1/withdrawals

**Purpose:** Withdrawal history (filterable tab in transactions).

**Auth:** Authenticated (Creator)

---

## MODULE: AMBASSADOR PROGRAM

---

### GET /api/v1/ambassador/application

**Purpose:** Creator Ambassador Program page – load own application status.

**Auth:** Authenticated (Creator)

**Response:**
```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "status": "under_review",
    "submittedAt": "2026-05-01T10:00:00Z",
    "updatedAt": "2026-05-10T10:00:00Z",
    "verificationSteps": {
      "identityVerified": true,
      "engagementVerified": true,
      "contentReviewPassed": false,
      "backgroundCheckPassed": false
    },
    "notes": "Application under review by our team.",
    "rejectionReason": null,
    "approvedAt": null
  }
}
```

*Returns `null` data if no application exists.*

---

### POST /api/v1/ambassador/application

**Purpose:** "Apply for Program" button submission.

**Auth:** Authenticated (Creator)

**Request:** *(no body required – application is auto-populated from creator profile)*

**Validation:**
- Creator must not have an active/approved application
- *(Eligibility pre-check can be done server-side or client delegates)*

**Response:** New AmbassadorApplication with `status: 'submitted'`

**Error Responses:**
- `409` – Application already exists
- `422` – Cannot reapply within 30 days of rejection

---

### GET /api/v1/ambassador/score

**Purpose:** Creator Dashboard and Ambassador Program page – display score, tier, percentile.

**Auth:** Authenticated (Creator)

**Response:**
```json
{
  "success": true,
  "data": {
    "creatorId": "uuid",
    "score": {
      "total": 78,
      "deliveryScore": 32,
      "accountAgeScore": 12,
      "ratingScore": 23,
      "cancellationScore": 8,
      "profileCompletenessScore": 9,
      "consistencyScore": 4
    },
    "tier": "verified_ambassador",
    "percentileRank": 84,
    "strengths": ["Excellent delivery track record", "Strong audience engagement"],
    "improvements": ["Expand to 3+ social platforms for broader reach"],
    "journeyMilestones": {
      "joinedPlatform": "2024-06-01T00:00:00Z",
      "firstDelivery": "2024-06-16T00:00:00Z",
      "consistencyAchieved": "2024-08-01T00:00:00Z",
      "ambassadorEligible": "2024-09-01T00:00:00Z"
    },
    "calculatedAt": "2026-05-26T09:00:00Z"
  }
}
```

*Score is recalculated server-side on a schedule (nightly or on significant events like order completion). Frontend scoring logic should be replicated exactly on the backend.*

---

### GET /api/v1/ambassador/application/:id *(Admin)*

**Purpose:** Admin reviewing an application.

**Auth:** Authenticated (PLATFORM_ADMIN)

---

### GET /api/v1/ambassador/applications *(Admin)*

**Purpose:** Admin list all applications with filters.

**Auth:** Authenticated (PLATFORM_ADMIN)

**Query Parameters:**
- `status` (filter)
- `page`, `limit`

---

### PATCH /api/v1/ambassador/applications/:id *(Admin)*

**Purpose:** Admin approves, rejects, or updates verification steps.

**Auth:** Authenticated (PLATFORM_ADMIN)

**Request:**
```json
{
  "status": "approved",
  "verificationSteps": {
    "identityVerified": true,
    "engagementVerified": true,
    "contentReviewPassed": true,
    "backgroundCheckPassed": true
  },
  "notes": "Active ambassador in good standing.",
  "rejectionReason": null
}
```

*On approval: sets `User.creatorProgramStatus = 'active_ambassador'`, sets `approvedAt`, triggers welcome notification.*

---

### GET /api/v1/ambassador/ambassadors

**Purpose:** Brand Ambassadors page – list all approved platform ambassadors.

**Auth:** Public

**Query Parameters:**
- `page`, `limit`

**Response:** Creator list with `ambassadorStatus`, `commissionPercentage`, `performanceScore`, `isExclusive`

---

## MODULE: SAVED CREATORS

---

### GET /api/v1/saved-creators

**Purpose:** Brand "Saved" page and dashboard "Saved Creators" widget.

**Auth:** Authenticated (Brand)

**Response:**
```json
{
  "success": true,
  "data": {
    "creators": [...],
    "total": 12
  }
}
```

---

### POST /api/v1/saved-creators/:creatorId

**Purpose:** Save (bookmark/heart) a creator from creator card.

**Auth:** Authenticated (Brand)

**Response:**
```json
{ "success": true, "data": { "saved": true } }
```

---

### DELETE /api/v1/saved-creators/:creatorId

**Purpose:** Unsave a creator.

**Auth:** Authenticated (Brand)

---

## MODULE: ANALYTICS

---

### GET /api/v1/analytics/creator/dashboard

**Purpose:** Creator Dashboard stats cards (earnings, active orders, profile views, rating).

**Auth:** Authenticated (Creator)

**Response:**
```json
{
  "success": true,
  "data": {
    "totalEarnings": 485000,
    "earningsChange": 12.5,
    "activeOrders": 8,
    "ordersChange": -2,
    "profileViews": 1250,
    "viewsChange": 23.1,
    "rating": 4.9,
    "reviewCount": 47,
    "ambassadorPercentile": 84,
    "newMessages": 3,
    "monthlyGoal": {
      "earningsTarget": 600000,
      "earningsCurrent": 485000,
      "ordersTarget": 10,
      "ordersCurrent": 8
    }
  }
}
```

---

### GET /api/v1/analytics/brand/dashboard

**Purpose:** Brand Dashboard stats cards.

**Auth:** Authenticated (Brand)

**Response:**
```json
{
  "success": true,
  "data": {
    "totalSpent": 785000,
    "spentChange": 18.3,
    "activeOrders": 5,
    "ordersChange": 2,
    "creatorsWorkedWith": 23,
    "creatorsChange": 4,
    "avgRating": 4.8,
    "savedCreatorsCount": 12,
    "monthlyBudgetUsed": 785000,
    "monthlyBudgetLimit": 1000000
  }
}
```

---

### GET /api/v1/analytics/creator/insights

**Purpose:** Creator Insights page – package funnel health.

**Auth:** Authenticated (Creator)

**Query Parameters:**
- `period` (string: `6m`, `1y`)

**Response:**
```json
{
  "success": true,
  "data": {
    "totals": {
      "packageViews": 8420,
      "packageViewsChange": 12.4,
      "inquiries": 312,
      "inquiriesChange": 8.1,
      "repeatBrands": 18,
      "repeatBrandsChange": 4.0,
      "avgConversionRate": 9.6,
      "avgConversionChange": 1.2
    },
    "monthlyInquiryTrend": [
      { "month": "Jan", "count": 42 },
      { "month": "Feb", "count": 51 }
    ],
    "platformContribution": [
      { "platform": "instagram", "percentage": 45, "note": "Highest inquiry conversion" },
      { "platform": "tiktok", "percentage": 35, "note": "Fastest top-of-funnel growth" },
      { "platform": "youtube", "percentage": 20, "note": "Strong high-value package intent" }
    ],
    "topPackages": [...]
  }
}
```

---

### GET /api/v1/analytics/creator/performance

**Purpose:** Creator Package Performance page – per-package breakdown table.

**Auth:** Authenticated (Creator)

**Response:**
```json
{
  "success": true,
  "data": {
    "packages": [
      {
        "packageId": "uuid",
        "title": "Instagram Story Pack",
        "views": 1240,
        "clicks": 380,
        "inquiries": 47,
        "conversionRate": 12.4,
        "completionRate": 96.0,
        "repeatBrands": 8,
        "ctr": 30.6,
        "inquiryToClickRate": 12.4,
        "efficiencyScore": 88
      }
    ]
  }
}
```

---

### GET /api/v1/analytics/brand/campaigns

**Purpose:** Brand Analytics / Campaign Analytics page.

**Auth:** Authenticated (Brand)

**Query Parameters:**
- `period` (string: `30d`, `90d`, `1y`)

**Response:**
```json
{
  "success": true,
  "data": {
    "totalReach": 12400000,
    "avgEngagementRate": 5.8,
    "creatorsActive": 32,
    "monthlySpend": 945000,
    "topCities": [
      { "city": "Jeddah", "reachPercentage": 34 },
      { "city": "Riyadh", "reachPercentage": 29 },
      { "city": "Dammam", "reachPercentage": 18 }
    ],
    "dealMix": {
      "paid": 52,
      "hybrid": 33,
      "barter": 15
    }
  }
}
```

---

## MODULE: FILE UPLOADS

---

### POST /api/v1/uploads/avatar

**Purpose:** Creator/Brand avatar upload (settings Profile tab – "Change Photo").

**Auth:** Authenticated

**Content-Type:** `multipart/form-data`

**Form Fields:**
- `file`: image file (JPG, PNG, WebP, max 5MB)

**Response:**
```json
{
  "success": true,
  "data": {
    "url": "https://cdn.zingzing.sa/avatars/uuid.webp"
  }
}
```

---

### POST /api/v1/uploads/cover-image

**Purpose:** Creator cover/banner image upload.

**Auth:** Authenticated (Creator)

**Content-Type:** `multipart/form-data`

**Form Fields:**
- `file`: image (JPG, PNG, WebP, max 10MB, recommended 1200×400px)

---

### POST /api/v1/uploads/content-preview

**Purpose:** Upload content preview items to creator portfolio.

**Auth:** Authenticated (Creator)

**Content-Type:** `multipart/form-data`

**Form Fields:**
- `file`: image or video (JPG, PNG, MP4, max 50MB)
- `platform`: ENUM(Platform)

---

### POST /api/v1/uploads/package-thumbnail

**Purpose:** Package thumbnail image during package creation/edit.

**Auth:** Authenticated (Creator)

**Content-Type:** `multipart/form-data`

**Form Fields:**
- `file`: image (JPG, PNG, WebP, max 5MB)

---

### POST /api/v1/uploads/deliverable

**Purpose:** Creator submits deliverable file for an order.

**Auth:** Authenticated (Creator)

**Content-Type:** `multipart/form-data`

**Form Fields:**
- `file`: any file (images, video, PDF, zip, max 500MB)
- `orderId`: UUID
- `deliverableId`: UUID

---

### POST /api/v1/uploads/brand-logo

**Purpose:** Brand logo upload in brand settings.

**Auth:** Authenticated (Brand)

**Content-Type:** `multipart/form-data`

**Form Fields:**
- `file`: image (JPG, PNG, WebP, SVG, max 5MB)

---

---

# 6. Frontend → Backend Mapping

```
app/login/page.tsx
→ POST /api/v1/auth/login
→ POST /api/v1/auth/send-otp
→ POST /api/v1/auth/verify-otp

app/signup/page.tsx
→ POST /api/v1/auth/register

app/forgot-password/page.tsx
→ POST /api/v1/auth/forgot-password
→ POST /api/v1/auth/reset-password

app/creator/dashboard/page.tsx
→ GET /api/v1/analytics/creator/dashboard
→ GET /api/v1/ambassador/score
→ GET /api/v1/orders?limit=3&status=recent

app/brand/dashboard/page.tsx
→ GET /api/v1/analytics/brand/dashboard
→ GET /api/v1/orders?limit=3&status=active
→ GET /api/v1/creators/trending?limit=4
→ GET /api/v1/saved-creators?limit=3

app/brand/explore/page.tsx
→ GET /api/v1/creators (with all filter params)

app/brand/ambassadors/page.tsx
→ GET /api/v1/ambassador/ambassadors
→ GET /api/v1/creators (independent creators)

app/creator/packages/page.tsx
→ GET /api/v1/packages
→ PATCH /api/v1/packages/:id/status (pause/resume/archive)
→ POST /api/v1/packages/:id/duplicate

app/creator/packages/new/page.tsx
→ POST /api/v1/packages
→ POST /api/v1/uploads/package-thumbnail

app/creator/packages/:id/edit/page.tsx
→ GET /api/v1/packages/:id
→ PATCH /api/v1/packages/:id

app/creator/orders/page.tsx
→ GET /api/v1/orders
→ PATCH /api/v1/orders/:id/status
→ POST /api/v1/orders/:id/deliverables/:dId/submit

app/brand/orders/page.tsx
→ GET /api/v1/orders
→ PATCH /api/v1/orders/:id/status (approve/revision)
→ POST /api/v1/reviews

app/creator/earnings/page.tsx
→ GET /api/v1/earnings/summary
→ GET /api/v1/earnings/transactions
→ GET /api/v1/payout-methods
→ POST /api/v1/withdrawals
→ POST /api/v1/payout-methods
→ GET /api/v1/earnings/transactions/export

app/messages/page.tsx
→ GET /api/v1/conversations
→ POST /api/v1/conversations
→ GET /api/v1/conversations/:id/messages
→ POST /api/v1/conversations/:id/messages
→ POST /api/v1/conversations/:id/messages/offer
→ POST /api/v1/conversations/:id/messages/attachment
→ PATCH /api/v1/conversations/:id/read

components/quick-deal-modal.tsx
→ POST /api/v1/quick-deals

app/creator/ambassador-program/page.tsx
→ GET /api/v1/ambassador/application
→ GET /api/v1/ambassador/score
→ POST /api/v1/ambassador/application

app/creator/insights/page.tsx
→ GET /api/v1/analytics/creator/insights

app/creator/performance/page.tsx
→ GET /api/v1/analytics/creator/performance

app/creator/settings/page.tsx
→ GET /api/v1/creators/me/profile
→ PATCH /api/v1/creators/me/profile
→ PUT /api/v1/creators/me/social-accounts
→ PATCH /api/v1/creators/me/payment-settings
→ PATCH /api/v1/creators/me/preferences
→ GET /api/v1/users/me/notification-preferences
→ PUT /api/v1/users/me/notification-preferences
→ PATCH /api/v1/users/me/password
→ DELETE /api/v1/users/me
→ POST /api/v1/uploads/avatar
→ POST /api/v1/uploads/cover-image

app/brand/analytics/page.tsx
→ GET /api/v1/analytics/brand/campaigns

app/brand/saved/page.tsx
→ GET /api/v1/saved-creators
→ DELETE /api/v1/saved-creators/:creatorId

components/creator-card.tsx (save button)
→ POST /api/v1/saved-creators/:creatorId
→ DELETE /api/v1/saved-creators/:creatorId

app/creator/[id]/page.tsx
→ GET /api/v1/creators/:idOrUsername
→ GET /api/v1/creators/:creatorId/packages
→ GET /api/v1/creators/:creatorId/reviews
```

---

# 7. Workflow & State Transition Analysis

## Order Lifecycle
```
PENDING
  ├─► ACCEPTED      (creator accepts the order)
  │     └─► IN_PROGRESS  (creator begins work)
  │           └─► DELIVERED   (creator marks work done)
  │                 ├─► REVIEW       (brand starts reviewing)
  │                 │     ├─► COMPLETED   (brand approves → triggers payment release)
  │                 │     └─► REVISION    (brand requests changes)
  │                 │           └─► IN_PROGRESS (creator revises)
  │                 └─► COMPLETED   (auto-complete after 72h if no brand action)
  └─► CANCELLED     (by creator before acceptance, or by brand within 24h)
```
*Payment is held in escrow on order placement, released on COMPLETED.*

## Ambassador Application Flow
```
DRAFT (optional) → SUBMITTED
  └─► UNDER_REVIEW
        ├─► VERIFIED (all steps pass)
        │     └─► APPROVED (admin final approval)
        │           → User.creatorProgramStatus = 'active_ambassador'
        │           → Tier computed and persisted
        └─► REJECTED (with rejectionReason)
              → Creator can reapply after 30 days
```

**Verification Steps (sequential, admin-controlled):**
1. identityVerified – Saudi ID/Iqama check
2. engagementVerified – Social metrics verification via API or manual
3. contentReviewPassed – Brand safety check
4. backgroundCheckPassed – Compliance review

## Creator Ambassador Score Tier Transitions
```
Score 0-40   → rising_creator
Score 41-70  → emerging_ambassador
Score 71-90  → verified_ambassador
Score 91-100 → elite_ambassador
```
*Score is recalculated nightly and on: order completion, review received, profile update.*

## Package Status Flow
```
DRAFT ──► ACTIVE ──► PAUSED ──► ACTIVE
            │           │
            └─► ARCHIVED └─► ARCHIVED
DRAFT ──► UNDER_REVIEW (when submitted for admin review)
UNDER_REVIEW ──► ACTIVE (admin approves) | DRAFT (rejected)
```

## Quick Deal → Order Flow
```
Brand sends QuickDeal offer
  → Creates Conversation (if not existing)
  → Creates Message{type: 'offer'}
  → Creator accepts offer in chat
  → Order auto-created with status PENDING
  → Brand notified to confirm and pay
  → Order status → ACCEPTED
```

## Withdrawal Flow
```
Creator requests withdrawal
  → Withdrawal status: PENDING
  → System validates available balance
  → Payment processor initiated
  → Withdrawal status: PROCESSING
  → Payment confirmed
  → Withdrawal status: COMPLETED
  → Wallet.availableBalance decremented
  → Transaction record created (type: 'withdrawal')
```

## Messaging Offer Flow
```
Offer message created with status: PENDING
Creator views offer in chat
  ├─► Accepts: offer.status = ACCEPTED → triggers order or notification
  └─► Rejects: offer.status = REJECTED → no further action
```

---

# 8. Realtime / WebSocket Requirements

## Required Realtime Features

### 1. Messaging (Critical)
- New message delivery in open conversations
- Typing indicators (`user_typing`, `user_stop_typing`)
- Read receipts (double-checkmark updates)
- Unread count updates in conversation list
- New conversation notification

**Recommended Socket Events:**
```
Client → Server:
  join_conversation   { conversationId }
  leave_conversation  { conversationId }
  send_message        { conversationId, content, type }
  typing_start        { conversationId }
  typing_stop         { conversationId }
  mark_read           { conversationId }

Server → Client:
  new_message         { message }
  message_read        { conversationId, readBy }
  user_typing         { conversationId, userId }
  user_stop_typing    { conversationId, userId }
  unread_count_update { conversationId, count }
```

### 2. Notifications (High Priority)
- New order received (creator)
- Order status changed (brand + creator)
- New message received (background)
- Offer accepted/rejected
- Ambassador application status update

**Recommended Socket Events:**
```
Server → Client:
  notification        { id, type, title, body, data, createdAt }
  notification_count  { unreadCount }
```

### 3. Order Status Updates
- Real-time progress updates on order cards

### 4. Dashboard Live Updates *(optional, lower priority)*
- Profile view counter increment
- New inquiry on package

## Socket Architecture Recommendation
- Use **Socket.IO** or equivalent WebSocket library
- Authenticate socket connection with JWT (`Authorization` header or handshake query)
- Namespace: `/ws`
- Rooms: `user:{userId}`, `conversation:{conversationId}`

---

# 9. File Upload Requirements

| Upload Type | Endpoint | Formats | Max Size | Association |
|-------------|----------|---------|----------|-------------|
| User Avatar | `POST /api/v1/uploads/avatar` | JPG, PNG, WebP | 5MB | User.avatarUrl |
| Creator Cover | `POST /api/v1/uploads/cover-image` | JPG, PNG, WebP | 10MB | Creator.coverImageUrl |
| Brand Logo | `POST /api/v1/uploads/brand-logo` | JPG, PNG, WebP, SVG | 5MB | Brand.logoUrl |
| Package Thumbnail | `POST /api/v1/uploads/package-thumbnail` | JPG, PNG, WebP | 5MB | Package.thumbnailUrl |
| Content Preview | `POST /api/v1/uploads/content-preview` | JPG, PNG, MP4 | 50MB | ContentPreview |
| Order Deliverable | `POST /api/v1/uploads/deliverable` | Any | 500MB | Deliverable.fileUrl |
| Message Attachment | `POST /api/v1/conversations/:id/messages/attachment` | Any | 100MB | Message.attachmentUrl |

**Storage:** Use S3-compatible object storage (AWS S3, MinIO). Store CDN URLs in DB.

**Response Format (all upload endpoints):**
```json
{
  "success": true,
  "data": {
    "url": "https://cdn.zingzing.sa/uploads/uuid.webp",
    "thumbnailUrl": "https://cdn.zingzing.sa/uploads/uuid_thumb.webp"
  }
}
```

---

# 10. Dashboard & Analytics Requirements

## Creator Dashboard Aggregates
- **Total Earnings** – SUM of completed transaction earnings
- **Earnings % Change** – vs previous equivalent period
- **Active Orders** – COUNT orders where status IN (`accepted`, `in_progress`, `delivered`, `review`, `revision`)
- **Orders Change** – vs previous period
- **Profile Views** – tracked via view events (increment counter on creator profile GET by non-owner)
- **Views Change** – vs previous period
- **Rating** – weighted average of all reviews
- **Review Count** – total reviews
- **New Messages** – unread message count
- **Monthly Goal** – configurable target; current earnings/orders this calendar month

## Brand Dashboard Aggregates
- **Total Spent** – SUM of completed order amounts
- **Spent Change** – vs previous period
- **Active Campaigns** – COUNT active orders
- **Creators Worked With** – COUNT distinct creatorIds with completed orders
- **Avg Rating Given** – AVG rating in reviews submitted by this brand
- **Monthly Budget Used** / **Monthly Budget Remaining**
- **Recommended Creators** – top 4 creators by trending+rating for brand's industry

## Creator Insights Aggregates
- **Package Views** – SUM of PackageAnalytics.views across all packages
- **Inquiries** – SUM of PackageAnalytics.inquiries
- **Repeat Brands** – SUM of PackageAnalytics.repeatBrands
- **Avg Conversion Rate** – AVG of PackageAnalytics.conversionRate
- **Monthly Inquiry Trend** – grouped by month for last 6 months
- **Platform Contribution** – % of packages per platform

## Creator Performance Table
Per package: views, clicks, inquiries, conversion rate, completion rate, repeat brands, CTR, inquiry-to-click rate, efficiency score (formula: `min(100, round(conversionRate * 6 + completionRate * 0.4))`)

## Brand Campaign Analytics
- **Total Reach** – aggregate estimated reach from all completed orders
- **Avg Engagement Rate** – AVG engagement rates of hired creators
- **Creators Active** – distinct creators with active/completed orders
- **Monthly Spend** – total spend this month
- **Top Cities** – grouped reach by creator city
- **Deal Mix** – % breakdown by dealType

## Ambassador Score Computation Schedule
- Recalculate on:
  - Order marked COMPLETED
  - Review received
  - Creator profile updated
  - Daily background job (nightly at 2AM KSA time)

---

# 11. Enumerations

```
UserRole:               creator | brand
Platform:               instagram | tiktok | youtube | facebook | snapchat
DealType:               paid | barter | hybrid
BarterType:             food | hotel | salon | events | products
BarterCategory:         food | hotel | salon | events | products | services | travel | education
City:                   Riyadh | Jeddah | Dammam | Mecca | Medina | Khobar | Tabuk
OrderStatus:            pending | accepted | in_progress | delivered | review | revision | completed | cancelled
PackageStatus:          active | draft | paused | archived | under_review
PackageVisibility:      public | private
AmbassadorStatus:       approved | pending_review | rejected | under_review | suspended
AmbassadorAppStatus:    draft | submitted | under_review | verified | approved | rejected
AmbassadorTier:         rising_creator | emerging_ambassador | verified_ambassador | elite_ambassador
CreatorProgramStatus:   none | in_path | active_ambassador
TransactionType:        earning | withdrawal | refund | platform_fee
TransactionStatus:      pending | completed | failed
WithdrawalStatus:       pending | processing | completed | failed
PayoutMethodType:       stcpay | mada | applepay | bank_transfer
MessageType:            text | offer | system | attachment
OfferStatus:            pending | accepted | rejected
DeliverableStatus:      pending | in_progress | completed | revision | review
```

---

# 12. API Response Wrapper Standard

All endpoints return a consistent response envelope:

**Success:**
```json
{
  "success": true,
  "data": { ... },
  "meta": {
    "page": 1,
    "limit": 20,
    "total": 248,
    "totalPages": 13
  }
}
```
*`meta` only included for paginated endpoints.*

**Error:**
```json
{
  "success": false,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Validation failed",
    "details": [
      { "field": "email", "message": "Email is required" }
    ]
  }
}
```

**Standard HTTP Status Codes:**
- `200` – Success
- `201` – Created
- `204` – No Content (DELETE)
- `400` – Bad Request / Validation Error
- `401` – Unauthenticated
- `403` – Forbidden
- `404` – Not Found
- `409` – Conflict
- `422` – Unprocessable Entity
- `429` – Rate Limited
- `500` – Server Error

---

# 13. Authentication & Security

- **Token Type:** JWT Bearer tokens
- **Header:** `Authorization: Bearer <accessToken>`
- **Access Token Expiry:** 15 minutes
- **Refresh Token Expiry:** 30 days (rotated on use)
- **OTP Expiry:** 5 minutes
- **OTP Rate Limit:** 3 attempts per 10 minutes per phone number
- **Password Reset Token Expiry:** 1 hour

## Commission Structure *(persisted in DB)*
```json
{
  "independentCreatorCommission": 0.10,
  "platformAmbassadorCommission": 0.15
}
```
*Commission is deducted from creator earnings at order completion automatically.*

---

# 14. Missing Backend Assumptions

> The following are intelligent assumptions where the frontend prototype lacks clarity. All are clearly marked as assumptions.

1. **[ASSUMPTION]** Payment gateway integration: The platform holds funds in escrow on order placement. Brands are charged when an order is accepted. Funds are released to creator when order reaches `COMPLETED`. Platform fee (10-15%) is deducted at release time.

2. **[ASSUMPTION]** Profile view tracking: A separate `profile_view` event is logged (creator ID, viewer ID/IP, timestamp) to compute the profile views metric on the creator dashboard and settings analytics tab.

3. **[ASSUMPTION]** The `under_review` package status is triggered when a creator submits a draft for admin review. An admin endpoint (`PATCH /api/v1/admin/packages/:id/review`) approves or rejects it. Frontend does not show this admin UI, so it is inferred.

4. **[ASSUMPTION]** Recommended creators on the brand dashboard are served by a recommendation engine or simple algorithm based on: brand's previous order categories + creator rating + trending flag.

5. **[ASSUMPTION]** Ambassador score percentile is computed by bucketing all creator scores and computing relative rank. This should be a cached/materialized view, recomputed nightly.

6. **[ASSUMPTION]** Auto-completion of orders: If a brand does not take action within 72 hours of a creator delivering, the order auto-completes. This requires a scheduled background job.

7. **[ASSUMPTION]** A `Notification` entity is stored for in-app notification feeds. Schema: `{id, userId, type, title, body, data(JSON), isRead, createdAt}`. Frontend shows unread dots but no explicit full notification center page is detected – assumed to be delivered via WebSocket only in this version.

8. **[ASSUMPTION]** The `SocialAccount` verification flag (`isVerified`) implies a social media verification integration (e.g., OAuth connect or manual admin verification). In v1, admin sets this manually as part of ambassador engagement verification.

9. **[ASSUMPTION]** Password-less phone-only users (loginWithPhone) have no email. An email can be added later via `PATCH /api/v1/users/me` to enable full platform features.

10. **[ASSUMPTION]** Ambassador's `isExclusive` flag prevents them from accepting orders from brands in competing categories. This may be enforced at the order placement validation layer.

11. **[ASSUMPTION]** The platform supports Arabic and English (inferred from creator language settings). All API responses should support an `Accept-Language: ar` header for translated error messages and system messages.

12. **[ASSUMPTION]** Package `under_review` status is entered when a creator with amateur standing submits a new package. Ambassador-tier creators may have packages auto-approved.
```
