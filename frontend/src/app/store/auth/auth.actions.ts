import { createAction, props } from '@ngrx/store';
import { User, AuthTokens } from '../../shared/models/user.model';

// Login
export const login = createAction(
  '[Auth] Login',
  props<{ email: string; password: string }>()
);

export const loginSuccess = createAction(
  '[Auth] Login Success',
  props<{ tokens: AuthTokens; user: User }>()
);

export const loginFailure = createAction(
  '[Auth] Login Failure',
  props<{ error: string }>()
);

// Logout
export const logout = createAction('[Auth] Logout');

export const logoutSuccess = createAction('[Auth] Logout Success');

// Register
export const register = createAction(
  '[Auth] Register',
  props<{ email: string; username: string; password: string }>()
);

export const registerSuccess = createAction('[Auth] Register Success');

export const registerFailure = createAction(
  '[Auth] Register Failure',
  props<{ error: string }>()
);

// Restore session from localStorage
export const restoreSession = createAction('[Auth] Restore Session');
