// src/app/store/cases/actions/cases.actions.ts
import { createActionGroup, emptyProps, props } from '@ngrx/store';
import {
  Case, CaseVersion, CaseStats, CaseFilters, PagedCases,
  CreateCaseRequest, UpdateCaseRequest,
  CreateVersionRequest, AddCommentRequest, AddCollaboratorRequest
} from '../../../core/models/case.model';

export const CasesActions = createActionGroup({
  source: 'Cases',
  events: {

    // ── LOAD LIST ──────────────────────────────────────────────────
    'Load Cases':         props<{ filters: CaseFilters }>(),
    'Load Cases Success': props<{ paged: PagedCases; filters: CaseFilters }>(),
    'Load Cases Failure': props<{ error: string }>(),

    // ── LOAD STATS ─────────────────────────────────────────────────
    'Load Stats':         emptyProps(),
    'Load Stats Success': props<{ stats: CaseStats }>(),
    'Load Stats Failure': props<{ error: string }>(),

    // ── LOAD DETAIL ────────────────────────────────────────────────
    'Load Case':         props<{ id: number }>(),
    'Load Case Success': props<{ case: Case }>(),
    'Load Case Failure': props<{ error: string }>(),

    // ── SELECT ─────────────────────────────────────────────────────
    'Select Case': props<{ id: number | null }>(),

    // ── CREATE ─────────────────────────────────────────────────────
    'Create Case':         props<{ request: CreateCaseRequest }>(),
    'Create Case Success': props<{ case: Case }>(),
    'Create Case Failure': props<{ error: string }>(),

    // ── UPDATE ─────────────────────────────────────────────────────
    'Update Case':         props<{ id: number; request: UpdateCaseRequest }>(),
    'Update Case Success': props<{ case: Case }>(),
    'Update Case Failure': props<{ error: string }>(),

    // ── DELETE ─────────────────────────────────────────────────────
    'Delete Case':         props<{ id: number }>(),
    'Delete Case Success': props<{ id: number }>(),
    'Delete Case Failure': props<{ error: string }>(),

    // ── KANBAN DRAG ────────────────────────────────────────────────
    'Move Case Status':         props<{ id: number; newStatus: string }>(),
    'Move Case Status Success': props<{ case: Case }>(),
    'Move Case Status Failure': props<{ error: string }>(),

    // ── VERSIONS ───────────────────────────────────────────────────
    'Load Versions':         props<{ caseId: number }>(),
    'Load Versions Success': props<{ caseId: number; versions: CaseVersion[] }>(),
    'Load Versions Failure': props<{ error: string }>(),

    'Create Version':         props<{ caseId: number; request: CreateVersionRequest }>(),
    'Create Version Success': props<{ caseId: number; version: CaseVersion }>(),
    'Create Version Failure': props<{ error: string }>(),

    // ── FILTERS ────────────────────────────────────────────────────
    'Set Filters':   props<{ filters: Partial<CaseFilters> }>(),
    'Reset Filters': emptyProps(),

    // ── UI ─────────────────────────────────────────────────────────
    'Set View Mode': props<{ mode: 'list' | 'kanban' }>(),
  }
});
