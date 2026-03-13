// src/app/store/documents/effects/documents.effects.ts
import { Injectable } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { Store } from '@ngrx/store';
import { of } from 'rxjs';
import { catchError, exhaustMap, map, mergeMap, switchMap, tap, withLatestFrom } from 'rxjs/operators';
import { DocumentsActions } from '../actions/documents.actions';
import { selectFilters, selectUploadQueue } from '../selectors/documents.selectors';
import { DocumentService } from '../../../core/services/document.service';
import { UploadQueueItem, UploadDocumentRequest } from '../../../core/models/document.model';

@Injectable()
export class DocumentsEffects {

  constructor(
    private actions$: Actions,
    private documentService: DocumentService,
    private store: Store
  ) {}

  // ── LOAD LIST ──────────────────────────────────────────────────────
  loadDocuments$ = createEffect(() =>
    this.actions$.pipe(
      ofType(DocumentsActions.loadDocuments),
      switchMap(({ filters }) =>
        this.documentService.getDocuments(filters).pipe(
          map(paged => DocumentsActions.loadDocumentsSuccess({ paged })),
          catchError(err => of(DocumentsActions.loadDocumentsFailure({ error: err.message })))
        )
      )
    )
  );

  reloadOnFilters$ = createEffect(() =>
    this.actions$.pipe(
      ofType(DocumentsActions.setFilters, DocumentsActions.resetFilters, DocumentsActions.setCategoryFilter),
      withLatestFrom(this.store.select(selectFilters)),
      map(([, filters]) => DocumentsActions.loadDocuments({ filters }))
    )
  );

  // ── LOAD BY CASE ───────────────────────────────────────────────────
  loadCaseDocuments$ = createEffect(() =>
    this.actions$.pipe(
      ofType(DocumentsActions.loadCaseDocuments),
      switchMap(({ caseId }) =>
        this.documentService.getDocumentsByCaseId(caseId).pipe(
          map(documents => DocumentsActions.loadCaseDocumentsSuccess({ caseId, documents })),
          catchError(err => of(DocumentsActions.loadCaseDocumentsFailure({ error: err.message })))
        )
      )
    )
  );

  // ── STATS ──────────────────────────────────────────────────────────
  loadStats$ = createEffect(() =>
    this.actions$.pipe(
      ofType(DocumentsActions.loadStats),
      switchMap(() =>
        this.documentService.getStats().pipe(
          map(stats => DocumentsActions.loadStatsSuccess({ stats })),
          catchError(err => of(DocumentsActions.loadStatsFailure({ error: err.message })))
        )
      )
    )
  );

  // ── SELECT DOCUMENT ────────────────────────────────────────────────
  selectDocument$ = createEffect(() =>
    this.actions$.pipe(
      ofType(DocumentsActions.selectDocument),
      switchMap(({ id }) =>
        this.documentService.getDocumentById(id).pipe(
          map(document => DocumentsActions.selectDocumentSuccess({ document })),
          catchError(err => of(DocumentsActions.selectDocumentFailure({ error: err.message })))
        )
      )
    )
  );

  // ── UPLOAD ALL ─────────────────────────────────────────────────────
  // Cuando el usuario pulsa "Subir Todo" lanza uploads en paralelo
  uploadAll$ = createEffect(() =>
    this.actions$.pipe(
      ofType(DocumentsActions.uploadAll),
      withLatestFrom(this.store.select(selectUploadQueue)),
      mergeMap(([, queue]) => {
        const pending = queue.filter(i => i.status === 'pending');
        return pending.map(item => DocumentsActions.uploadDocument({ item }));
      })
    )
  );

  // ── SINGLE UPLOAD ──────────────────────────────────────────────────
  // mergeMap para permitir uploads paralelos
  uploadDocument$ = createEffect(() =>
    this.actions$.pipe(
      ofType(DocumentsActions.uploadDocument),
      mergeMap(({ item }) => {
        if (!item.caseId || !item.category) {
          return of(DocumentsActions.uploadDocumentFailure({
            localId: item.localId,
            error: 'Debes seleccionar un caso y categoría antes de subir'
          }));
        }

        const request: UploadDocumentRequest = {
          file: item.file,
          caseId: item.caseId,
          category: item.category,
          description: item.description,
        };

        return this.documentService.uploadDocument(request).pipe(
          map(result => {
            if (result.document) {
              return DocumentsActions.uploadDocumentSuccess({
                localId: item.localId,
                document: result.document
              });
            }
            return DocumentsActions.uploadProgress({
              localId: item.localId,
              progress: result.progress
            });
          }),
          catchError(err => of(DocumentsActions.uploadDocumentFailure({
            localId: item.localId,
            error: err.error?.message ?? err.message ?? 'Error al subir el archivo'
          })))
        );
      })
    )
  );

  // ── DOWNLOAD URL ───────────────────────────────────────────────────
  getDownloadUrl$ = createEffect(() =>
    this.actions$.pipe(
      ofType(DocumentsActions.getDownloadUrl),
      exhaustMap(({ id }) =>
        this.documentService.getDownloadUrl(id).pipe(
          tap(({ url }) => window.open(url, '_blank')),
          map(({ url }) => DocumentsActions.getDownloadUrlSuccess({ id, url })),
          catchError(err => of(DocumentsActions.getDownloadUrlFailure({ error: err.message })))
        )
      )
    )
  );

  // ── DELETE ─────────────────────────────────────────────────────────
  deleteDocument$ = createEffect(() =>
    this.actions$.pipe(
      ofType(DocumentsActions.deleteDocument),
      exhaustMap(({ id }) =>
        this.documentService.softDelete(id).pipe(
          map(() => DocumentsActions.deleteDocumentSuccess({ id })),
          catchError(err => of(DocumentsActions.deleteDocumentFailure({ error: err.message })))
        )
      )
    )
  );

  // ── RESTORE ────────────────────────────────────────────────────────
  restoreDocument$ = createEffect(() =>
    this.actions$.pipe(
      ofType(DocumentsActions.restoreDocument),
      exhaustMap(({ id }) =>
        this.documentService.restore(id).pipe(
          map(document => DocumentsActions.restoreDocumentSuccess({ document })),
          catchError(err => of(DocumentsActions.restoreDocumentFailure({ error: err.message })))
        )
      )
    )
  );
}
