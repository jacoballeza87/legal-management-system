// src/app/store/cases/reducers/cases.reducer.ts
import { createReducer, on } from '@ngrx/store';
import { CasesActions } from '../actions/cases.actions';
import { Case, CaseVersion, CaseStats, CaseFilters } from '../../../core/models/case.model';

export interface CasesState {
  cases: Case[];
  selectedCase: Case | null;
  versions: Record<number, CaseVersion[]>;
  stats: CaseStats | null;
  filters: CaseFilters;
  totalElements: number;
  totalPages: number;
  viewMode: 'list' | 'kanban';
  loading: boolean;
  loadingDetail: boolean;
  loadingCreate: boolean;
  error: string | null;
}

const defaultFilters: CaseFilters = {
  page: 0,
  size: 10,
  sortBy: 'createdAt',
  sortDir: 'desc'
};

export const initialState: CasesState = {
  cases: [],
  selectedCase: null,
  versions: {},
  stats: null,
  filters: defaultFilters,
  totalElements: 0,
  totalPages: 0,
  viewMode: 'list',
  loading: false,
  loadingDetail: false,
  loadingCreate: false,
  error: null
};

export const casesReducer = createReducer(
  initialState,

  // ── LOAD LIST ────────────────────────────────────────────────────
  on(CasesActions.loadCases, state => ({ ...state, loading: true, error: null })),
  on(CasesActions.loadCasesSuccess, (state, { paged, filters }) => ({
    ...state,
    loading: false,
    cases: paged.content,
    totalElements: paged.totalElements,
    totalPages: paged.totalPages,
    filters: { ...state.filters, ...filters }
  })),
  on(CasesActions.loadCasesFailure, (state, { error }) => ({
    ...state, loading: false, error
  })),

  // ── STATS ────────────────────────────────────────────────────────
  on(CasesActions.loadStatsSuccess, (state, { stats }) => ({ ...state, stats })),

  // ── DETAIL ───────────────────────────────────────────────────────
  on(CasesActions.loadCase, state => ({ ...state, loadingDetail: true, error: null })),
  on(CasesActions.loadCaseSuccess, (state, { case: c }) => ({
    ...state, loadingDetail: false, selectedCase: c
  })),
  on(CasesActions.loadCaseFailure, (state, { error }) => ({
    ...state, loadingDetail: false, error
  })),
  on(CasesActions.selectCase, (state, { id }) => ({
    ...state,
    selectedCase: id ? (state.cases.find(c => c.id === id) ?? state.selectedCase) : null
  })),

  // ── CREATE ───────────────────────────────────────────────────────
  on(CasesActions.createCase, state => ({ ...state, loadingCreate: true, error: null })),
  on(CasesActions.createCaseSuccess, (state, { case: c }) => ({
    ...state,
    loadingCreate: false,
    cases: [c, ...state.cases],
    totalElements: state.totalElements + 1
  })),
  on(CasesActions.createCaseFailure, (state, { error }) => ({
    ...state, loadingCreate: false, error
  })),

  // ── UPDATE ───────────────────────────────────────────────────────
  on(CasesActions.updateCaseSuccess, (state, { case: updated }) => ({
    ...state,
    cases: state.cases.map(c => c.id === updated.id ? updated : c),
    selectedCase: state.selectedCase?.id === updated.id ? updated : state.selectedCase
  })),

  // ── DELETE ───────────────────────────────────────────────────────
  on(CasesActions.deleteCaseSuccess, (state, { id }) => ({
    ...state,
    cases: state.cases.filter(c => c.id !== id),
    totalElements: state.totalElements - 1,
    selectedCase: state.selectedCase?.id === id ? null : state.selectedCase
  })),

  // ── KANBAN MOVE ──────────────────────────────────────────────────
  on(CasesActions.moveCaseStatusSuccess, (state, { case: updated }) => ({
    ...state,
    cases: state.cases.map(c => c.id === updated.id ? updated : c),
    selectedCase: state.selectedCase?.id === updated.id ? updated : state.selectedCase
  })),

  // ── VERSIONS ─────────────────────────────────────────────────────
  on(CasesActions.loadVersionsSuccess, (state, { caseId, versions }) => ({
    ...state,
    versions: { ...state.versions, [caseId]: versions }
  })),
  on(CasesActions.createVersionSuccess, (state, { caseId, version }) => ({
    ...state,
    versions: {
      ...state.versions,
      [caseId]: [version, ...(state.versions[caseId] ?? [])]
    }
  })),

  // ── FILTERS ──────────────────────────────────────────────────────
  on(CasesActions.setFilters, (state, { filters }) => ({
    ...state,
    filters: { ...state.filters, ...filters, page: 0 }
  })),
  on(CasesActions.resetFilters, state => ({
    ...state, filters: defaultFilters
  })),

  // ── UI ───────────────────────────────────────────────────────────
  on(CasesActions.setViewMode, (state, { mode }) => ({ ...state, viewMode: mode }))
);
