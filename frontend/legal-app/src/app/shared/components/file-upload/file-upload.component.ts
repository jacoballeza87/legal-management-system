// src/app/shared/components/file-upload/file-upload.component.ts
import {
  Component, EventEmitter, Input, Output, HostListener, ElementRef
} from '@angular/core';
import { ALLOWED_MIME_TYPES, MAX_FILE_SIZE_BYTES, UploadQueueItem } from '../../../core/models/document.model';

@Component({
  selector: 'app-file-upload',
  template: `
    <div class="upload-zone"
         [class.drag-over]="isDragging"
         [class.has-files]="files.length > 0"
         (click)="fileInput.click()">

      <input #fileInput type="file" multiple [accept]="acceptString"
             style="display:none" (change)="onFileInputChange($event)">

      <div class="zone-content" *ngIf="!isDragging">
        <span class="zone-icon">📁</span>
        <div class="zone-title">Arrastra los archivos aquí</div>
        <div class="zone-sub">o haz clic para seleccionar</div>
        <div class="zone-types">
          <span *ngFor="let t of allowedLabels" class="type-badge">{{ t }}</span>
        </div>
        <div class="zone-limit">Máximo {{ maxSizeMB }} MB por archivo</div>
      </div>

      <div class="zone-content dragging-state" *ngIf="isDragging">
        <span class="zone-icon">📂</span>
        <div class="zone-title">Suelta los archivos aquí</div>
      </div>
    </div>

    <!-- Validation errors -->
    <div class="validation-errors" *ngIf="validationErrors.length">
      <div *ngFor="let e of validationErrors" class="val-error">⚠ {{ e }}</div>
    </div>
  `
})
export class FileUploadComponent {
  @Input() maxSizeMB = 50;
  @Output() filesSelected = new EventEmitter<File[]>();

  isDragging = false;
  validationErrors: string[] = [];

  get acceptString(): string {
    return Object.keys(ALLOWED_MIME_TYPES).join(',');
  }

  get allowedLabels(): string[] {
    return [...new Set(Object.values(ALLOWED_MIME_TYPES))];
  }

  files: File[] = [];

  @HostListener('dragover', ['$event'])
  onDragOver(e: DragEvent) {
    e.preventDefault();
    e.stopPropagation();
    this.isDragging = true;
  }

  @HostListener('dragleave', ['$event'])
  onDragLeave(e: DragEvent) {
    e.preventDefault();
    e.stopPropagation();
    // Solo desactivar si el puntero salió del componente completamente
    if (!this.el.nativeElement.contains(e.relatedTarget as Node)) {
      this.isDragging = false;
    }
  }

  @HostListener('drop', ['$event'])
  onDrop(e: DragEvent) {
    e.preventDefault();
    e.stopPropagation();
    this.isDragging = false;
    const droppedFiles = Array.from(e.dataTransfer?.files ?? []);
    this.processFiles(droppedFiles);
  }

  onFileInputChange(e: Event) {
    const input = e.target as HTMLInputElement;
    const selected = Array.from(input.files ?? []);
    this.processFiles(selected);
    input.value = ''; // reset para permitir seleccionar el mismo archivo de nuevo
  }

  private processFiles(files: File[]) {
    this.validationErrors = [];
    const valid: File[] = [];
    const maxBytes = this.maxSizeMB * 1024 * 1024;

    files.forEach(file => {
      // Validar tipo MIME
      if (!ALLOWED_MIME_TYPES[file.type] && !this.isAllowedByExtension(file.name)) {
        this.validationErrors.push(`${file.name}: tipo de archivo no permitido`);
        return;
      }
      // Validar tamaño
      if (file.size > maxBytes) {
        this.validationErrors.push(`${file.name}: supera el límite de ${this.maxSizeMB} MB`);
        return;
      }
      valid.push(file);
    });

    if (valid.length) {
      this.files = [...this.files, ...valid];
      this.filesSelected.emit(valid);
    }
  }

  private isAllowedByExtension(filename: string): boolean {
    const ext = filename.split('.').pop()?.toLowerCase() ?? '';
    const allowed = ['pdf','docx','xlsx','doc','xls','png','jpg','jpeg','txt','zip','rar'];
    return allowed.includes(ext);
  }

  constructor(private el: ElementRef) {}
}
