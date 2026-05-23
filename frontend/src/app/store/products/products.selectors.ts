import { createFeatureSelector, createSelector } from '@ngrx/store';
import { ProductsState } from './products.reducer';

export const selectProductsState = createFeatureSelector<ProductsState>('products');

export const selectProducts = createSelector(selectProductsState, s => s.products);
export const selectSelectedProduct = createSelector(selectProductsState, s => s.selectedProduct);
export const selectProductsLoading = createSelector(selectProductsState, s => s.loading);
export const selectProductsError = createSelector(selectProductsState, s => s.error);
export const selectTotalElements = createSelector(selectProductsState, s => s.totalElements);
export const selectTotalPages = createSelector(selectProductsState, s => s.totalPages);
export const selectCurrentPage = createSelector(selectProductsState, s => s.currentPage);
export const selectSubmitting = createSelector(selectProductsState, s => s.submitting);
