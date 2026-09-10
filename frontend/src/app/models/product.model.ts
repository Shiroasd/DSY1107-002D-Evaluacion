export interface Product {
  id: number;
  sku: string;
  name: string;
  description?: string;
  price: number;
  stock: number;
  categoryId: number;
  categoryName: string;
}

export interface ProductRequest {
  sku: string;
  name: string;
  description?: string;
  price: number;
  stock: number;
  categoryId: number;
}
