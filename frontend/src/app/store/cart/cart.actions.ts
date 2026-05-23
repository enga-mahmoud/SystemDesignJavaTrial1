import { createAction, props } from '@ngrx/store';
import { CartItem } from '../../shared/models/cart.model';

export const addToCart = createAction('[Cart] Add to Cart', props<{ item: CartItem }>());

export const removeFromCart = createAction('[Cart] Remove from Cart', props<{ skuId: string }>());

export const updateCartItem = createAction(
  '[Cart] Update Cart Item',
  props<{ skuId: string; quantity: number }>()
);

export const clearCart = createAction('[Cart] Clear Cart');

export const placeOrder = createAction('[Cart] Place Order');

export const placeOrderSuccess = createAction(
  '[Cart] Place Order Success',
  props<{ orderId: string }>()
);

export const placeOrderFailure = createAction(
  '[Cart] Place Order Failure',
  props<{ error: string }>()
);
