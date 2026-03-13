// src/app/store/documents/actions/documents.actions.ts
import { createActionGroup, emptyProps, props } from '@ngrx/store';
import {
  CaseDocument, DocumentStats, DocumentFilters, PagedDocuments,
  UploadQueueItem, UploadDocumentRequest
} from '../../../core/models/document.model';

export const DocumentsActions = createActionGroup({
  source: 'Documents',
  events: {

    // ── LOAD LIST ─────────────────────────────────────────────────
    'Load Documents':         props<{ filters: DocumentFilters }>(),
    'Load Documents Success': props<{ paged: PagedDocuments }>(),
    'Load Documents Failure': props<{ error: string }>(),

    // ── LOAD BY CASE ──────────────────────────────────────────────
    'Load Case Documents':         props<{ caseId: number }>(),
    'Load Case Documents Success': props<{ caseId: number; documents: CaseDocument[] }>(),
    'Load Case Documents Failure': props<{ error: string }>(),

    // ── STATS ─────────────────────────────────────────────────────
    'Load Stats':         emptyProps(),
    'Load Stats Success': props<{ stats: DocumentStats }>(),
    'Load Stats Failure': props<{ error: string }>(),

    // ── SELECT / DETAIL ───────────────────────────────────────────
    'Select Document':         props<{ id: number }>(),
    'Select Document Success': props<{ document: CaseDocument }>(),
    'Select Document Failure': props<{ error: string }>(),
    'Clear Selected':          emptyProps(),

    // ── UPLOAD QUEUE (local, pre-envío) ───────────────────────────
    'Add To Queue':      props<{ items: UploadQueueItem[] }>(),
    'Remove From Queue': props<{ localId: string }>(),
    'Clear Queue':       emptyProps(),
    'Set Queue Meta':    props<{ localId: string; caseId: number; category: string; description?: string }>(),

    // ── UPLOAD (envío real al servidor) ───────────────────────────
    'Upload Document':          props<{ item: UploadQueueItem }>(),
    'Upload Progress':          props<{ localId: string; progress: number }>(),
    'Upload Document Success':  props<{ localId: string; document: CaseDocument }>(),
    'Upload Document Failure':  props<{ localId: string; error: string }>(),
    'Upload All':               emptyProps(),

    // ── DOWNLOAD ──────────────────────────────────────────────────
    'Get Download Url':         props<{ id: number }>(),
    'Get Download Url Success': props<{ id: number; url: string }>(),
    'Get Download Url Failure': props<{ error: string }>(),

    // ── DELETE ────────────────────────────────────────────────────
    'Delete Document':         props<{ id: number }>(),
    'Delete Document Success': props<{ id: number }>(),
    'Delete Document Failure': props<{ error: string }>(),

    // ── RESTORE ───────────────────────────────────────────────────
    'Restore Document':         props<{ id: number }>(),
    'Restore Document Success': props<{ document: CaseDocument }>(),
    'Restore Document Failure': props<{ error: string }>(),

    // ── FILTERS / UI ──────────────────────────────────────────────
    'Set Filters':      props<{ filters: Partial<DocumentFilters> }>(),
    'Reset Filters':    emptyProps(),
    'Set View Mode':    props<{ mode: 'grid' | 'list' }>(),
    'Set Category Filter': props<{ category: string | null }>(),
  }
});
