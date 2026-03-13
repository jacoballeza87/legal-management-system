// src/app/features/documents/documents.module.ts
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Routes } from '@angular/router';
import { ReactiveFormsModule, FormsModule } from '@angular/forms';
import { StoreModule } from '@ngrx/store';
import { EffectsModule } from '@ngrx/effects';

import { documentsReducer } from '../../store/documents/reducers/documents.reducer';
import { DocumentsEffects } from '../../store/documents/effects/documents.effects';

import { DocumentRepositoryComponent } from './document-repository/document-repository.component';
import { DocumentUploadComponent }     from './document-upload/document-upload.component';
import { DocumentViewerComponent }     from './document-viewer/document-viewer.component';
import { FileUploadComponent }         from '../../shared/components/file-upload/file-upload.component';

import { AuthGuard } from '../../core/guards/auth.guard';
import { RoleGuard } from '../../core/guards/role.guard';

const routes: Routes = [
  {
    path: '',
    component: DocumentRepositoryComponent,
    canActivate: [AuthGuard]
  },
  {
    path: 'upload',
    component: DocumentUploadComponent,
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: ['LAWYER','ADMIN','SUPER_ADMIN','ACCOUNTANT'] }
  },
  {
    path: ':id',
    component: DocumentViewerComponent,
    canActivate: [AuthGuard]
  }
];

@NgModule({
  declarations: [
    DocumentRepositoryComponent,
    DocumentUploadComponent,
    DocumentViewerComponent,
    FileUploadComponent,
  ],
  imports: [
    CommonModule,
    ReactiveFormsModule,
    FormsModule,
    RouterModule.forChild(routes),
    StoreModule.forFeature('documents', documentsReducer),
    EffectsModule.forFeature([DocumentsEffects]),
  ],
  exports: [
    FileUploadComponent,    // exportar para uso en CaseDetail
  ]
})
export class DocumentsModule {}


// ════════════════════════════════════════════════════════════════════
// En app-routing.module.ts agrega:
// ════════════════════════════════════════════════════════════════════
/*
  {
    path: 'documents',
    loadChildren: () =>
      import('./features/documents/documents.module').then(m => m.DocumentsModule),
    canActivate: [AuthGuard]
  }
*/


// ════════════════════════════════════════════════════════════════════
// NOTA: instalar uuid para los localIds del upload queue
// ════════════════════════════════════════════════════════════════════
/*
  npm install uuid
  npm install --save-dev @types/uuid
*/
