import { createFeatureSelector, createSelector } from '@ngrx/store';
import { CartState } from './cart.reducer';

export const selectCartState = createFeatureSelector<CartState>('cart');

export const selectCartItems = createSelector(selectCartState, s => s.items);
export const selectCartTotal = createSelector(selectCartState, s => s.total);
export const selectCartOrdering = createSelector(selectCartState, s => s.ordering);
export const selectCartError = createSelector(selectCartState, s => s.error);
export const selectCartItemCount = createSelector(selectCartState, s =>
  s.items.reduce((sum, item) => sum + item.quantity, 0)
);
