## 🚀 Introduction

Online shopping apps have become the backbone of modern commerce. To explore how these platforms work behind the scenes, I decided to build a full-stack **e-commerce application** using **Spring Boot** for the backend and **React JS** for the frontend.

This project gave me hands-on experience with REST APIs, role-based user access, cart functionality, and payment flow — all of which mirror real-world development in retail applications.

## 🎯 Why I Built This

I wanted to challenge myself with a project that includes everything from **user authentication** and **product management** to **order processing** and **payment integration**.

---

E-commerce systems require:

- Dynamic UI for customers and admins
- Strong backend validation
- Secure payment handling  
- This made it the perfect playground to deepen my understanding of full-stack development.

## 🧰 Tech Stack Overview

**Frontend:** React, Axios, Tailwind CSS
**Backend:** Java, Spring Boot, Spring Security, Spring Data JPA
**Database:** MySQL
**Security:** JWT Authentication
**Payments:** Stripe API (optional)
**DevOps:** Git, Postman, Docker (for local setup)

## 🔑 Core Features

- 👤 User registration and login
- 🛍️ Product listing with categories and filters
- 🧺 Cart management with quantity adjustment
- 💳 Checkout with order summary
- 🔐 Admin panel for product CRUD
- 📦 Order placement and history
- 📊 Role-based access (Admin, Customer)

## 🔧 Backend — Spring Boot
The backend manages:
- **User authentication** using Spring Security + JWT
- **CRUD operations** for products and orders
- **REST APIs** for cart and checkout
- **Role-based endpoints** (e.g., only admins can add/delete products)

## 🔒 Sample Endpoint: Secure Product Creation

```java
@PostMapping("/admin/products")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<Product> addProduct(@RequestBody Product product) {
    return ResponseEntity.ok(productService.save(product));
}
```

Entities like `User`, `Product`, `Order`, and `CartItem` were mapped using **JPA**, and relationships were handled with proper `@ManyToOne` / `@OneToMany` annotations.

## 🎨 Frontend — React JS

The frontend is designed to provide a smooth and responsive shopping experience.

## 🔹 Features:

- Product catalog with images, price, and descriptions
- Cart UI with quantity and total price updates
- Conditional rendering for admin tools
- Axios to handle API calls
- React Router for page navigation

## 🧾 Example: Add to Cart

```jsx
const addToCart = (productId) => {
  axios.post("/api/cart", { productId, quantity: 1 }, {
    headers: { Authorization: `Bearer ${token}` },
  }).then(() => alert("Item added to cart"));
};
```

I used **local storage** to keep the JWT and user info, and handled token refresh for persistent login sessions.

## 💥 Challenges Faced

- Handling role-based route protection in both frontend and backend
- Managing session and cart state between page reloads
- Creating responsive UI components with Tailwind
- Stripe integration for mock payments
- Avoiding duplicate orders during network retries
## 📚 What I Learned

✅ Implementing role-based JWT authentication  
✅ Designing RESTful APIs for shopping workflows  
✅ Managing shared state across multiple React components  
✅ Connecting frontend payment forms with Stripe  
✅ Dockerizing backend and frontend for easy deployment

## ✅ Final Thoughts

Building this platform helped me bring together every piece of the full-stack puzzle — frontend interaction, backend validation, secure auth, and a smooth UI.
