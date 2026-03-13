// src/app/core/models/document.model.ts

export type DocumentStatus   = 'UPLOADING' | 'ACTIVE' | 'ARCHIVED' | 'DELETED' | 'VIRUS_DETECTED';
export type DocumentCategory =
  'CONTRATO' | 'DEMANDA' | 'SENTENCIA' | 'EVIDENCIA' |
  'CORRESPONDENCIA' | 'FACTURA' | 'PODER_NOTARIAL' |
  'IDENTIFICACION' | 'OTRO';

export interface CaseDocument {
  id: number;
  caseId: number;
  caseNumber: string;
  documentKey: string;           // UUID único del documento
  originalFileName: string;
  mimeType: string;              // verificado por Apache Tika
  fileSize: number;              // bytes
  fileSizeFormatted?: string;    // "2.4 MB" — calculado en frontend
  s3Key: string;                 // cases/{caseNumber}/documents/{uuid}/{filename}
  s3BucketName: string;
  googleDriveFileId?: string;
  googleDriveFolderId?: string;
  description?: string;
  uploadedByUserId: number;
  uploadedByName?: string;
  status: DocumentStatus;
  category: DocumentCategory;
  version: number;
  previousVersionId?: number;
  checksum: string;              // MD5
  downloadUrl?: string;          // presigned URL — generada al obtener el doc
  uploadedAt: string;
  updatedAt: string;
  deletedAt?: string;
}

export interface DocumentStats {
  totalDocuments: number;
  activeDocuments: number;
  totalSizeBytes: number;
  totalSizeFormatted: string;
  byCategory: Record<DocumentCategory, number>;
  byMimeType: Record<string, number>;
  casesWithDocuments: number;
}

export interface DocumentFilters {
  caseId?: number;
  category?: DocumentCategory;
  status?: DocumentStatus;
  search?: string;
  uploadedByUserId?: number;
  page: number;
  size: number;
}

export interface PagedDocuments {
  content: CaseDocument[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

// ── Upload ──────────────────────────────────────────────────────────

export interface UploadDocumentRequest {
  caseId: number;
  category: DocumentCategory;
  description?: string;
  file: File;                    // manejado por FormData
}

// Estado local de cada archivo en la cola de upload
export type UploadStatus = 'pending' | 'validating' | 'uploading' | 'done' | 'error';

export interface UploadQueueItem {
  localId: string;               // UUID temporal local
  file: File;
  caseId?: number;
  category?: DocumentCategory;
  description?: string;
  progress: number;              // 0-100
  status: UploadStatus;
  errorMessage?: string;
  uploadedDoc?: CaseDocument;    // resultado tras upload exitoso
}

// ── S3 Info (admin) ─────────────────────────────────────────────────

export interface S3BucketInfo {
  bucketName: string;
  region: string;
  purpose: string;
  versioning: boolean;
  totalFiles: number;
  totalSizeBytes: number;
}

// ── Allowed file types ───────────────────────────────────────────────

export const ALLOWED_MIME_TYPES: Record<string, string> = {
  'application/pdf':                                                   'PDF',
  'application/vnd.openxmlformats-officedocument.wordprocessingml.document': 'DOCX',
  'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet':       'XLSX',
  'application/msword':                                                'DOC',
  'application/vnd.ms-excel':                                          'XLS',
  'image/png':   'PNG',
  'image/jpeg':  'JPG',
  'text/plain':  'TXT',
  'application/zip':  'ZIP',
  'application/x-rar-compressed': 'RAR',
};

export const MAX_FILE_SIZE_BYTES = 50 * 1024 * 1024; // 50 MB

export const CATEGORY_LABELS: Record<DocumentCategory, string> = {
  CONTRATO:        'Contrato',
  DEMANDA:         'Demanda',
  SENTENCIA:       'Sentencia',
  EVIDENCIA:       'Evidencia',
  CORRESPONDENCIA: 'Correspondencia',
  FACTURA:         'Factura',
  PODER_NOTARIAL:  'Poder Notarial',
  IDENTIFICACION:  'Identificación',
  OTRO:            'Otro',
};

export const CATEGORY_ICONS: Record<DocumentCategory, string> = {
  CONTRATO:        '📝',
  DEMANDA:         '⚖️',
  SENTENCIA:       '🔨',
  EVIDENCIA:       '🔍',
  CORRESPONDENCIA: '✉️',
  FACTURA:         '💰',
  PODER_NOTARIAL:  '📜',
  IDENTIFICACION:  '🪪',
  OTRO:            '📁',
};
