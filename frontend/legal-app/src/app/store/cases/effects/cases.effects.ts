// src/app/store/cases/effects/cases.effects.ts
import { Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { Store } from '@ngrx/store';
import { of } from 'rxjs';
import { catchError, exhaustMap, map, switchMap, tap, withLatestFrom } from 'rxjs/operators';
import { CasesActions } from '../actions/cases.actions';
import { selectFilters } from '../selectors/cases.selectors';
import { CaseService } from '../../../core/services/case.service';

@Injectable()
export class CasesEffects {

  constructor(
    private actions$: Actions,
    private caseService: CaseService,
    private store: Store,
    private router: Router
  ) {}

  // ── LOAD LIST ──────────────────────────────────────────────────────
  loadCases$ = createEffect(() =>
    this.actions$.pipe(
      ofType(CasesActions.loadCases),
      switchMap(({ filters }) =>
        this.caseService.getCases(filters).pipe(
          map(paged => CasesActions.loadCasesSuccess({ paged, filters })),
          catchError(err => of(CasesActions.loadCasesFailure({ error: err.message })))
        )
      )
    )
  );

  // Cuando cambian filtros → recargar lista
  reloadOnFiltersChange$ = createEffect(() =>
    this.actions$.pipe(
      ofType(CasesActions.setFilters, CasesActions.resetFilters),
      withLatestFrom(this.store.select(selectFilters)),
      map(([, filters]) => CasesActions.loadCases({ filters }))
    )
  );

  // ── STATS ──────────────────────────────────────────────────────────
  loadStats$ = createEffect(() =>
    this.actions$.pipe(
      ofType(CasesActions.loadStats),
      switchMap(() =>
        this.caseService.getStats().pipe(
          map(stats => CasesActions.loadStatsSuccess({ stats })),
          catchError(err => of(CasesActions.loadStatsFailure({ error: err.message })))
        )
      )
    )
  );

  // ── DETAIL ────────────────────────────────────────────────────────
  loadCase$ = createEffect(() =>
    this.actions$.pipe(
      ofType(CasesActions.loadCase),
      switchMap(({ id }) =>
        this.caseService.getCaseById(id).pipe(
          map(c => CasesActions.loadCaseSuccess({ case: c })),
          catchError(err => of(CasesActions.loadCaseFailure({ error: err.message })))
        )
      )
    )
  );

  // ── CREATE ────────────────────────────────────────────────────────
  createCase$ = createEffect(() =>
    this.actions$.pipe(
      ofType(CasesActions.createCase),
      exhaustMap(({ request }) =>
        this.caseService.createCase(request).pipe(
          map(c => CasesActions.createCaseSuccess({ case: c })),
          catchError(err => of(CasesActions.createCaseFailure({ error: err.message })))
        )
      )
    )
  );

  // Navegar al detalle después de crear
  navigateAfterCreate$ = createEffect(() =>
    this.actions$.pipe(
      ofType(CasesActions.createCaseSuccess),
      tap(({ case: c }) => this.router.navigate(['/cases', c.id]))
    ),
    { dispatch: false }
  );

  // ── UPDATE ────────────────────────────────────────────────────────
  updateCase$ = createEffect(() =>
    this.actions$.pipe(
      ofType(CasesActions.updateCase),
      exhaustMap(({ id, request }) =>
        this.caseService.updateCase(id, request).pipe(
          map(c => CasesActions.updateCaseSuccess({ case: c })),
          catchError(err => of(CasesActions.updateCaseFailure({ error: err.message })))
        )
      )
    )
  );

  // ── DELETE ────────────────────────────────────────────────────────
  deleteCase$ = createEffect(() =>
    this.actions$.pipe(
      ofType(CasesActions.deleteCase),
      exhaustMap(({ id }) =>
        this.caseService.deleteCase(id).pipe(
          map(() => CasesActions.deleteCaseSuccess({ id })),
          catchError(err => of(CasesActions.deleteCaseFailure({ error: err.message })))
        )
      )
    )
  );

  navigateAfterDelete$ = createEffect(() =>
    this.actions$.pipe(
      ofType(CasesActions.deleteCaseSuccess),
      tap(() => this.router.navigate(['/cases']))
    ),
    { dispatch: false }
  );

  // ── KANBAN MOVE ───────────────────────────────────────────────────
  moveCaseStatus$ = createEffect(() =>
    this.actions$.pipe(
      ofType(CasesActions.moveCaseStatus),
      exhaustMap(({ id, newStatus }) =>
        this.caseService.updateCase(id, { status: newStatus as any }).pipe(
          map(c => CasesActions.moveCaseStatusSuccess({ case: c })),
          catchError(err => of(CasesActions.moveCaseStatusFailure({ error: err.message })))
        )
      )
    )
  );

  // ── VERSIONS ──────────────────────────────────────────────────────
  loadVersions$ = createEffect(() =>
    this.actions$.pipe(
      ofType(CasesActions.loadVersions),
      switchMap(({ caseId }) =>
        this.caseService.getVersions(caseId).pipe(
          map(versions => CasesActions.loadVersionsSuccess({ caseId, versions })),
          catchError(err => of(CasesActions.loadVersionsFailure({ error: err.message })))
        )
      )
    )
  );

  createVersion$ = createEffect(() =>
    this.actions$.pipe(
      ofType(CasesActions.createVersion),
      exhaustMap(({ caseId, request }) =>
        this.caseService.createVersion(caseId, request).pipe(
          map(version => CasesActions.createVersionSuccess({ caseId, version })),
          catchError(err => of(CasesActions.createVersionFailure({ error: err.message })))
        )
      )
    )
  );
}
