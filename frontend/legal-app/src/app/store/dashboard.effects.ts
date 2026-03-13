import { Injectable } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { forkJoin, of } from 'rxjs';
import { switchMap, map, catchError } from 'rxjs/operators';
import { DashboardService } from '../dashboard.service';
import * as A from './dashboard.actions';

@Injectable()
export class DashboardEffects {
  constructor(private actions$: Actions, private svc: DashboardService) {}

  load$ = createEffect(() => this.actions$.pipe(
    ofType(A.loadDashboard, A.refreshDashboard),
    switchMap(() =>
      forkJoin({
        stats:         this.svc.getDashboardStats(),
        recentCases:   this.svc.getRecentCases(6),
        notifications: this.svc.getRecentNotifications(5)
      }).pipe(
        map(d => A.loadDashboardSuccess(d)),
        catchError(err => of(A.loadDashboardFailure({
          error: err.error?.message || 'Error cargando el dashboard'
        })))
      )
    )
  ));

  activity$ = createEffect(() => this.actions$.pipe(
    ofType(A.loadActivityFeed),
    switchMap(() =>
      this.svc.getActivityFeed(10).pipe(
        map(items => A.loadActivityFeedSuccess({ items })),
        catchError(() => of(A.loadActivityFeedSuccess({ items: [] })))
      )
    )
  ));
}
