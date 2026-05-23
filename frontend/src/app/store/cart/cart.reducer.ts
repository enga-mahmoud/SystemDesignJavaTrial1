import { createReducer, on } from '@ngrx/store';
import { ActionReducer } from '@ngrx/store';
import { CartItem } from '../../shared/models/cart.model';
import { addToCart, removeFromCart, updateCartItem, clearCart, placeOrder, placeOrderSuccess, placeOrderFailure } from './cart.actions';

export interface CartState {
  items: CartItem[];
  total: number;
  ordering: boolean;
  error: string | null;
}

const initialCartState: CartState = {
  items: [],
  total: 0,
  ordering: false,
  error: null
};

function calculateTotal(items: CartItem[]): number {
  return items.reduce((sum, item) => sum + item.unitPrice * item.quantity, 0);
}

export const cartReducer = createReducer(
  initialCartState,

  on(addToCart, (state, { item }) => {
    const existing = state.items.find(i => i.skuId === item.skuId);
    const items = existing
      ? state.items.map(i => i.skuId === item.skuId ? { ...i, quantity: i.quantity + item.quantity } : i)
      : [...state.items, item];
    return { ...state, items, total: calculateTotal(items) };
  }),

  on(removeFromCart, (state, { skuId }) => {
    const items = state.items.filter(i => i.skuId !== skuId);
    return { ...state, items, total: calculateTotal(items) };
  }),

  on(updateCartItem, (state, { skuId, quantity }) => {
    const items = state.items.map(i => i.skuId === skuId ? { ...i, quantity } : i);
    return { ...state, items, total: calculateTotal(items) };
  }),

  on(clearCart, () => initialCartState),

  on(placeOrder, state => ({ ...state, ordering: true, error: null })),
  on(placeOrderSuccess, state => ({ ...state, ordering: false })),
  on(placeOrderFailure, (state, { error }) => ({ ...state, ordering: false, error }))
);

export function localStorageCartMetaReducer(reducer: ActionReducer<any>): ActionReducer<any> {
  return (state, action) => {
    if (action.type === '@ngrx/store/init') {
      try {
        const stored = localStorage.getItem('cart');
        if (stored) {
          state = { ...state, cart: JSON.parse(stored) };
        }
      } catch {}
    }
    const nextState = reducer(state, action);
    try {
      localStorage.setItem('cart', JSON.stringify(nextState.cart));
    } catch {}
    return nextState;
  };
}
