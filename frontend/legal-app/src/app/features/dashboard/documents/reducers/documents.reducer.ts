// src/app/store/documents/reducers/documents.reducer.ts
import { createReducer, on } from '@ngrx/store';
import { DocumentsActions } from '../actions/documents.actions';
import { CaseDocument, DocumentStats, DocumentFilters, UploadQueueItem } from '../../../core/models/document.model';

export interface DocumentsState {
  // Lista paginada
  documents: CaseDocument[];
  totalElements: number;
  totalPages: number;
  filters: DocumentFilters;

  // Documentos por caso (cache local)
  documentsByCase: Record<number, CaseDocument[]>;

  // Detalle seleccionado
  selectedDocument: CaseDocument | null;
  downloadUrls: Record<number, string>;  // cache de presigned URLs

  // Estadísticas
  stats: DocumentStats | null;

  // Cola de upload local
  uploadQueue: UploadQueueItem[];

  // UI
  viewMode: 'grid' | 'list';
  activeCategoryFilter: string | null;

  // Loading states
  loading: boolean;
  loadingDetail: boolean;
  uploadingIds: string[];   // localIds en progreso

  error: string | null;
}

const defaultFilters: DocumentFilters = {
  page: 0,
  size: 24,
};

export const initialState: DocumentsState = {
  documents: [],
  totalElements: 0,
  totalPages: 0,
  filters: defaultFilters,
  documentsByCase: {},
  selectedDocument: null,
  downloadUrls: {},
  stats: null,
  uploadQueue: [],
  viewMode: 'grid',
  activeCategoryFilter: null,
  loading: false,
  loadingDetail: false,
  uploadingIds: [],
  error: null,
};

export const documentsReducer = createReducer(
  initialState,

  // ── LOAD LIST ────────────────────────────────────────────────────
  on(DocumentsActions.loadDocuments, state => ({ ...state, loading: true, error: null })),
  on(DocumentsActions.loadDocumentsSuccess, (state, { paged }) => ({
    ...state,
    loading: false,
    documents: paged.content,
    totalElements: paged.totalElements,
    totalPages: paged.totalPages,
  })),
  on(DocumentsActions.loadDocumentsFailure, (state, { error }) => ({
    ...state, loading: false, error
  })),

  // ── LOAD BY CASE ─────────────────────────────────────────────────
  on(DocumentsActions.loadCaseDocumentsSuccess, (state, { caseId, documents }) => ({
    ...state,
    documentsByCase: { ...state.documentsByCase, [caseId]: documents }
  })),

  // ── STATS ────────────────────────────────────────────────────────
  on(DocumentsActions.loadStatsSuccess, (state, { stats }) => ({ ...state, stats })),

  // ── SELECT ───────────────────────────────────────────────────────
  on(DocumentsActions.selectDocument, state => ({ ...state, loadingDetail: true })),
  on(DocumentsActions.selectDocumentSuccess, (state, { document }) => ({
    ...state, loadingDetail: false, selectedDocument: document
  })),
  on(DocumentsActions.selectDocumentFailure, (state, { error }) => ({
    ...state, loadingDetail: false, error
  })),
  on(DocumentsActions.clearSelected, state => ({ ...state, selectedDocument: null })),

  // ── UPLOAD QUEUE ─────────────────────────────────────────────────
  on(DocumentsActions.addToQueue, (state, { items }) => ({
    ...state, uploadQueue: [...state.uploadQueue, ...items]
  })),
  on(DocumentsActions.removeFromQueue, (state, { localId }) => ({
    ...state, uploadQueue: state.uploadQueue.filter(i => i.localId !== localId)
  })),
  on(DocumentsActions.clearQueue, state => ({ ...state, uploadQueue: [] })),
  on(DocumentsActions.setQueueMeta, (state, { localId, caseId, category, description }) => ({
    ...state,
    uploadQueue: state.uploadQueue.map(i =>
      i.localId === localId
        ? { ...i, caseId, category: category as any, description }
        : i
    )
  })),

  // ── UPLOAD PROGRESS ──────────────────────────────────────────────
  on(DocumentsActions.uploadDocument, (state, { item }) => ({
    ...state,
    uploadingIds: [...state.uploadingIds, item.localId],
    uploadQueue: state.uploadQueue.map(i =>
      i.localId === item.localId ? { ...i, status: 'uploading' as any } : i
    )
  })),
  on(DocumentsActions.uploadProgress, (state, { localId, progress }) => ({
    ...state,
    uploadQueue: state.uploadQueue.map(i =>
      i.localId === localId ? { ...i, progress } : i
    )
  })),
  on(DocumentsActions.uploadDocumentSuccess, (state, { localId, document }) => ({
    ...state,
    uploadingIds: state.uploadingIds.filter(id => id !== localId),
    uploadQueue: state.uploadQueue.map(i =>
      i.localId === localId
        ? { ...i, status: 'done' as any, progress: 100, uploadedDoc: document }
        : i
    ),
    documents: [document, ...state.documents],
    totalElements: state.totalElements + 1,
  })),
  on(DocumentsActions.uploadDocumentFailure, (state, { localId, error }) => ({
    ...state,
    uploadingIds: state.uploadingIds.filter(id => id !== localId),
    uploadQueue: state.uploadQueue.map(i =>
      i.localId === localId
        ? { ...i, status: 'error' as any, errorMessage: error }
        : i
    )
  })),

  // ── DOWNLOAD URL ─────────────────────────────────────────────────
  on(DocumentsActions.getDownloadUrlSuccess, (state, { id, url }) => ({
    ...state,
    downloadUrls: { ...state.downloadUrls, [id]: url }
  })),

  // ── DELETE ───────────────────────────────────────────────────────
  on(DocumentsActions.deleteDocumentSuccess, (state, { id }) => ({
    ...state,
    documents: state.documents.filter(d => d.id !== id),
    totalElements: state.totalElements - 1,
    selectedDocument: state.selectedDocument?.id === id ? null : state.selectedDocument,
  })),

  // ── RESTORE ──────────────────────────────────────────────────────
  on(DocumentsActions.restoreDocumentSuccess, (state, { document }) => ({
    ...state,
    documents: state.documents.map(d => d.id === document.id ? document : d),
    selectedDocument: state.selectedDocument?.id === document.id ? document : state.selectedDocument
  })),

  // ── FILTERS / UI ─────────────────────────────────────────────────
  on(DocumentsActions.setFilters, (state, { filters }) => ({
    ...state, filters: { ...state.filters, ...filters, page: 0 }
  })),
  on(DocumentsActions.resetFilters, state => ({
    ...state, filters: defaultFilters, activeCategoryFilter: null
  })),
  on(DocumentsActions.setViewMode, (state, { mode }) => ({ ...state, viewMode: mode })),
  on(DocumentsActions.setCategoryFilter, (state, { category }) => ({
    ...state,
    activeCategoryFilter: category,
    filters: { ...state.filters, category: category as any ?? undefined, page: 0 }
  })),
);
