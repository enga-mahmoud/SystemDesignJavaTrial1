import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';
import { AdminProductListComponent } from './admin-product-list/admin-product-list.component';
import { ProductFormComponent } from './product-form/product-form.component';
import { ServicesDashboardComponent } from './services-dashboard/services-dashboard.component';

const routes: Routes = [
  { path: '', component: AdminProductListComponent },
  { path: 'products/new', component: ProductFormComponent },
  { path: 'products/:id/edit', component: ProductFormComponent },
  { path: 'services', component: ServicesDashboardComponent }
];

@NgModule({
  declarations: [AdminProductListComponent, ProductFormComponent, ServicesDashboardComponent],
  imports: [SharedModule, RouterModule.forChild(routes)]
})
export class AdminModule {}
