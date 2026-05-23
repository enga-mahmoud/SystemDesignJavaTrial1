import { Component, OnInit, OnDestroy } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { Store } from '@ngrx/store';
import { Observable, Subject } from 'rxjs';
import { filter, takeUntil } from 'rxjs/operators';
import {
  createProduct, updateProduct, clearSelectedProduct, loadProduct
} from '../../../store/products/products.actions';
import {
  selectSelectedProduct, selectSubmitting, selectProductsError
} from '../../../store/products/products.selectors';

@Component({
  selector: 'app-product-form',
  templateUrl: './product-form.component.html'
})
export class ProductFormComponent implements OnInit, OnDestroy {
  form: FormGroup;
  isEdit = false;
  productId: string | null = null;
  submitting$: Observable<boolean>;
  error$: Observable<string | null>;
  private destroy$ = new Subject<void>();

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private store: Store
  ) {
    this.form = this.fb.group({
      name:        ['', [Validators.required, Validators.maxLength(500)]],
      description: [''],
      price:       [null, [Validators.required, Validators.min(0.01)]],
      categoryId:  ['']
    });
    this.submitting$ = this.store.select(selectSubmitting);
    this.error$      = this.store.select(selectProductsError);
  }

  ngOnInit(): void {
    this.productId = this.route.snapshot.paramMap.get('id');
    this.isEdit = !!this.productId;

    if (this.isEdit && this.productId) {
      this.store.dispatch(loadProduct({ id: this.productId }));
      this.store.select(selectSelectedProduct).pipe(
        filter(p => !!p),
        takeUntil(this.destroy$)
      ).subscribe(product => {
        this.form.patchValue({
          name:        product!.name,
          description: product!.description ?? '',
          price:       product!.price,
          categoryId:  product!.categoryId ?? ''
        });
      });
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.store.dispatch(clearSelectedProduct());
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const value = this.form.value;
    const payload = {
      name:        value.name,
      description: value.description || '',
      price:       +value.price,
      categoryId:  value.categoryId || undefined
    };
    if (this.isEdit && this.productId) {
      this.store.dispatch(updateProduct({ id: this.productId, product: payload }));
    } else {
      this.store.dispatch(createProduct({ product: payload }));
    }
  }
}
