// src/app/features/cases/case-form/case-form.component.ts
import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Store } from '@ngrx/store';
import { Subject } from 'rxjs';
import { takeUntil, filter } from 'rxjs/operators';
import { FormGroup, FormControl, Validators } from '@angular/forms';
import { CasesActions } from '../../../store/cases/actions/cases.actions';
import { selectSelectedCase, selectLoadingCreate, selectError } from '../../../store/cases/selectors/cases.selectors';
import { CaseType, CasePriority, CreateCaseRequest, UpdateCaseRequest } from '../../../core/models/case.model';

@Component({
  selector: 'app-case-form',
  template: `
    <div class="form-page">
      <button class="back-btn" (click)="goBack()">← {{ isEdit ? 'Cancelar edición' : 'Cancelar' }}</button>

      <div class="page-header">
        <div>
          <h1>{{ isEdit ? 'Editar Caso' : 'Nuevo Caso' }}</h1>
          <p class="subtitle">{{ isEdit ? 'Actualiza la información del expediente' : 'Completa los datos para crear el expediente' }}</p>
        </div>
      </div>

      <!-- Error banner -->
      <div class="error-banner" *ngIf="error$ | async as error">⚠ {{ error }}</div>

      <form [formGroup]="form" (ngSubmit)="onSubmit()">

        <!-- Información Principal -->
        <div class="card">
          <div class="card-header">
            <div class="card-title">📋 Información Principal</div>
          </div>
          <div class="form-grid">
            <div class="form-group full">
              <label>Título del Caso *</label>
              <input formControlName="title" placeholder="Ej: Demanda laboral por despido injustificado">
              <span class="field-error" *ngIf="f['title'].touched && f['title'].invalid">
                El título es requerido
              </span>
            </div>

            <div class="form-group">
              <label>Cliente *</label>
              <input formControlName="clientName" placeholder="Nombre del cliente o empresa">
            </div>

            <div class="form-group">
              <label>Abogado Principal *</label>
              <select formControlName="assignedLawyerId">
                <option value="">Seleccionar abogado...</option>
                <option value="1">Ana López Martínez</option>
                <option value="2">Carlos Ruiz Sánchez</option>
                <option value="3">María García López</option>
              </select>
            </div>

            <div class="form-group">
              <label>Tipo de Caso *</label>
              <select formControlName="caseType">
                <option value="">Seleccionar tipo...</option>
                <option *ngFor="let t of types" [value]="t">{{ t }}</option>
              </select>
            </div>

            <div class="form-group">
              <label>Prioridad *</label>
              <select formControlName="priority">
                <option *ngFor="let p of priorities" [value]="p">{{ p }}</option>
              </select>
            </div>

            <div class="form-group">
              <label>Fecha de Vencimiento</label>
              <input type="date" formControlName="dueDate">
            </div>

            <div class="form-group">
              <label>Juzgado / Tribunal</label>
              <input formControlName="courtName" placeholder="Ej: Tribunal Civil #3">
            </div>

            <div class="form-group">
              <label>N° Expediente Externo</label>
              <input formControlName="externalExpedientNumber" placeholder="Ej: EXT-2024-44521">
            </div>

            <div class="form-group full">
              <label>Descripción del Caso</label>
              <textarea formControlName="description"
                        placeholder="Descripción detallada de los hechos, antecedentes y objeto del litigio...">
              </textarea>
            </div>
          </div>
        </div>

        <!-- Colaboradores -->
        <div class="card" *ngIf="!isEdit">
          <div class="card-header">
            <div class="card-title">👥 Colaboradores (opcional)</div>
          </div>
          <div class="form-grid">
            <div class="form-group">
              <label>Agregar colaborador</label>
              <select [(ngModel)]="selectedCollaborator" [ngModelOptions]="{standalone:true}">
                <option value="">Seleccionar usuario...</option>
                <option value="2">Carlos Ruiz</option>
                <option value="3">María García</option>
                <option value="4">Pedro Sánchez</option>
              </select>
            </div>
            <div class="form-group">
              <label>Rol en el caso</label>
              <select [(ngModel)]="selectedCollabRole" [ngModelOptions]="{standalone:true}">
                <option value="VIEWER">VIEWER</option>
                <option value="COLLABORATOR">COLLABORATOR</option>
                <option value="ACCOUNTANT">ACCOUNTANT</option>
              </select>
            </div>
          </div>
          <button type="button" class="btn btn-outline btn-sm"
                  (click)="addCollaborator()"
                  [disabled]="!selectedCollaborator">
            + Agregar al caso
          </button>

          <div class="collaborators-preview" *ngIf="collaborators.length">
            <div *ngFor="let c of collaborators; let i = index" class="collab-chip">
              <span>{{ c.label }}</span>
              <span class="chip-role">{{ c.role }}</span>
              <button type="button" (click)="removeCollaborator(i)">✕</button>
            </div>
          </div>
        </div>

        <!-- Acciones -->
        <div class="form-actions">
          <button type="button" class="btn btn-outline" (click)="goBack()">Cancelar</button>
          <button type="button" class="btn btn-outline" *ngIf="!isEdit" (click)="saveDraft()">
            Guardar borrador
          </button>
          <button type="submit" class="btn btn-gold"
                  [disabled]="form.invalid || (loading$ | async)">
            <span *ngIf="loading$ | async">Guardando...</span>
            <span *ngIf="!(loading$ | async)">
              {{ isEdit ? '✓ Actualizar Caso' : '✓ Crear Caso' }}
            </span>
          </button>
        </div>

      </form>
    </div>
  `
})
export class CaseFormComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();
  private caseId: number | null = null;

  isEdit = false;
  loading$ = this.store.select(selectLoadingCreate);
  error$   = this.store.select(selectError);

  types: CaseType[]        = ['CIVIL','PENAL','LABORAL','MERCANTIL','FAMILIAR','ADMINISTRATIVO'];
  priorities: CasePriority[] = ['URGENTE','ALTA','MEDIA','BAJA'];

  selectedCollaborator = '';
  selectedCollabRole = 'VIEWER';
  collaborators: { id: number; label: string; role: string }[] = [];

  form = new FormGroup({
    title:                    new FormControl('', [Validators.required, Validators.minLength(5)]),
    clientName:               new FormControl('', [Validators.required]),
    assignedLawyerId:         new FormControl('', [Validators.required]),
    caseType:                 new FormControl('', [Validators.required]),
    priority:                 new FormControl('MEDIA', [Validators.required]),
    dueDate:                  new FormControl(''),
    courtName:                new FormControl(''),
    externalExpedientNumber:  new FormControl(''),
    description:              new FormControl('')
  });

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private store: Store
  ) {}

  ngOnInit(): void {
    this.route.params.pipe(takeUntil(this.destroy$)).subscribe(params => {
      if (params['id']) {
        this.isEdit = true;
        this.caseId = +params['id'];
        this.store.dispatch(CasesActions.loadCase({ id: this.caseId }));
        this.store.select(selectSelectedCase).pipe(
          filter(c => !!c),
          takeUntil(this.destroy$)
        ).subscribe(c => {
          if (c) this.form.patchValue(c as any);
        });
      }
    });
  }

  ngOnDestroy(): void { this.destroy$.next(); this.destroy$.complete(); }

  onSubmit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }

    const val = this.form.value;
    if (this.isEdit && this.caseId) {
      const req: UpdateCaseRequest = { ...val as any };
      this.store.dispatch(CasesActions.updateCase({ id: this.caseId, request: req }));
    } else {
      const req: CreateCaseRequest = {
        ...val as any,
        assignedLawyerId: +val.assignedLawyerId!,
        collaboratorIds: this.collaborators.map(c => c.id)
      };
      this.store.dispatch(CasesActions.createCase({ request: req }));
    }
  }

  saveDraft(): void {
    // Guardar como borrador sin validación completa
    const req: CreateCaseRequest = { ...this.form.value as any, status: 'ABIERTO' } as any;
    this.store.dispatch(CasesActions.createCase({ request: req }));
  }

  addCollaborator(): void {
    if (!this.selectedCollaborator) return;
    this.collaborators.push({
      id: +this.selectedCollaborator,
      label: `Usuario #${this.selectedCollaborator}`,
      role: this.selectedCollabRole
    });
    this.selectedCollaborator = '';
  }

  removeCollaborator(i: number): void { this.collaborators.splice(i, 1); }

  goBack(): void { this.router.navigate([this.isEdit && this.caseId ? `/cases/${this.caseId}` : '/cases']); }

  get f() { return this.form.controls; }
}
