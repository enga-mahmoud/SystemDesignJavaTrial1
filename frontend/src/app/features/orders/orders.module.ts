import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';
import { OrderHistoryComponent } from './order-history/order-history.component';

const routes: Routes = [
  { path: '', component: OrderHistoryComponent }
];

@NgModule({
  declarations: [OrderHistoryComponent],
  imports: [SharedModule, RouterModule.forChild(routes)]
})
export class OrdersModule {}
