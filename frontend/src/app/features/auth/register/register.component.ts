import { Component } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { Observable } from 'rxjs';
import { register } from '../../../store/auth/auth.actions';
import { selectAuthLoading, selectAuthError, selectRegisterSuccess } from '../../../store/auth/auth.selectors';

@Component({
  selector: 'app-register',
  templateUrl: './register.component.html'
})
export class RegisterComponent {
  form: FormGroup;
  loading$: Observable<boolean>;
  error$: Observable<string | null>;
  success$: Observable<boolean>;

  constructor(private fb: FormBuilder, private store: Store) {
    this.form = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      username: ['', [Validators.required, Validators.minLength(3)]],
      password: ['', [Validators.required, Validators.minLength(8)]]
    });
    this.loading$ = this.store.select(selectAuthLoading);
    this.error$ = this.store.select(selectAuthError);
    this.success$ = this.store.select(selectRegisterSuccess);
  }

  submit(): void {
    if (this.form.valid) {
      const { email, username, password } = this.form.value;
      this.store.dispatch(register({ email, username, password }));
    }
  }
}
