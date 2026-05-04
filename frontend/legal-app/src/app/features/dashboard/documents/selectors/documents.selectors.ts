// src/app/store/documents/selectors/documents.selectors.ts
import { createFeatureSelector, createSelector } from '@ngrx/store';
import { DocumentsState } from '../reducers/documents.reducer';

export const selectDocumentsState = createFeatureSelector<DocumentsState>('documents');

// ── Lista ────────────────────────────────────────────────────────────
export const selectAllDocuments      = createSelector(selectDocumentsState, s => s.documents);
export const selectTotalElements     = createSelector(selectDocumentsState, s => s.totalElements);
export const selectTotalPages        = createSelector(selectDocumentsState, s => s.totalPages);
export const selectFilters           = createSelector(selectDocumentsState, s => s.filters);
export const selectActiveCategoryFilter = createSelector(selectDocumentsState, s => s.activeCategoryFilter);

// ── Documento seleccionado ───────────────────────────────────────────
export const selectSelectedDocument  = createSelector(selectDocumentsState, s => s.selectedDocument);
export const selectDownloadUrls      = createSelector(selectDocumentsState, s => s.downloadUrls);
export const selectDownloadUrl = (id: number) =>
  createSelector(selectDownloadUrls, urls => urls[id] ?? null);

// ── Docs por caso ────────────────────────────────────────────────────
export const selectDocumentsByCase = (caseId: number) =>
  createSelector(selectDocumentsState, s => s.documentsByCase[caseId] ?? []);

// ── Stats ────────────────────────────────────────────────────────────
export const selectStats             = createSelector(selectDocumentsState, s => s.stats);

// ── Upload Queue ─────────────────────────────────────────────────────
export const selectUploadQueue       = createSelector(selectDocumentsState, s => s.uploadQueue);
export const selectPendingUploads    = createSelector(selectUploadQueue, q => q.filter(i => i.status === 'pending'));
export const selectUploadingItems    = createSelector(selectUploadQueue, q => q.filter(i => i.status === 'uploading'));
export const selectDoneUploads       = createSelector(selectUploadQueue, q => q.filter(i => i.status === 'done'));
export const selectFailedUploads     = createSelector(selectUploadQueue, q => q.filter(i => i.status === 'error'));
export const selectHasQueueItems     = createSelector(selectUploadQueue, q => q.length > 0);
export const selectAllDone           = createSelector(selectUploadQueue,
  q => q.length > 0 && q.every(i => i.status === 'done' || i.status === 'error')
);

// ── UI ───────────────────────────────────────────────────────────────
export const selectViewMode          = createSelector(selectDocumentsState, s => s.viewMode);
export const selectLoading           = createSelector(selectDocumentsState, s => s.loading);
export const selectLoadingDetail     = createSelector(selectDocumentsState, s => s.loadingDetail);
export const selectError             = createSelector(selectDocumentsState, s => s.error);
export const selectUploadingIds      = createSelector(selectDocumentsState, s => s.uploadingIds);
export const selectIsUploading       = createSelector(selectUploadingIds, ids => ids.length > 0);

// ── Derivados útiles ─────────────────────────────────────────────────
// Contar documentos por categoría (desde la lista actual)
export const selectDocCountByCategory = createSelector(selectAllDocuments, docs => {
  const map: Record<string, number> = {};
  docs.forEach(d => { map[d.category] = (map[d.category] ?? 0) + 1; });
  return map;
});
