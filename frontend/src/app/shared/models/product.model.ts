export interface Product {
  id: string;
  name: string;
  description: string;
  price: number;
  categoryId: string;
  categoryName?: string;
  vendorId: string;
  status: string;
  createdAt?: string;
}
