export interface LegalCase {
  id: number;
  dossierNumber: string;
  category: string;
  clientInfo: string;
  status: string;
  estimatedDate: Date;
  clientCollaborationEnabled: boolean;
}
