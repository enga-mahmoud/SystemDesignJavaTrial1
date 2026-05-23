import { createReducer, on } from '@ngrx/store';
import { Product } from '../../shared/models/product.model';
import {
  loadProducts, loadProductsSuccess, loadProductsFailure,
  loadProduct, loadProductSuccess, loadProductFailure,
  clearSelectedProduct,
  createProduct, createProductSuccess, createProductFailure,
  updateProduct, updateProductSuccess, updateProductFailure,
  deleteProduct, deleteProductSuccess, deleteProductFailure
} from './products.actions';

export interface ProductsState {
  products: Product[];
  selectedProduct: Product | null;
  loading: boolean;
  submitting: boolean;
  error: string | null;
  totalElements: number;
  totalPages: number;
  currentPage: number;
}

export const initialProductsState: ProductsState = {
  products: [],
  selectedProduct: null,
  loading: false,
  submitting: false,
  error: null,
  totalElements: 0,
  totalPages: 0,
  currentPage: 0
};

export const productsReducer = createReducer(
  initialProductsState,

  on(loadProducts, (state, { page }) => ({ ...state, loading: true, error: null, currentPage: page ?? 0 })),
  on(loadProductsSuccess, (state, { products, totalElements, totalPages }) => ({
    ...state, loading: false, products, totalElements, totalPages, error: null
  })),
  on(loadProductsFailure, (state, { error }) => ({ ...state, loading: false, error })),

  on(loadProduct, (state) => ({ ...state, loading: true, error: null })),
  on(loadProductSuccess, (state, { product }) => ({ ...state, loading: false, selectedProduct: product, error: null })),
  on(loadProductFailure, (state, { error }) => ({ ...state, loading: false, error })),

  on(clearSelectedProduct, (state) => ({ ...state, selectedProduct: null })),

  on(createProduct, (state) => ({ ...state, submitting: true, error: null })),
  on(createProductSuccess, (state, { product }) => ({
    ...state, submitting: false, products: [product, ...state.products]
  })),
  on(createProductFailure, (state, { error }) => ({ ...state, submitting: false, error })),

  on(updateProduct, (state) => ({ ...state, submitting: true, error: null })),
  on(updateProductSuccess, (state, { product }) => ({
    ...state,
    submitting: false,
    products: state.products.map(p => p.id === product.id ? product : p),
    selectedProduct: state.selectedProduct?.id === product.id ? product : state.selectedProduct
  })),
  on(updateProductFailure, (state, { error }) => ({ ...state, submitting: false, error })),

  on(deleteProduct, (state) => ({ ...state, submitting: true, error: null })),
  on(deleteProductSuccess, (state, { id }) => ({
    ...state, submitting: false, products: state.products.filter(p => p.id !== id)
  })),
  on(deleteProductFailure, (state, { error }) => ({ ...state, submitting: false, error }))
);
