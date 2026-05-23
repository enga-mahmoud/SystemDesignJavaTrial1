import { ActionReducerMap, MetaReducer } from '@ngrx/store';
import { authReducer, AuthState } from './auth/auth.reducer';
import { productsReducer, ProductsState } from './products/products.reducer';
import { cartReducer, CartState, localStorageCartMetaReducer } from './cart/cart.reducer';

export interface AppState {
  auth: AuthState;
  products: ProductsState;
  cart: CartState;
}

export const reducers: ActionReducerMap<AppState> = {
  auth: authReducer,
  products: productsReducer,
  cart: cartReducer
};

export const metaReducers: MetaReducer<AppState>[] = [localStorageCartMetaReducer];
