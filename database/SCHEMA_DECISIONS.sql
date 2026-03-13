-- =============================================================================
-- SCHEMA DECISION REFERENCE
-- legal_management_db — What changed, why, and which business rule it covers
-- =============================================================================

/*
HOW TO USE THIS FILE
--------------------
1. Run fix_legal_management_db.sql ONCE on your RDS MySQL instance
2. Use this file to understand every decision so you can map your @Entity classes correctly
3. After running the migration, set ddl-auto: validate in ALL services
   so Hibernate checks but never modifies the schema again

DDL-AUTO RECOMMENDATION:
  During migration:  ddl-auto: update   (lets Hibernate reconcile)
  After confirmed:   ddl-auto: validate  (safe — crashes if mismatch, never destroys data)
  Never use:         ddl-auto: create-drop (destroys data on restart)
  Never use:         ddl-auto: create     (destroys data on first boot)

=============================================================================
CHANGES EXPLAINED
=============================================================================

── USERS TABLE ────────────────────────────────────────────────────────────────

temporary_password: BOOLEAN → VARCHAR(255)
  WHY: BR#29 says OAuth users get a temporary password sent by email.
       A BOOLEAN can't store the actual bcrypt hash. Needed VARCHAR to store hash.
  ENTITY FIX: @Column(name="temporary_password") private String temporaryPassword;

temp_password_expires_at: NEW DATETIME
  WHY: BR#29 — temp passwords must expire. Without this column there's no way
       to enforce expiry in the auth-service validation logic.

email_verified: NEW TINYINT(1) DEFAULT 0
  WHY: BR#29 — OAuth users must verify email before full access.
       Also BR#28 — Google/Hotmail registration flow requires email verification step.

failed_login_attempts + locked_until: NEW
  WHY: BR#44 — device limits imply security awareness. Brute force protection
       is a standard companion requirement. auth-service needs these to implement lockout.

uuid: NEW CHAR(36)
  WHY: Best practice — never expose internal auto-increment IDs in APIs or QR codes (BR#16).
       All external-facing endpoints (REST, QR, PDF) should use uuid, not user_id.
  NOTE: Backfilled with UUID() for all existing rows.

deleted_at: NEW DATETIME
  WHY: BR#12 — only SYS_ADMIN can delete users. Soft delete preserves data integrity
       (case history, audit logs remain valid). Hard deletes would break foreign keys.

── CASES TABLE ────────────────────────────────────────────────────────────────

supervisor_id: NEW BIGINT FK → users(user_id)
  WHY: BR#26 — supervisors exist per category. A supervisor needs to be assignable
       to a specific case to receive inactivity alerts (BR#27) and notifications.

certified_at + certified_by: NEW
  WHY: BR#36 — CERTIFIED is a terminal state that cannot be reopened. Need to track
       when and who certified for audit trail (BR#12 admin actions).

deleted_at + deleted_by: NEW
  WHY: BR#12 — only SYS_ADMIN can delete cases. Soft delete keeps case history intact.

status ENUM: ADDED 'NOT_CREATED' and 'CERTIFIED'
  WHY: BR#35 lists NOT_CREATED as status #1. BR#36/#37 define CERTIFIED as the only
       truly terminal state. Without CERTIFIED in the ENUM, the business rule
       "cannot reopen certified cases" cannot be enforced at DB level.

authority_id: NEW BIGINT → authorities(authority_id)
  WHY: BR#10 — system must look up exact address of the authority. Having just a
       VARCHAR authority_name means you can't store or retrieve the address, maps link,
       phone, etc. The new authorities catalog table solves this.

── CASE_VERSIONS TABLE ────────────────────────────────────────────────────────

extra_notification_email: NEW VARCHAR(255)
  WHY: BR#15 explicitly says "se podrá registrar un nuevo correo para poder enviar
       esa alerta también a ese correo por cada versión nueva el cual es opcional."
       This is per-version, not per-case, so it belongs on case_versions.

pdf_url + pdf_generated: NEW
  WHY: BR#11 says PDF is generated and emailed on case creation.
       BR#53 says PDF is generated on every version update.
       Storing the URL means you can re-send without regenerating.

is_deleted + deleted_by + deleted_at: NEW
  WHY: BR#12 — only SYS_ADMIN can delete versions. Soft delete preserves timeline
       integrity. A hard delete would break the version sequence display (BR#4).

── S3_BUCKETS TABLE ────────────────────────────────────────────────────────────

bucket_type ENUM('CASES','CLOUD','GENERAL'): NEW
  WHY: BR#20 — case documents in a separate repository.
       BR#22 — originally Google Drive but BR#40 says cloud is S3.
       BR#40 — SYS_ADMIN-only private cloud bucket.
       Without this distinction, the backend can't enforce who can see what bucket.

is_private: NEW TINYINT(1)
  WHY: BR#40 — "solo el administrador del sistema puede administrarla."
       is_private=1 means only SYS_ADMIN role can access this bucket at all.

── S3_FILES TABLE ──────────────────────────────────────────────────────────────

case_id + version_id: NEW
  WHY: BR#30 — documents uploaded on a version go to the case folder.
       Without direct FK links, you can't efficiently query "all files for case X"
       or "all files attached to version Y."

is_evidence: NEW TINYINT(1)
  WHY: BR#31 — evidence files on closed/completed cases have special visibility rules.
       Any user who clicks the case can see evidence. This flag enables that filter.

── NEW TABLE: authorities ──────────────────────────────────────────────────────
  WHY: BR#10 — "al identificar la autoridad y el nombre exacto de la autoridad
       deberá de buscar la dirección exacta de esa autoridad."
       This catalog stores pre-loaded juzgados. Your case-service can do a lookup
       and auto-populate address when authority is selected on case creation.

── NEW TABLE: case_inactivity_alerts ──────────────────────────────────────────
  WHY: BR#27 — "si de la última actualización o última versión a tres días no
       existe una actualización se creará una alerta."
       Without this log table, your scheduler job can't know if it already sent
       the alert today and would spam users on every run.

── NEW TABLE: bucket_permissions ──────────────────────────────────────────────
  WHY: BR#54 — SYS_ADMIN: full access. ADMIN: upload only, no modify/delete.
       SUPERVISOR: view only.
       This table lets you configure permissions per bucket per role without
       hardcoding them in your microservice.

── NEW TABLE: error_catalog ───────────────────────────────────────────────────
  WHY: BR#49 — "crear sección para identificar errores del sistema y poder
       customizarlos a nuestra manera."
       Pre-seeded with the most common business errors. Your error-handling
       @ControllerAdvice can look up error_code and return the right message.

── client_collaborations: reviewed_by + reviewed_at ──────────────────────────
  WHY: BR#38 — admin needs to review client collaboration submissions.
       Without tracking who reviewed and when, there's no audit trail.

=============================================================================
ENTITY MAPPING CHECKLIST
=============================================================================
After running the migration, update these @Entity classes:

User.java:
  □ temporaryPassword: String (not boolean)
  □ tempPasswordExpiresAt: LocalDateTime
  □ emailVerified: boolean
  □ failedLoginAttempts: int
  □ lockedUntil: LocalDateTime
  □ uuid: String
  □ deletedAt: LocalDateTime

Case.java (or Cases.java):
  □ supervisorId: Long
  □ certifiedAt: LocalDateTime
  □ certifiedBy: Long
  □ deletedAt: LocalDateTime
  □ deletedBy: Long
  □ authorityId: Long
  □ status: CaseStatus enum must include NOT_CREATED, CERTIFIED

CaseVersion.java:
  □ extraNotificationEmail: String
  □ pdfUrl: String
  □ pdfGenerated: boolean
  □ isDeleted: boolean
  □ deletedBy: Long
  □ deletedAt: LocalDateTime

S3Bucket.java:
  □ bucketType: BucketType enum (CASES, CLOUD, GENERAL)
  □ isPrivate: boolean
  □ region: String

S3File.java:
  □ caseId: Long
  □ versionId: Long
  □ isEvidence: boolean
  □ deletedAt: LocalDateTime

New entities to CREATE:
  □ Authority.java         → authorities table
  □ CaseInactivityAlert.java → case_inactivity_alerts table
  □ BucketPermission.java  → bucket_permissions table
  □ ErrorCatalog.java      → error_catalog table

=============================================================================
HIBERNATE DDL-AUTO SETTINGS PER SERVICE (application.yml)
=============================================================================

auth-service:        ddl-auto: validate
case-service:        ddl-auto: validate
user-service:        ddl-auto: validate
notification-service: ddl-auto: validate
document-service:    ddl-auto: validate

# After confirming everything works, NEVER change back to update or create.
# If schema drifts in future, write a new migration SQL file — never rely on Hibernate DDL.
*/
