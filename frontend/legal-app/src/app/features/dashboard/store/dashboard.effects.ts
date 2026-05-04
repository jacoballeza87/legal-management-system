import { Injectable } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { HttpClient } from '@angular/common/http';
import { catchError, map, mergeMap, of } from 'rxjs';
import * as A from './dashboard.actions';
import { environment } from '../../../../environments/environment';

@Injectable()
export class DashboardEffects {

  private base = environment.apiUrl;

  loadDashboard$ = createEffect(() =>
    this.actions$.pipe(
      ofType(A.loadDashboard, A.refreshDashboard),
      mergeMap(() =>
        this.http.get<any>(`${this.base}/cases/stats`).pipe(
          map(data => A.loadDashboardSuccess({
            stats:         data.stats         ?? {},
            recentCases:   data.recentCases   ?? [],
            notifications: data.notifications ?? [],
            casesByStatus: data.casesByStatus ?? {},
          })),
          catchError(err => of(A.loadDashboardFailure({ error: err.message ?? 'Error loading dashboard' })))
        )
      )
    )
  );

  loadActivity$ = createEffect(() =>
    this.actions$.pipe(
      ofType(A.loadActivityFeed),
      mergeMap(() =>
        this.http.get<any[]>(`${this.base}/cases/activity`).pipe(
          map(activity => A.loadActivityFeedSuccess({ activity: activity ?? [] })),
          catchError(() => of(A.loadActivityFeedSuccess({ activity: [] })))
        )
      )
    )
  );

  constructor(private actions$: Actions, private http: HttpClient) {}
}
