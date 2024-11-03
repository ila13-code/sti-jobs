import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class JsonService {
  private state = {
    importJobs: false,
    importProjects: false,
    exportJobs: false,
    exportProjects: false
  };

  private baseUrl = 'http://localhost:7001/api/v1'; // URL del backend

  constructor(private http: HttpClient) { }

  public isImport(): boolean {
    return this.state.importJobs || this.state.importProjects;
  }

  public isJobs(): boolean {
    return this.state.importJobs || this.state.exportJobs;
  }

  public setAction(actionType: 'import' | 'export', entityType: 'Jobs' | 'Projects') {
    this.resetState();

    const key = `${actionType}${entityType}`;
    if (key in this.state) {
      this.state[key as keyof typeof this.state] = true;
    } else {
      console.warn(`Azione non riconosciuta: ${key}`);
    }
  }

  private resetState() {
    Object.keys(this.state).forEach(key => {
      this.state[key as keyof typeof this.state] = false;
    });
  }

  public getActionStatus() {
    return { ...this.state };
  }

  callJsonEndpoint(data: any): Observable<any> {
    return this.http.post<any>(`${this.baseUrl}/json/import`, data);
  }
}
