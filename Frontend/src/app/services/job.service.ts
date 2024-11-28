import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { JobDTO } from "../generated-api";

@Injectable({
  providedIn: 'root'
})
export class JobService {

  private baseUrl = 'http://localhost:7001/sti-jobs/api/v1/job';

  constructor(private http: HttpClient) { }

  showJob(): Observable<JobDTO[]> {
    const email = localStorage.getItem('user-email');
    if (!email) {
      throw new Error('User email not found in local storage');
    }
    return this.http.get<JobDTO[]>(`${this.baseUrl}/jobs-by-assignee-email/${email}`)
      .pipe(catchError(this.handleError));
  }

  deleteJob(id: number): Observable<any> {
    return this.http.delete(`${this.baseUrl}/${id}`)
      .pipe(catchError(this.handleError));
  }

  updateJob(job: JobDTO): Observable<any> {
    return this.http.put(`${this.baseUrl}/${job.id}`, job)
      .pipe(catchError(this.handleError));
  }

  getJobById(id: number): Observable<JobDTO> {
    return this.http.get<JobDTO>(`${this.baseUrl}/jobs-by-id/${id}`)
      .pipe(catchError(this.handleError));
  }

  private handleError(error: HttpErrorResponse) {
    if (error.status === 0) {
      console.error('An error occurred:', error.error);
    } else {
      console.error(
        `Backend returned code ${error.status}, body was: `, error.error);
    }
    return throwError(() => new Error('Something bad happened; please try again later.'));
  }
}
