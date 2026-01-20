# API Test Plan (Postman)

This guide explains how to test KuberFashion backend endpoints using Postman. It covers public, authenticated (user), and admin endpoints, along with file uploads to Cloudflare R2.

## 1) Setup

- Create a Postman Environment with these variables:
  - `baseUrl` (e.g. `http://localhost:8080/api` for local or `https://api.kuberfashions.in/api` for prod)
  - `adminEmail` (admin user email)
  - `adminPassword` (admin password)
  - `userEmail` (normal user email)
  - `userPassword` (normal user password)
  - `token` (will be set after login)

- In Postman, set Authorization type to "Bearer Token" and use `{{token}}` for requests that need auth.

## 2) Health

- GET `{{baseUrl}}/health`
  - Expect 200 with `{ status: "UP" }`

## 3) Auth

- POST `{{baseUrl}}/auth/register`
  - Body (JSON):
```json
{
  "firstName": "John",
  "lastName": "Doe",
  "email": "{{userEmail}}",
  "phone": "+910000000001",
  "password": "Passw0rd!",
  "confirmPassword": "Passw0rd!"
}
```
  - Expect 201 with `{ success: true, data: { token, user } }`

- POST `{{baseUrl}}/auth/login`
  - Body (JSON):
```json
{
  "email": "{{userEmail}}",
  "password": "{{userPassword}}"
}
```
  - Copy `data.token` into `token` environment variable.

- GET `{{baseUrl}}/auth/me`
  - Bearer `{{token}}`
  - Expect 200 with current user info.

## 4) Categories (Public)

- GET `{{baseUrl}}/categories`
  - Expect 200 with array of categories.

## 5) Products (Public)

- GET `{{baseUrl}}/products`
- GET `{{baseUrl}}/products/featured`
- GET `{{baseUrl}}/products/top-rated`
- GET `{{baseUrl}}/products/newest`
- GET `{{baseUrl}}/products/slug/{slug}` (replace `{slug}` with a real one)
- GET `{{baseUrl}}/products/category/{categorySlug}`
- GET `{{baseUrl}}/products/search?q=shirt`

All should return 200 with lists or an item.

## 6) Cart (User auth required)

- GET `{{baseUrl}}/cart`
  - Expect 200 with items array (may be empty)

- POST `{{baseUrl}}/cart/add`
  - Body (JSON):
```json
{
  "productId": 1,
  "quantity": 1,
  "size": null,
  "color": null
}
```
  - Expect 200 with the created/updated cart item DTO

- PUT `{{baseUrl}}/cart/items/{itemId}`
  - Body (JSON): `{ "quantity": 2 }`
  - Expect 200

- DELETE `{{baseUrl}}/cart/items/{itemId}`
  - Expect 200

- DELETE `{{baseUrl}}/cart`
  - Expect 200

## 7) Wishlist (User auth required)

- GET `{{baseUrl}}/wishlist`
- POST `{{baseUrl}}/wishlist/add`
  - Body: `{ "productId": 1 }`
- DELETE `{{baseUrl}}/wishlist/remove/{productId}`
- DELETE `{{baseUrl}}/wishlist/clear`

All should return 200.

## 8) Orders (User auth required)

- POST `{{baseUrl}}/orders/create`
  - Body (JSON):
```json
{
  "cartItems": [
    { "id": 1, "productId": 1, "quantity": 1, "price": 29.99 }
  ],
  "shippingAddress": "123 Street, City",
  "billingAddress": "123 Street, City",
  "paymentMethod": "COD"
}
```
  - Expect 200 with order

- GET `{{baseUrl}}/orders/my-orders`
- GET `{{baseUrl}}/orders/{orderId}`
- PUT `{{baseUrl}}/orders/{orderId}/cancel`

## 9) Admin Auth

- POST `{{baseUrl}}/admin/auth/login`
  - Body:
```json
{
  "email": "{{adminEmail}}",
  "password": "{{adminPassword}}"
}
```
  - Set `token` to the returned admin token.

## 10) Admin – Users

- GET `{{baseUrl}}/admin/users?page=0&size=10`
- PUT `{{baseUrl}}/admin/users/{id}/status`
  - Body: `{ "enabled": true }`
- PUT `{{baseUrl}}/admin/users/{id}/role`
  - Body: `{ "role": "ADMIN" }`

## 11) Admin – Products

- POST `{{baseUrl}}/admin/products`
- PUT `{{baseUrl}}/admin/products/{id}`
- PUT `{{baseUrl}}/admin/products/{id}/status`
  - Body: `{ "active": true }` or `{ "featured": true }`
- PUT `{{baseUrl}}/admin/products/{id}/stock`
  - Body: `{ "stockQuantity": 100 }`
- DELETE `{{baseUrl}}/admin/products/{id}`

## 12) Admin – Orders

- GET `{{baseUrl}}/admin/orders?page=0&size=10`
- PUT `{{baseUrl}}/admin/{orderId}/status`
  - Body: `{ "status": "SHIPPED" }`
- PUT `{{baseUrl}}/admin/{orderId}/payment-status`
  - Body: `{ "paymentStatus": "PAID" }`

## 13) File Uploads (Cloudflare R2)

- POST `{{baseUrl}}/admin/storage/upload`
  - Form-Data:
    - `file` (type: File)
    - `categorySlug` = `products` (or any folder)
    - `filename` (optional – backend will generate if not provided)
  - Expect 200 with `{ publicUrl }`

- POST `{{baseUrl}}/files/upload/product`
  - Form-Data: `file` (File)
  - Expect 200 with `{ data: { url } }`

- DELETE `{{baseUrl}}/files/delete?url={{fileUrl}}`

## Notes

- Always include `Authorization: Bearer {{token}}` for protected endpoints.
- For production, ensure `{{baseUrl}}` is `https://api.kuberfashions.in/api`.
- CORS is enabled for `https://kuberfashions.in` and `https://www.kuberfashions.in`.
