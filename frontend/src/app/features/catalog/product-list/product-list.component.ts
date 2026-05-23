import { Component, OnInit } from '@angular/core';
import { Store } from '@ngrx/store';
import { Observable } from 'rxjs';
import { Product } from '../../../shared/models/product.model';
import { CartItem } from '../../../shared/models/cart.model';
import { loadProducts } from '../../../store/products/products.actions';
import { addToCart } from '../../../store/cart/cart.actions';
import {
  selectProducts,
  selectProductsLoading,
  selectProductsError,
  selectTotalPages,
  selectCurrentPage
} from '../../../store/products/products.selectors';

@Component({
  selector: 'app-product-list',
  templateUrl: './product-list.component.html'
})
export class ProductListComponent implements OnInit {
  products$: Observable<Product[]>;
  loading$: Observable<boolean>;
  error$: Observable<string | null>;
  totalPages$: Observable<number>;
  currentPage$: Observable<number>;
  currentQuery = '';

  constructor(private store: Store) {
    this.products$ = this.store.select(selectProducts);
    this.loading$ = this.store.select(selectProductsLoading);
    this.error$ = this.store.select(selectProductsError);
    this.totalPages$ = this.store.select(selectTotalPages);
    this.currentPage$ = this.store.select(selectCurrentPage);
  }

  ngOnInit(): void {
    this.store.dispatch(loadProducts({}));
  }

  onSearch(query: string): void {
    this.currentQuery = query;
    this.store.dispatch(loadProducts({ query: query || undefined, page: 0 }));
  }

  onPageChange(page: number): void {
    this.store.dispatch(loadProducts({ query: this.currentQuery || undefined, page }));
  }

  addToCart(product: Product): void {
    const item: CartItem = {
      skuId: product.id,
      productId: product.id,
      productName: product.name,
      quantity: 1,
      unitPrice: product.price
    };
    this.store.dispatch(addToCart({ item }));
  }
}
