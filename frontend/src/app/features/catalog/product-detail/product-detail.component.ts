import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Store } from '@ngrx/store';
import { Observable } from 'rxjs';
import { Product } from '../../../shared/models/product.model';
import { CartItem } from '../../../shared/models/cart.model';
import { loadProduct, clearSelectedProduct } from '../../../store/products/products.actions';
import { addToCart } from '../../../store/cart/cart.actions';
import { selectSelectedProduct, selectProductsLoading, selectProductsError } from '../../../store/products/products.selectors';

@Component({
  selector: 'app-product-detail',
  templateUrl: './product-detail.component.html'
})
export class ProductDetailComponent implements OnInit, OnDestroy {
  product$: Observable<Product | null>;
  loading$: Observable<boolean>;
  error$: Observable<string | null>;
  quantity = 1;

  constructor(private route: ActivatedRoute, private store: Store) {
    this.product$ = this.store.select(selectSelectedProduct);
    this.loading$ = this.store.select(selectProductsLoading);
    this.error$ = this.store.select(selectProductsError);
  }

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id')!;
    this.store.dispatch(loadProduct({ id }));
  }

  ngOnDestroy(): void {
    this.store.dispatch(clearSelectedProduct());
  }

  addToCart(product: Product): void {
    const item: CartItem = {
      skuId: product.id,
      productId: product.id,
      productName: product.name,
      quantity: this.quantity,
      unitPrice: product.price
    };
    this.store.dispatch(addToCart({ item }));
  }

  onQuantityChange(event: Event): void {
    const value = parseInt((event.target as HTMLInputElement).value, 10);
    if (value > 0) {
      this.quantity = value;
    }
  }
}
