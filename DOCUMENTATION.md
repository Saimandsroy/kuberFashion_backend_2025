# Workflow Management Backend - Technical Documentation

## Table of Contents

1. [System Overview](#system-overview)
2. [Architecture](#architecture)
3. [Database Design](#database-design)
4. [Workflow State Machine](#workflow-state-machine)
5. [API Reference](#api-reference)
6. [Authentication & Authorization](#authentication--authorization)
7. [User Roles & Responsibilities](#user-roles--responsibilities)
8. [Data Flow](#data-flow)
9. [Wallet & Payment System](#wallet--payment-system)
10. [Error Handling](#error-handling)

---

## System Overview

The Workflow Management Backend is a comprehensive platform designed to streamline the lifecycle of SEO link-building and content publishing. It connects five distinct user roles through a linear dependency pipeline with a sophisticated state machine.

### Core Workflow Loop

```
Topic Discovery (Team) 
  → Manager Approval 
    → Content Writing (Writer) 
      → Manager Approval 
        → Blogger Posting 
          → Final Verification (Manager) 
            → Payment (Automatic)
```

### Key Features

- **Role-Based Access Control (RBAC)**: 5 user roles with granular permissions
- **State Machine Workflow**: 11-stage workflow with strict validation
- **Automatic Payments**: Blogger wallet credits upon task completion
- **Withdrawal Management**: Request/approval system for bloggers
- **Audit Trail**: Complete history of all status transitions
- **Bulk Operations**: CSV upload for website inventory

---

## Architecture

### High-Level Architecture

```
┌─────────────┐
│   Client    │
│ (Frontend)  │
└──────┬──────┘
       │ HTTP/REST
       │ JWT Auth
┌──────▼──────────────────────────────────┐
│        Express.js Server                │
│  ┌────────────┐    ┌─────────────┐    │
│  │ Middleware │───▶│  Routes     │    │
│  │  - CORS    │    │  - Auth     │    │
│  │  - JWT     │    │  - Admin    │    │
│  │  - RBAC    │    │  - Manager  │    │
│  │  - Errors  │    │  - Team     │    │
│  └────────────┘    │  - Writer   │    │
│                     │  - Blogger  │    │
│                     └──────┬──────┘    │
│                            │            │
│                     ┌──────▼──────┐    │
│                     │ Controllers │    │
│                     └──────┬──────┘    │
│                            │            │
│  ┌─────────────────────────▼─────────┐ │
│  │          Models Layer             │ │
│  │  - User  - Task  - Transaction   │ │
│  │  - Website  - Config              │ │
│  └───────────────────┬───────────────┘ │
└────────────────────────────────────────┘
                       │
                ┌──────▼──────┐
                │ PostgreSQL  │
                │  Database   │
                └─────────────┘
```

### Technology Stack

- **Runtime**: Node.js v14+
- **Framework**: Express.js 4.x
- **Database**: PostgreSQL 12+
- **Authentication**: JWT (jsonwebtoken)
- **Password Hashing**: bcryptjs
- **File Upload**: Multer
- **CSV Parsing**: csv-parser

---

## Database Design

### Entity Relationship Diagram

```
┌─────────────┐       ┌──────────────┐       ┌──────────────┐
│    Users    │       │   Websites   │       │    Tasks     │
├─────────────┤       ├──────────────┤       ├──────────────┤
│ id (PK)     │◀─┐    │ id (PK)      │◀──┐   │ id (PK)      │
│ username    │  │    │ domain_url   │   │   │ website_id   │
│ email       │  │    │ category     │   └───│ (FK)         │
│ password    │  │    │ da_pa_score  │       │ created_by   │
│ role (ENUM) │  │    │ status       │   ┌───│ (FK)         │
│ wallet_bal  │  │    └──────────────┘   │   │ assigned_    │
│ is_active   │  │                       │   │ writer_id(FK)│
│ created_at  │  │                       │   │ assigned_    │
└─────────────┘  │                       │   │ blogger_id   │
                 │                       │   │ (FK)         │
                 └───────────────────────┘   │ current_     │
                                             │ status(ENUM) │
                 ┌───────────────────────────│ content_body │
                 │                           │ live_url     │
                 │                           │ payment_amt  │
                 │                           └──────────────┘
                 │
        ┌────────▼──────────┐
        │   Transactions    │
        ├───────────────────┤
        │ id (PK)           │
        │ user_id (FK)      │
        │ amount            │
        │ status (ENUM)     │
        │ request_date      │
        │ processed_by (FK) │
        └───────────────────┘
```

### Table Details

#### Users Table
- **Purpose**: Stores all system users across 5 roles
- **Key Fields**:
  - `role`: ENUM (Admin, Manager, Team, Writer, Blogger)
  - `wallet_balance`: Decimal (for Bloggers only)
  - `is_active`: Boolean flag for account status
- **Indexes**: role, email

#### Websites Table
- **Purpose**: Inventory of available posting websites
- **Key Fields**:
  - `domain_url`: Unique website URL
  - `da_pa_score`: Domain Authority score (0-100)
  - `status`: ENUM (Active, Inactive)
- **Indexes**: status, category

#### Tasks Table (Core Entity)
- **Purpose**: Manages the entire workflow lifecycle
- **Key Fields**:
  - `current_status`: ENUM (11 states - see State Machine section)
  - `suggested_topic_url`: From Team member
  - `content_body`: From Writer
  - `live_published_url`: From Blogger
  - `payment_amount`: Credit amount for Blogger
- **Indexes**: current_status, created_by, assigned_writer_id, assigned_blogger_id

#### Transactions Table
- **Purpose**: Withdrawal requests and payment history
- **Key Fields**:
  - `status`: ENUM (Requested, Processing, Paid, Rejected)
  - `processed_by`: Manager who approved/rejected
- **Indexes**: user_id, status

---

## Workflow State Machine

### State Diagram

```
          DRAFT
            │
            ▼
   PENDING_MANAGER_APPROVAL_1 ──┐
            │                    │
            ▼                    │
    ASSIGNED_TO_WRITER          │
            │                    │
            ▼                    │
  WRITING_IN_PROGRESS           │
            │                    │
            ▼                    │
   PENDING_MANAGER_APPROVAL_2   │
            │        │           │
            │        ▼           │
            │  (Return to        │
            │   Writer)          │
            ▼                    │
   ASSIGNED_TO_BLOGGER          │
            │                    │
            ▼                    │
PUBLISHED_PENDING_VERIFICATION  │
            │                    │
            ▼                    │
   PENDING_FINAL_CHECK          │
            │        │           │
            ▼        ▼           ▼
        COMPLETED  REJECTED  REJECTED
            │
            ▼
         CREDITED
       (FINAL STATE)
```

### State Descriptions

| Status | Description | Performer | Next Allowed States |
|--------|-------------|-----------|-------------------|
| DRAFT | Initial creation | Team | PENDING_MANAGER_APPROVAL_1 |
| PENDING_MANAGER_APPROVAL_1 | Topic awaits approval | - | ASSIGNED_TO_WRITER, REJECTED |
| ASSIGNED_TO_WRITER | Task assigned to writer | Manager | WRITING_IN_PROGRESS, PENDING_MANAGER_APPROVAL_2 |
| WRITING_IN_PROGRESS | Content being written | Writer | PENDING_MANAGER_APPROVAL_2 |
| PENDING_MANAGER_APPROVAL_2 | Content awaits approval | - | ASSIGNED_TO_BLOGGER, ASSIGNED_TO_WRITER, REJECTED |
| ASSIGNED_TO_BLOGGER | Task assigned to blogger | Manager | PUBLISHED_PENDING_VERIFICATION, PENDING_FINAL_CHECK |
| PUBLISHED_PENDING_VERIFICATION | Blogger published | Blogger | PENDING_FINAL_CHECK |
| PENDING_FINAL_CHECK | Link awaits verification | - | COMPLETED, ASSIGNED_TO_BLOGGER, REJECTED |
| COMPLETED | Task completed | Manager | CREDITED |
| CREDITED | Payment credited (FINAL) | System | - |
| REJECTED | Task rejected (FINAL) | Manager | - |

### Transition Validation

All status transitions are validated using the `statusTransitions.js` utility:

```javascript
const { isTransitionAllowed } = require('./utils/statusTransitions');

if (!isTransitionAllowed(currentStatus, newStatus)) {
  throw new Error('Invalid transition');
}
```

---

## API Reference

### Authentication Endpoints

#### POST /api/auth/login
**Description**: Authenticate user and receive JWT token

**Request**:
```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

**Response** (200):
```json
{
  "message": "Login successful",
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "id": 1,
    "username": "john_doe",
    "email": "user@example.com",
    "role": "Writer",
    "wallet_balance": 0.00
  }
}
```

#### GET /api/auth/me
**Description**: Get current authenticated user

**Headers**: `Authorization: Bearer <token>`

**Response** (200):
```json
{
  "user": {
    "id": 1,
    "username": "john_doe",
    "email": "user@example.com",
    "role": "Writer",
    "wallet_balance": 0.00,
    "is_active": true,
    "created_at": "2024-01-01T00:00:00Z"
  }
}
```

---

### Admin Panel Endpoints

#### POST /api/admin/users
**Description**: Create new user

**Access**: Admin only

**Request**:
```json
{
  "username": "new_writer",
  "email": "writer@example.com",
  "password": "password123",
  "role": "Writer"
}
```

**Response** (201):
```json
{
  "message": "User created successfully",
  "user": {
    "id": 5,
    "username": "new_writer",
    "email": "writer@example.com",
    "role": "Writer",
    "wallet_balance": 0.00,
    "created_at": "2024-01-01T00:00:00Z"
  }
}
```

#### POST /api/admin/websites/upload
**Description**: Bulk upload websites from CSV

**Access**: Admin only

**Request**: `multipart/form-data` with file field

**CSV Format**:
```
domain_url,category,da_pa_score
example.com,Technology,65
another-site.com,Health,72
```

**Response** (200):
```json
{
  "message": "Websites uploaded successfully",
  "total_uploaded": 2
}
```

---

### Manager Panel Endpoints

#### PATCH /api/manager/tasks/:id/assign
**Description**: Assign task to writer (Approval 1)

**Access**: Manager only

**Request**:
```json
{
  "writer_id": 5,
  "instructions": "Write 1500 words on SEO best practices"
}
```

**Response** (200):
```json
{
  "message": "Task assigned to writer successfully",
  "task": {
    "id": 10,
    "current_status": "ASSIGNED_TO_WRITER",
    "assigned_writer_id": 5,
    "content_instructions": "Write 1500 words on SEO best practices"
  }
}
```

#### PATCH /api/manager/tasks/:id/finalize
**Description**: Finalize task and credit blogger (Approval 3)

**Access**: Manager only

**Response** (200):
```json
{
  "message": "Task finalized and blogger credited successfully",
  "task": {
    "id": 10,
    "current_status": "CREDITED",
    "payment_amount": 50.00
  },
  "payment_credited": 50.00
}
```

---

### Blogger Panel Endpoints

#### GET /api/blogger/wallet
**Description**: Get wallet balance and transaction history

**Access**: Blogger only

**Response** (200):
```json
{
  "wallet": {
    "current_balance": 275.00,
    "available_balance": 175.00,
    "total_withdrawn": 100.00,
    "pending_withdrawals": 100.00
  },
  "statistics": {
    "completed_ tasks": 5
  },
  "transactions": [...]
}
```

#### POST /api/blogger/withdrawals/request
**Description**: Request withdrawal

**Access**: Blogger only

**Request**:
```json
{
  "amount": 100.00,
  "notes": "PayPal withdrawal"
}
```

**Response** (201):
```json
{
  "message": "Withdrawal request submitted successfully",
  "withdrawal": {
    "id": 3,
    "user_id": 8,
    "amount": 100.00,
    "status": "Requested",
    "request_date": "2024-01-01T00:00:00Z"
  }
}
```

---

## Authentication & Authorization

### JWT Token Structure

```javascript
{
  "id": 1,
  "email": "user@example.com",
  "role": "Writer",
  "iat": 1640000000,
  "exp": 1640086400
}
```

### RBAC Implementation

Every protected route uses the `authenticate` and `authorize` middleware:

```javascript
router.get('/tasks', 
  authenticate,           // Verify JWT
  authorize('Writer'),    // Check role
  controller.getTasks
);
```

### Role Permissions Matrix

| Endpoint | Admin | Manager | Team | Writer | Blogger |
|----------|-------|---------|------|--------|---------|
| User Management | ✅ | ❌ | ❌ | ❌ | ❌ |
| Website Management | ✅ | ❌ | ❌ | ❌ | ❌ |
| Create Task | ❌ | ❌ | ✅ | ❌ | ❌ |
| Approve Tasks | ❌ | ✅ | ❌ | ❌ | ❌ |
| Submit Content | ❌ | ❌ | ❌ | ✅ | ❌ |
| Submit Link | ❌ | ❌ | ❌ | ❌ | ✅ |
| Withdrawal Approval | ❌ | ✅ | ❌ | ❌ | ❌ |
| Request Withdrawal | ❌ | ❌ | ❌ | ❌ | ✅ |

---

## User Roles & Responsibilities

### Super Admin
**Responsibilities**:
- Create and manage users across all roles
- Manage website inventory
- Configure system settings (payment rates, etc.)
- Access all system data

**Key Operations**:
- User CRUD
- Website CRUD
- System configuration
- View statistics

### Manager
**Responsibilities**:
- Approve topics from Team (Approval 1)
- Review and approve content from Writers (Approval 2)
- Verify published links from Bloggers (Approval 3)
- Manage withdrawal requests

**Key Operations**:
- Task assignment
- Content approval/rejection
- Final verification
- Payment processing

### Team (Researcher)
**Responsibilities**:
- Find relevant posts/topics
- Submit topic suggestions with URLs

**Key Operations**:
- Create tasks
- View own submitted tasks

### Writer
**Responsibilities**:
- Receive approved topics
- Write content based on instructions
- Submit completed content

**Key Operations**:
- View assigned tasks
- Mark tasks in progress
- Submit content

### Blogger
**Responsibilities**:
- Receive approved content
- Publish on their blog
- Submit live URL
- Request payment withdrawals

**Key Operations**:
- View assigned content
- Submit published links
- View wallet balance
- Request withdrawals

---

## Data Flow

### Complete Task Lifecycle

```
1. TEAM creates task
   POST /api/team/tasks
   { "suggested_topic_url": "https://example.com/article" }
   Status: PENDING_MANAGER_APPROVAL_1

2. MANAGER approves and assigns to Writer
   PATCH /api/manager/tasks/1/assign
   { "writer_id": 5, "instructions": "Write 1500 words" }
   Status: ASSIGNED_TO_WRITER

3. WRITER submits content
   POST /api/writer/tasks/1/submit-content
   { "content_body": "..." }
   Status: PENDING_MANAGER_APPROVAL_2

4. MANAGER approves content and assigns to Blogger
   PATCH /api/manager/tasks/1/approve-content
   { "blogger_id": 8 }
   Status: ASSIGNED_TO_BLOGGER

5. BLOGGER publishes and submits link
   POST /api/blogger/tasks/1/submit-link
   { "live_url": "https://blog.com/published-post" }
   Status: PENDING_FINAL_CHECK

6. MANAGER finalizes (triggers payment)
   PATCH /api/manager/tasks/1/finalize
   Status: CREDITED
   Blogger wallet += 50.00
```

---

## Wallet & Payment System

### Wallet Credit Flow

```
┌─────────────────┐
│ Manager clicks  │
│ "Finalize Task" │
└────────┬────────┘
         │
         ▼
┌────────────────────────────┐
│ System fetches payment     │
│ amount from config         │
│ (default: $50.00)          │
└────────┬───────────────────┘
         │
         ▼
┌────────────────────────────┐
│ addCreditToBloggerWallet() │
│ - Uses DB transaction      │
│ - Updates wallet_balance   │
└────────┬───────────────────┘
         │
         ▼
┌────────────────────────────┐
│ Task status → CREDITED     │
│ payment_amount recorded    │
└────────────────────────────┘
```

### Withdrawal Process

```
1. Blogger requests withdrawal
   └─> Checks: amount > min_withdrawal_amount
   └─> Checks: available_balance >= amount
   └─> Creates transaction (status: Requested)

2. Manager reviews request
   └─> Option A: Approve
       ├─> Deducts from wallet_balance
       └─> Transaction status → Paid
   └─> Option B: Reject
       └─> Transaction status → Rejected
```

### Available Balance Calculation

```
available_balance = wallet_balance - pending_withdrawals

where:
  wallet_balance = SUM(all credits)
  pending_withdrawals = SUM(status IN ('Requested', 'Processing'))
```

---

## Error Handling

### Error Response Format

All errors return consistent JSON structure:

```json
{
  "error": "Error Type",
  "message": "Human-readable error message",
  "stack": "..." // Only in development
}
```

### HTTP Status Codes

| Code | Usage |
|------|-------|
| 200 | Success |
| 201 | Created |
| 400 | Bad Request / Validation Error |
| 401 | Unauthorized / Invalid Token |
| 403 | Forbidden / Insufficient Permissions |
| 404 | Not Found |
| 409 | Conflict (e.g., duplicate email) |
| 500 | Internal Server Error |

### Common Error Scenarios

**Invalid Status Transition**:
```json
{
  "error": "Invalid Transition",
  "message": "Cannot assign to writer from status: COMPLETED"
}
```

**Insufficient Balance**:
```json
{
  "error": "Insufficient Balance",
  "message": "Available balance: 75.00. Requested: 100.00"
}
```

**Unauthorized Access**:
```json
{
  "error": "Forbidden",
  "message": "Access denied. Required role: Manager"
}
```

---

## Appendix

### Configuration Options

System configuration is stored in the `system_config` table:

| Key | Default Value | Description |
|-----|---------------|-------------|
| `default_post_payment` | 50.00 | Payment per completed post |
| `default_currency` | USD | Currency for transactions |
| `min_withdrawal_amount` | 100.00 | Minimum withdrawal amount |

### Database Triggers

**Updated At Trigger**: Automatically updates `updated_at` timestamp on row modification for:
- users
- websites
- tasks
- system_config

### Security Best Practices

1. **Always use HTTPS** in production
2. **Rotate JWT secrets** regularly
3. **Use strong passwords** (enforced by bcrypt)
4. **Rate limit** authentication endpoints
5. **Validate all inputs** server-side
6. **Use parameterized queries** (prevents SQL injection)
7. **Keep dependencies updated**

---

*End of Documentation*
