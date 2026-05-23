import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthTokens } from '../../shared/models/user.model';
import { TokenStorageService } from './token-storage.service';

@Injectable({ providedIn: 'root' })
export class AuthService {
  constructor(
    private http: HttpClient,
    private tokenStorage: TokenStorageService
  ) {}

  register(email: string, username: string, password: string): Observable<any> {
    return this.http.post(
      `${environment.apiUrl}/api/auth/register`,
      { email, username, password }
    );
  }

  login(email: string, password: string): Observable<AuthTokens> {
    return this.http.post<AuthTokens>(
      `${environment.apiUrl}/api/auth/login`,
      { email, password }
    );
  }

  refreshToken(refreshToken: string): Observable<AuthTokens> {
    const user = this.tokenStorage.getUser();
    return this.http.post<AuthTokens>(
      `${environment.apiUrl}/api/auth/refresh`,
      { userId: user?.id ?? '', refreshToken }
    );
  }

  logout(): Observable<any> {
    return this.http.post(`${environment.apiUrl}/api/auth/logout`, {});
  }
}
