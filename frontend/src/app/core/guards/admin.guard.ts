import { Injectable } from '@angular/core';
import { CanActivate, Router } from '@angular/router';
import { TokenStorageService } from '../services/token-storage.service';

@Injectable({ providedIn: 'root' })
export class AdminGuard implements CanActivate {
  constructor(private tokenStorage: TokenStorageService, private router: Router) {}

  canActivate(): boolean {
    const user = this.tokenStorage.getUser();
    if (user?.role === 'ADMIN') return true;
    this.router.navigate(['/products']);
    return false;
  }
}
