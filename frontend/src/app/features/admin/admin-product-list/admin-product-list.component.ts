import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { Store } from '@ngrx/store';
import { Observable } from 'rxjs';
import { Product } from '../../../shared/models/product.model';
import { loadProducts, deleteProduct } from '../../../store/products/products.actions';
import { selectProducts, selectProductsLoading, selectSubmitting, selectProductsError } from '../../../store/products/products.selectors';

@Component({
  selector: 'app-admin-product-list',
  templateUrl: './admin-product-list.component.html'
})
export class AdminProductListComponent implements OnInit {
  products$: Observable<Product[]>;
  loading$: Observable<boolean>;
  submitting$: Observable<boolean>;
  error$: Observable<string | null>;

  constructor(private store: Store, private router: Router) {
    this.products$  = this.store.select(selectProducts);
    this.loading$   = this.store.select(selectProductsLoading);
    this.submitting$ = this.store.select(selectSubmitting);
    this.error$     = this.store.select(selectProductsError);
  }

  ngOnInit(): void {
    this.store.dispatch(loadProducts({ page: 0, size: 50 }));
  }

  edit(product: Product): void {
    this.router.navigate(['/admin/products', product.id, 'edit']);
  }

  delete(product: Product): void {
    if (confirm(`Delete "${product.name}"? This cannot be undone.`)) {
      this.store.dispatch(deleteProduct({ id: product.id }));
    }
  }
}
