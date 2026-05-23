import { createReducer, on } from '@ngrx/store';
import { User } from '../../shared/models/user.model';
import {
  login,
  loginSuccess,
  loginFailure,
  logout,
  logoutSuccess,
  register,
  registerSuccess,
  registerFailure,
  restoreSession
} from './auth.actions';

export interface AuthState {
  user: User | null;
  accessToken: string | null;
  loading: boolean;
  error: string | null;
  registerSuccess: boolean;
}

export const initialAuthState: AuthState = {
  user: null,
  accessToken: null,
  loading: false,
  error: null,
  registerSuccess: false
};

export const authReducer = createReducer(
  initialAuthState,

  on(login, (state) => ({
    ...state,
    loading: true,
    error: null
  })),

  on(loginSuccess, (state, { tokens, user }) => ({
    ...state,
    loading: false,
    user,
    accessToken: tokens.accessToken,
    error: null
  })),

  on(loginFailure, (state, { error }) => ({
    ...state,
    loading: false,
    error
  })),

  on(logout, (state) => ({
    ...state,
    loading: true
  })),

  on(logoutSuccess, () => ({
    ...initialAuthState
  })),

  on(register, (state) => ({
    ...state,
    loading: true,
    error: null,
    registerSuccess: false
  })),

  on(registerSuccess, (state) => ({
    ...state,
    loading: false,
    registerSuccess: true,
    error: null
  })),

  on(registerFailure, (state, { error }) => ({
    ...state,
    loading: false,
    error,
    registerSuccess: false
  })),

  on(restoreSession, (state) => ({
    ...state
  }))
);
