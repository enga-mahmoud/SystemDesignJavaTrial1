import { Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { of } from 'rxjs';
import { catchError, map, switchMap, tap } from 'rxjs/operators';
import { AuthService } from '../../core/services/auth.service';
import { TokenStorageService } from '../../core/services/token-storage.service';
import { AuthTokens, User } from '../../shared/models/user.model';
import {
  login,
  loginSuccess,
  loginFailure,
  logout,
  logoutSuccess,
  register,
  registerSuccess,
  registerFailure
} from './auth.actions';

@Injectable()
export class AuthEffects {
  login$ = createEffect(() =>
    this.actions$.pipe(
      ofType(login),
      switchMap(({ email, password }) =>
        this.authService.login(email, password).pipe(
          map((tokens: AuthTokens) => {
            // Decode user info from the access token (JWT payload)
            let user: User = { id: '', email, username: email, role: 'USER' };
            try {
              const payload = JSON.parse(atob(tokens.accessToken.split('.')[1]));
              user = {
                id: payload.sub ?? '',
                email: payload.email ?? email,
                username: payload.username ?? email,
                role: payload.role ?? 'USER'
              };
            } catch {
              // fall back to defaults
            }
            this.tokenStorage.setTokens(tokens.accessToken, tokens.refreshToken);
            this.tokenStorage.setUser(user);
            return loginSuccess({ tokens, user });
          }),
          catchError((err) => {
            const error =
              err?.error?.message ?? err?.error?.error ?? err?.message ?? 'Login failed. Please try again.';
            return of(loginFailure({ error }));
          })
        )
      )
    )
  );

  loginSuccess$ = createEffect(
    () =>
      this.actions$.pipe(
        ofType(loginSuccess),
        tap(() => this.router.navigate(['/products']))
      ),
    { dispatch: false }
  );

  logout$ = createEffect(() =>
    this.actions$.pipe(
      ofType(logout),
      switchMap(() =>
        this.authService.logout().pipe(
          map(() => {
            this.tokenStorage.clear();
            return logoutSuccess();
          }),
          catchError(() => {
            this.tokenStorage.clear();
            return of(logoutSuccess());
          })
        )
      )
    )
  );

  logoutSuccess$ = createEffect(
    () =>
      this.actions$.pipe(
        ofType(logoutSuccess),
        tap(() => this.router.navigate(['/login']))
      ),
    { dispatch: false }
  );

  register$ = createEffect(() =>
    this.actions$.pipe(
      ofType(register),
      switchMap(({ email, username, password }) =>
        this.authService.register(email, username, password).pipe(
          map(() => registerSuccess()),
          catchError((err) => {
            const error =
              err?.error?.message ?? err?.error?.error ?? err?.message ?? 'Registration failed. Please try again.';
            return of(registerFailure({ error }));
          })
        )
      )
    )
  );

  registerSuccess$ = createEffect(
    () =>
      this.actions$.pipe(
        ofType(registerSuccess),
        tap(() => this.router.navigate(['/login']))
      ),
    { dispatch: false }
  );

  constructor(
    private actions$: Actions,
    private authService: AuthService,
    private tokenStorage: TokenStorageService,
    private router: Router
  ) {}
}
