import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Observable, Subject, catchError, throwError } from 'rxjs';
import { environment } from '../../environments/environment';
import { Product, ProductRequest } from '../models/product.model';
import { Category, CategoryRequest } from '../models/category.model';

export interface ApiErrorMessage {
  status: number;
  title: string;
  message: string;
  timestamp: Date;
}

@Injectable({
  providedIn: 'root'
})
export class InventoryService {
  private http = inject(HttpClient);
  private baseUrl = environment.apiConfig.baseUrl;

  private errorNotificationSubject = new Subject<ApiErrorMessage>();
  public errorNotification$: Observable<ApiErrorMessage> = this.errorNotificationSubject.asObservable();

  // --- PRODUCTOS ---

  public getProducts(categoryId?: number | null, search?: string | null): Observable<Product[]> {
    let params = new HttpParams();
    if (categoryId) {
      params = params.set('categoryId', categoryId.toString());
    }
    if (search && search.trim().length > 0) {
      params = params.set('search', search.trim());
    }

    return this.http.get<Product[]>(`${this.baseUrl}/products`, { params }).pipe(
      catchError(err => this.handleHttpError(err))
    );
  }

  public getProductById(id: number): Observable<Product> {
    return this.http.get<Product>(`${this.baseUrl}/products/${id}`).pipe(
      catchError(err => this.handleHttpError(err))
    );
  }

  public createProduct(product: ProductRequest): Observable<Product> {
    return this.http.post<Product>(`${this.baseUrl}/products`, product).pipe(
      catchError(err => this.handleHttpError(err))
    );
  }

  public updateProduct(id: number, product: ProductRequest): Observable<Product> {
    return this.http.put<Product>(`${this.baseUrl}/products/${id}`, product).pipe(
      catchError(err => this.handleHttpError(err))
    );
  }

  public deleteProduct(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/products/${id}`).pipe(
      catchError(err => this.handleHttpError(err))
    );
  }

  // --- CATEGORÍAS ---

  public getCategories(): Observable<Category[]> {
    return this.http.get<Category[]>(`${this.baseUrl}/categories`).pipe(
      catchError(err => this.handleHttpError(err))
    );
  }

  public createCategory(category: CategoryRequest): Observable<Category> {
    return this.http.post<Category>(`${this.baseUrl}/categories`, category).pipe(
      catchError(err => this.handleHttpError(err))
    );
  }

  // --- GESTIÓN DE ERRORES AMIGABLES (401, 403, 404, 500) ---

  private handleHttpError(error: HttpErrorResponse): Observable<never> {
    let userTitle = 'Error en el Sistema';
    let userMessage = 'Ocurrió un error inesperado al procesar la solicitud.';

    if (error.status === 401) {
      userTitle = 'Sesión No Autorizada (401)';
      userMessage = 'Su token de autenticación no fue reconocido o ha expirado. Por favor, vuelva a iniciar sesión con su cuenta Microsoft Entra ID.';
    } else if (error.status === 403) {
      userTitle = 'Permisos Insuficientes (403 Acceso Denegado)';
      userMessage = 'Esta operación requiere privilegios de Administrador (Rol "Admin" en Microsoft Entra ID). Los usuarios con permisos de vendedor u operador únicamente disponen de acceso de consulta.';
    } else if (error.status === 404) {
      userTitle = 'Elemento No Encontrado (404)';
      userMessage = error.error?.message || 'El producto o categoría solicitado no fue encontrado en la base de datos.';
    } else if (error.status === 400) {
      userTitle = 'Datos Inválidos (400)';
      userMessage = error.error?.message || 'Por favor revise los datos del formulario.';
    } else if (error.status === 0) {
      userTitle = 'Servidor Inaccesible';
      userMessage = 'No fue posible conectar con el Resource Server en http://localhost:8080. Verifique que el backend esté en ejecución.';
    }

    this.errorNotificationSubject.next({
      status: error.status,
      title: userTitle,
      message: userMessage,
      timestamp: new Date()
    });

    return throwError(() => error);
  }
}
