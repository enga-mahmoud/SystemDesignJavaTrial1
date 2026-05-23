import { Component, OnInit } from '@angular/core';
import { Store } from '@ngrx/store';
import { Observable } from 'rxjs';
import { TokenStorageService } from './core/services/token-storage.service';
import { loginSuccess, logout } from './store/auth/auth.actions';
import { selectIsLoggedIn, selectCurrentUser, selectIsAdmin } from './store/auth/auth.selectors';
import { selectCartItemCount } from './store/cart/cart.selectors';
import { User } from './shared/models/user.model';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html'
})
export class AppComponent implements OnInit {
  isLoggedIn$: Observable<boolean>;
  isAdmin$: Observable<boolean>;
  currentUser$: Observable<User | null>;
  cartCount$: Observable<number>;

  constructor(private store: Store, private tokenStorage: TokenStorageService) {
    this.isLoggedIn$ = this.store.select(selectIsLoggedIn);
    this.isAdmin$    = this.store.select(selectIsAdmin);
    this.currentUser$ = this.store.select(selectCurrentUser);
    this.cartCount$ = this.store.select(selectCartItemCount);
  }

  ngOnInit(): void {
    const token = this.tokenStorage.getAccessToken();
    const user = this.tokenStorage.getUser();
    if (token && user) {
      this.store.dispatch(loginSuccess({
        tokens: { accessToken: token, refreshToken: this.tokenStorage.getRefreshToken() ?? '' },
        user
      }));
    }
  }

  logout(): void {
    this.store.dispatch(logout());
  }
}
