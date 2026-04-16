import { Injectable } from '@angular/core';
import { HttpRequest, HttpHandler, HttpEvent, HttpInterceptor, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { ToastService } from '../services/toast.service';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  constructor(private auth: AuthService, private router: Router, private toast: ToastService) {}

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    const token = this.auth.getToken();
    const authReq = token
      ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
      : req;

    return next.handle(authReq).pipe(
      catchError((err: HttpErrorResponse) => {
        if (err.status === 401) {
          this.auth.logout();
          this.toast.error('Session expired. Please login again.');
          this.router.navigate(['/auth/login']);
        } else if (err.status === 403) {
          this.toast.error('Access denied. Insufficient permissions.');
          this.router.navigate(['/access-denied']);
        } else if (err.status === 500) {
          this.toast.error('Server error. Please try again later.');
        } else if (err.status === 0) {
          this.toast.error('Cannot connect to server. Check your connection.');
        }
        return throwError(() => err);
      })
    );
  }
}
