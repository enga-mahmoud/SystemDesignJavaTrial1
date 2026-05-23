import { Injectable } from '@angular/core';
import {
  HttpErrorResponse,
  HttpEvent,
  HttpHandler,
  HttpInterceptor,
  HttpRequest
} from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, throwError } from 'rxjs';
import { catchError, switchMap } from 'rxjs/operators';
import { TokenStorageService } from '../services/token-storage.service';
import { AuthService } from '../services/auth.service';
import { AuthTokens } from '../../shared/models/user.model';

@Injectable()
export class ErrorInterceptor implements HttpInterceptor {
  constructor(
    private tokenStorage: TokenStorageService,
    private authService: AuthService,
    private router: Router
  ) {}

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    return next.handle(req).pipe(
      catchError((error: HttpErrorResponse) => {
        if (error.status === 401) {
          const refreshToken = this.tokenStorage.getRefreshToken();
          if (refreshToken) {
            return this.authService.refreshToken(refreshToken).pipe(
              switchMap((tokens: AuthTokens) => {
                this.tokenStorage.setTokens(tokens.accessToken, tokens.refreshToken);
                const retryReq = req.clone({
                  setHeaders: { Authorization: `Bearer ${tokens.accessToken}` }
                });
                return next.handle(retryReq);
              }),
              catchError((refreshError) => {
                this.tokenStorage.clear();
                this.router.navigate(['/login']);
                return throwError(() => refreshError);
              })
            );
          } else {
            this.tokenStorage.clear();
            this.router.navigate(['/login']);
          }
        }
        return throwError(() => error);
      })
    );
  }
}
