import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { AuthGuard } from './core/guards/auth.guard';

const routes: Routes = [
  { path: '',        redirectTo: 'dashboard', pathMatch: 'full' },
  {
    path: 'auth',
    loadChildren: () => import('./features/auth/auth.module').then(m => m.AuthModule)
  },
{ path: 'cases', loadChildren: () => import('./features/cases/cases.module').then(m => m.CasesModule) },
  {
    path: 'dashboard',
    loadChildren: () => import('./features/dashboard/dashboard.module').then(m => m.DashboardModule),
    canActivate: [AuthGuard]
  },
  {
    path: 'cases',
    loadChildren: () => import('./features/cases/cases.module').then(m => m.CasesModule),
    canActivate: [AuthGuard]
  },
  {
    path: 'users',
    loadChildren: () => import('./features/users/users.module').then(m => m.UsersModule),
    canActivate: [AuthGuard]
  },
  {
    path: 'documents',
    loadChildren: () => import('./features/documents/documents.module').then(m => m.DocumentsModule),
    canActivate: [AuthGuard]
  },
  { path: 'forbidden', redirectTo: 'auth/login' },
  { path: '**',        redirectTo: 'auth/login' }
];

@NgModule({
  imports: [RouterModule.forRoot(routes, {
    scrollPositionRestoration: 'top',
    bindToComponentInputs: true
  })],
  exports: [RouterModule]
})
export class AppRoutingModule {}
