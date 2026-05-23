import { Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { of } from 'rxjs';
import { catchError, map, switchMap, tap } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { Product } from '../../shared/models/product.model';
import {
  loadProducts, loadProductsSuccess, loadProductsFailure,
  loadProduct, loadProductSuccess, loadProductFailure,
  createProduct, createProductSuccess, createProductFailure,
  updateProduct, updateProductSuccess, updateProductFailure,
  deleteProduct, deleteProductSuccess, deleteProductFailure
} from './products.actions';

interface PageResponse { content: Product[]; totalElements: number; totalPages: number; }

@Injectable()
export class ProductsEffects {

  loadProducts$ = createEffect(() =>
    this.actions$.pipe(
      ofType(loadProducts),
      switchMap(({ query, page = 0, size = 12 }) => {
        let params = new HttpParams().set('page', String(page)).set('size', String(size));
        if (query) {
          params = params.set('q', query);
        }
        return this.http.get<PageResponse>(`${environment.apiUrl}/api/products`, { params }).pipe(
          map(res => loadProductsSuccess({ products: res.content ?? [], totalElements: res.totalElements ?? 0, totalPages: res.totalPages ?? 0 })),
          catchError(err => of(loadProductsFailure({ error: err?.error?.message ?? 'Search failed' })))
        );
      })
    )
  );

  loadProduct$ = createEffect(() =>
    this.actions$.pipe(
      ofType(loadProduct),
      switchMap(({ id }) =>
        this.http.get<Product>(`${environment.apiUrl}/api/products/${id}`).pipe(
          map(product => loadProductSuccess({ product })),
          catchError(err => of(loadProductFailure({ error: err?.error?.message ?? 'Product not found' })))
        )
      )
    )
  );

  createProduct$ = createEffect(() =>
    this.actions$.pipe(
      ofType(createProduct),
      switchMap(({ product }) =>
        this.http.post<Product>(`${environment.apiUrl}/api/products`, product).pipe(
          map(p => createProductSuccess({ product: p })),
          catchError(err => of(createProductFailure({ error: err?.error?.error ?? err?.error?.message ?? 'Failed to create product' })))
        )
      )
    )
  );

  createProductSuccess$ = createEffect(() =>
    this.actions$.pipe(
      ofType(createProductSuccess),
      tap(() => this.router.navigate(['/admin']))
    ),
    { dispatch: false }
  );

  updateProduct$ = createEffect(() =>
    this.actions$.pipe(
      ofType(updateProduct),
      switchMap(({ id, product }) =>
        this.http.put<Product>(`${environment.apiUrl}/api/products/${id}`, product).pipe(
          map(p => updateProductSuccess({ product: p })),
          catchError(err => of(updateProductFailure({ error: err?.error?.error ?? err?.error?.message ?? 'Failed to update product' })))
        )
      )
    )
  );

  updateProductSuccess$ = createEffect(() =>
    this.actions$.pipe(
      ofType(updateProductSuccess),
      tap(() => this.router.navigate(['/admin']))
    ),
    { dispatch: false }
  );

  deleteProduct$ = createEffect(() =>
    this.actions$.pipe(
      ofType(deleteProduct),
      switchMap(({ id }) =>
        this.http.delete(`${environment.apiUrl}/api/products/${id}`).pipe(
          map(() => deleteProductSuccess({ id })),
          catchError(err => of(deleteProductFailure({ error: err?.error?.error ?? err?.error?.message ?? 'Failed to delete product' })))
        )
      )
    )
  );

  constructor(private actions$: Actions, private http: HttpClient, private router: Router) {}
}
