// src/app/features/cases/cases.module.ts
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Routes } from '@angular/router';
import { ReactiveFormsModule, FormsModule } from '@angular/forms';
import { StoreModule } from '@ngrx/store';
import { EffectsModule } from '@ngrx/effects';

import { casesReducer } from '../../store/cases/reducers/cases.reducer';
import { CasesEffects } from '../../store/cases/effects/cases.effects';

import { CaseListComponent }   from './case-list/case-list.component';
import { CaseKanbanComponent } from './case-kanban/case-kanban.component';
import { CaseDetailComponent } from './case-detail/case-detail.component';
import { CaseFormComponent }   from './case-form/case-form.component';

// ── Guards ──────────────────────────────────────────────────────────
import { AuthGuard } from '../../core/guards/auth.guard';
import { RoleGuard } from '../../core/guards/role.guard';

const routes: Routes = [
  {
    path: '',
    component: CaseListComponent,
    canActivate: [AuthGuard]
  },
  {
    path: 'new',
    component: CaseFormComponent,
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: ['LAWYER', 'ADMIN', 'SUPER_ADMIN'] }
  },
  {
    path: ':id',
    component: CaseDetailComponent,
    canActivate: [AuthGuard]
  },
  {
    path: ':id/edit',
    component: CaseFormComponent,
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: ['LAWYER', 'ADMIN', 'SUPER_ADMIN'] }
  }
];

@NgModule({
  declarations: [
    CaseListComponent,
    CaseKanbanComponent,
    CaseDetailComponent,
    CaseFormComponent,
  ],
  imports: [
    CommonModule,
    ReactiveFormsModule,
    FormsModule,
    RouterModule.forChild(routes),
    StoreModule.forFeature('cases', casesReducer),
    EffectsModule.forFeature([CasesEffects]),
  ]
})
export class CasesModule {}


// ════════════════════════════════════════════════════════════════════
// app-routing.module.ts — agrega esta ruta al routing principal
// ════════════════════════════════════════════════════════════════════
/*
  {
    path: 'cases',
    loadChildren: () => import('./features/cases/cases.module').then(m => m.CasesModule),
    canActivate: [AuthGuard]
  }
*/


// ════════════════════════════════════════════════════════════════════
// app.module.ts — agrega CasesEffects al EffectsModule raíz
// ════════════════════════════════════════════════════════════════════
/*
  StoreModule.forRoot({ auth: authReducer, dashboard: dashboardReducer }),
  EffectsModule.forRoot([AuthEffects, DashboardEffects]),
  // Los casos se registran en el CasesModule como feature state
*/
