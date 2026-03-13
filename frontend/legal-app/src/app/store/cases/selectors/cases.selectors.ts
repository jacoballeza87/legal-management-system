// src/app/store/cases/selectors/cases.selectors.ts
import { createFeatureSelector, createSelector } from '@ngrx/store';
import { CasesState } from '../reducers/cases.reducer';

export const selectCasesState = createFeatureSelector<CasesState>('cases');

export const selectAllCases      = createSelector(selectCasesState, s => s.cases);
export const selectSelectedCase  = createSelector(selectCasesState, s => s.selectedCase);
export const selectStats         = createSelector(selectCasesState, s => s.stats);
export const selectFilters       = createSelector(selectCasesState, s => s.filters);
export const selectTotalElements = createSelector(selectCasesState, s => s.totalElements);
export const selectTotalPages    = createSelector(selectCasesState, s => s.totalPages);
export const selectLoading       = createSelector(selectCasesState, s => s.loading);
export const selectLoadingDetail = createSelector(selectCasesState, s => s.loadingDetail);
export const selectLoadingCreate = createSelector(selectCasesState, s => s.loadingCreate);
export const selectError         = createSelector(selectCasesState, s => s.error);
export const selectViewMode      = createSelector(selectCasesState, s => s.viewMode);

export const selectVersionsByCaseId = (caseId: number) =>
  createSelector(selectCasesState, s => s.versions[caseId] ?? []);

// Casos agrupados por status para Kanban
export const selectCasesByStatus = createSelector(selectAllCases, cases => ({
  ABIERTO:     cases.filter(c => c.status === 'ABIERTO'),
  EN_REVISION: cases.filter(c => c.status === 'EN_REVISION'),
  ACTIVO:      cases.filter(c => c.status === 'ACTIVO'),
  CERRADO:     cases.filter(c => c.status === 'CERRADO'),
}));

// Casos urgentes
export const selectUrgentCases = createSelector(selectAllCases,
  cases => cases.filter(c => c.priority === 'URGENTE' && c.status !== 'CERRADO')
);

// Casos próximos a vencer (dentro de 7 días)
export const selectNearDueCases = createSelector(selectAllCases, cases => {
  const now = new Date();
  const in7 = new Date(now.getTime() + 7 * 24 * 60 * 60 * 1000);
  return cases.filter(c => {
    if (!c.dueDate || c.status === 'CERRADO') return false;
    const due = new Date(c.dueDate);
    return due >= now && due <= in7;
  });
});

// Paginación actual
export const selectCurrentPage = createSelector(selectFilters, f => f.page);
export const selectPageSize    = createSelector(selectFilters, f => f.size);
