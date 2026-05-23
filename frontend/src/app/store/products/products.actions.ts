import { createAction, props } from '@ngrx/store';
import { Product } from '../../shared/models/product.model';

export const loadProducts = createAction(
  '[Products] Load Products',
  props<{ query?: string; page?: number; size?: number }>()
);

export const loadProductsSuccess = createAction(
  '[Products] Load Products Success',
  props<{ products: Product[]; totalElements: number; totalPages: number }>()
);

export const loadProductsFailure = createAction(
  '[Products] Load Products Failure',
  props<{ error: string }>()
);

export const loadProduct = createAction(
  '[Products] Load Product',
  props<{ id: string }>()
);

export const loadProductSuccess = createAction(
  '[Products] Load Product Success',
  props<{ product: Product }>()
);

export const loadProductFailure = createAction(
  '[Products] Load Product Failure',
  props<{ error: string }>()
);

export const clearSelectedProduct = createAction(
  '[Products] Clear Selected Product'
);

export const createProduct = createAction(
  '[Products] Create Product',
  props<{ product: { name: string; description: string; price: number; categoryId?: string } }>()
);
export const createProductSuccess = createAction(
  '[Products] Create Product Success',
  props<{ product: Product }>()
);
export const createProductFailure = createAction(
  '[Products] Create Product Failure',
  props<{ error: string }>()
);

export const updateProduct = createAction(
  '[Products] Update Product',
  props<{ id: string; product: { name: string; description: string; price: number; categoryId?: string } }>()
);
export const updateProductSuccess = createAction(
  '[Products] Update Product Success',
  props<{ product: Product }>()
);
export const updateProductFailure = createAction(
  '[Products] Update Product Failure',
  props<{ error: string }>()
);

export const deleteProduct = createAction(
  '[Products] Delete Product',
  props<{ id: string }>()
);
export const deleteProductSuccess = createAction(
  '[Products] Delete Product Success',
  props<{ id: string }>()
);
export const deleteProductFailure = createAction(
  '[Products] Delete Product Failure',
  props<{ error: string }>()
);
