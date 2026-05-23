import { createFeatureSelector, createSelector } from '@ngrx/store';
import { AuthState } from './auth.reducer';

export const selectAuthState = createFeatureSelector<AuthState>('auth');

export const selectCurrentUser = createSelector(
  selectAuthState,
  (state) => state.user
);

export const selectAccessToken = createSelector(
  selectAuthState,
  (state) => state.accessToken
);

export const selectIsLoggedIn = createSelector(
  selectAuthState,
  (state) => !!state.accessToken && !!state.user
);

export const selectAuthLoading = createSelector(
  selectAuthState,
  (state) => state.loading
);

export const selectAuthError = createSelector(
  selectAuthState,
  (state) => state.error
);

export const selectRegisterSuccess = createSelector(
  selectAuthState,
  (state) => state.registerSuccess
);

export const selectIsAdmin = createSelector(
  selectCurrentUser,
  (user) => user?.role === 'ADMIN'
);
