import { Component } from '@angular/core';
import { Store } from '@ngrx/store';
import { Observable } from 'rxjs';
import { CartItem } from '../../../shared/models/cart.model';
import {
  removeFromCart,
  updateCartItem,
  placeOrder,
  placeOrderFailure
} from '../../../store/cart/cart.actions';
import {
  selectCartItems,
  selectCartTotal,
  selectCartOrdering,
  selectCartError
} from '../../../store/cart/cart.selectors';
import { selectIsLoggedIn } from '../../../store/auth/auth.selectors';

@Component({
  selector: 'app-cart',
  templateUrl: './cart.component.html'
})
export class CartComponent {
  items$: Observable<CartItem[]>;
  total$: Observable<number>;
  ordering$: Observable<boolean>;
  error$: Observable<string | null>;
  isLoggedIn$: Observable<boolean>;

  constructor(private store: Store) {
    this.items$ = this.store.select(selectCartItems);
    this.total$ = this.store.select(selectCartTotal);
    this.ordering$ = this.store.select(selectCartOrdering);
    this.error$ = this.store.select(selectCartError);
    this.isLoggedIn$ = this.store.select(selectIsLoggedIn);
  }

  remove(skuId: string): void {
    this.store.dispatch(removeFromCart({ skuId }));
  }

  onQuantityChange(skuId: string, event: Event): void {
    const qty = parseInt((event.target as HTMLInputElement).value, 10);
    if (qty > 0) {
      this.store.dispatch(updateCartItem({ skuId, quantity: qty }));
    } else {
      this.store.dispatch(removeFromCart({ skuId }));
    }
  }

  checkout(): void {
    this.store.dispatch(placeOrder());
  }
}
