import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { AuthService } from './auth';

@Injectable({
  providedIn: 'root'
})
export class ApiService {
  private apiUrl = 'http://localhost:8080/CV_project-1.0-SNAPSHOT/api';

  constructor(private http: HttpClient, private auth: AuthService) {}

  private getHeaders() {
    return new HttpHeaders({
      'Authorization': `Bearer ${this.auth.getToken()}`
    });
  }

  // Jobs
  getJobs() {
    return this.http.get(`${this.apiUrl}/jobs`, { headers: this.getHeaders() });
  }

  createJob(job: any) {
    return this.http.post(`${this.apiUrl}/jobs`, job, { headers: this.getHeaders() });
  }

  deleteJob(id: number) {
    return this.http.delete(`${this.apiUrl}/jobs/${id}`, { headers: this.getHeaders() });
  }

  // Applications
  getApplicationsForJob(jobId: number) {
    return this.http.get(`${this.apiUrl}/applications/job/${jobId}`, { headers: this.getHeaders() });
  }

  getMyApplications() {
    return this.http.get(`${this.apiUrl}/applications/me`, { headers: this.getHeaders() });
  }

  applyForJob(jobId: number) {
    return this.http.post(`${this.apiUrl}/applications`, { jobOfferId: jobId }, { headers: this.getHeaders() });
  }

  updateApplicationStatus(applicationId: number, status: string) {
    return this.http.put(`${this.apiUrl}/applications/${applicationId}/status`, { status }, { headers: this.getHeaders() });
  }

  // CV
  getMyCv() {
    return this.http.get(`${this.apiUrl}/cv/me`, { headers: this.getHeaders() });
  }

  uploadCv(fileName: string, base64Cv: string) {
    return this.http.post(`${this.apiUrl}/cv`, { fileName: fileName, base64Content: base64Cv }, { headers: this.getHeaders() });
  }

  downloadCv(candidateId: number) {
    return this.http.get(`${this.apiUrl}/cv/download/${candidateId}`, { headers: this.getHeaders(), responseType: 'blob' });
  }
}
