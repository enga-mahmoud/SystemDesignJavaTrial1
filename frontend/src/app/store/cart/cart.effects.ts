import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { Store } from '@ngrx/store';
import { of } from 'rxjs';
import { catchError, map, switchMap, tap, withLatestFrom } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { placeOrder, placeOrderSuccess, placeOrderFailure, clearCart } from './cart.actions';
import { selectCartItems } from './cart.selectors';

@Injectable()
export class CartEffects {
  placeOrder$ = createEffect(() =>
    this.actions$.pipe(
      ofType(placeOrder),
      withLatestFrom(this.store.select(selectCartItems)),
      switchMap(([, items]) => {
        const orderItems = items.map(item => ({
          skuId: item.skuId,
          productId: item.productId,
          quantity: item.quantity,
          unitPrice: item.unitPrice
        }));
        return this.http.post<{ orderId: string; status: string }>(
          `${environment.apiUrl}/api/orders`,
          { items: orderItems }
        ).pipe(
          map(res => placeOrderSuccess({ orderId: res.orderId })),
          catchError(err => of(placeOrderFailure({
            error: err?.error?.message ?? 'Order placement failed'
          })))
        );
      })
    )
  );

  placeOrderSuccess$ = createEffect(
    () =>
      this.actions$.pipe(
        ofType(placeOrderSuccess),
        tap(() => {
          this.store.dispatch(clearCart());
          this.router.navigate(['/orders']);
        })
      ),
    { dispatch: false }
  );

  constructor(
    private actions$: Actions,
    private http: HttpClient,
    private store: Store,
    private router: Router
  ) {}
}
