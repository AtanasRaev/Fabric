# 🧵 Fabric — E-Commerce Clothing Platform

![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.1-green)
![PostgreSQL](https://img.shields.io/badge/Database-PostgreSQL-blue)
![License](https://img.shields.io/badge/License-All%20Rights%20Reserved-red)

**Fabric** is a modern e-commerce platform for clothing products built with **Spring Boot**.  
It provides a complete backend solution for managing an online clothing store — including product management, user authentication, shopping cart functionality, order processing, and shipping service integration.

---

## 🧰 Technologies Used

- **Java 21**
- **Spring Boot 3.4.1**
- **Spring Security** – Authentication & authorization  
- **Spring Data JPA** – Database access layer  
- **PostgreSQL** – Database  
- **JWT** – Secure API authentication  
- **Cloudinary** – Image storage  
- **Mailjet** – Email notifications  
- **Bucket4j** – API rate limiting  
- **Caffeine** – Caching  
- **ModelMapper** – DTO mapping  
- **WebP** – Optimized image handling  

---

## 🎥 Demo

A live version is available here:  
👉 [https://fabric-bg.com/](https://fabric-bg.com/)

> *Includes both backend and frontend integration.*

---

## 🏗️ Architecture Overview

- **Backend:** Spring Boot REST API (this repository)  
- **Frontend:** React — [GitHub Repository](https://github.com/emilbankov/Fabric)  
- **Database:** PostgreSQL  
- **Image Hosting:** Cloudinary  
- **Email Service:** Mailjet  
- **Caching & Rate Limiting:** Caffeine & Bucket4j  
- **Deployment:** SuperHosting  

---

## ✨ Features

### 🧦 Product Management
- Add, edit, and delete clothing items  
- Categorize products by type and category  
- Tag products for better searchability  
- Manage product images  
- Set regular and discount prices  

### 👥 User Management
- Registration and authentication  
- Role-based access control (admin/user)  
- Profile management  

### 🛍️ Shopping Experience
- Browse product catalog  
- Search, filter, and sort products  
- View detailed product pages  

### 📦 Order Processing
- Shopping cart functionality  
- Checkout process  
- Order tracking and history  

### 🚚 Shipping Integration
- Integration with **Econt** shipping service  
- Office or address delivery  
- Automatic shipping cost calculation  

### 🛠️ Admin Features
- Product, order, and user management  
- Sales analytics dashboard  

---

## ⚙️ Setup & Installation

### Prerequisites
- Java 21  
- PostgreSQL  
- Gradle  

### Steps
1. **Clone the repository**
   ```bash
   git clone https://github.com/AtanasRaev/fabric.git
   cd fabric
```

2. **Configure the database**
   Update connection details in `application.yaml`.
3. **Run the application**

   ```bash
   ./gradlew bootRun
   ```
4. **Access the API**

   ```
   http://localhost:8080
   ```

---

## 🔗 API Endpoints

### 🔐 Authentication

* `POST /auth/register` – Register a new user
* `POST /auth/login` – Authenticate a user

### 👕 Products

* `GET /clothes/catalog` – Get product catalog
* `GET /clothes/{id}` – Get product details
* `GET /clothes/search` – Search for products
* `GET /clothes/by-tag/{tagName}` – Get products by tag
* `GET /clothes/category` – Get categories by type
* `GET /clothes/categories` – Get all categories

### 🧾 Orders

* `POST /orders/create` – Create a new order
* `GET /orders/{id}` – Get order details
* `GET /orders/user` – Get user’s orders

### 🏤 Shipping (Econt)

* `GET /econt/cities` – Get available cities
* `GET /econt/offices` – Get offices in a city

---

## 🤝 Contributing

Contributions are welcome!
Please open an issue or submit a pull request if you’d like to improve the project.

---

## 👤 Author

**Atanas Petrov Raev**  
📍 Plovdiv, Bulgaria  
📧 [atanaspraev@gmail.com](mailto:atanaspraev@gmail.com)  
🔗 [GitHub Profile](https://github.com/AtanasRaev)

---

## ⚖️ License

© 2025 Atanas Petrov Raev. All rights reserved.
This project is part of my personal portfolio.
The source code is shared for learning and demonstration purposes only and may not be used for commercial purposes without permission.

