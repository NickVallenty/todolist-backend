# TodoList Backend API

Backend REST API cho ứng dụng Web TodoList, xây dựng bằng **Spring Boot 3 + Spring Security (JWT) + Spring Data JPA + MySQL**, theo đúng scope đã mô tả:

- **Admin**: CRUD User (tạo/xem/sửa/xóa/tìm kiếm/lọc), không đụng vào Todo của User.
- **User**: CRUD Todo của chính mình, không xem/sửa được Todo của User khác.
- RBAC 2 role: `ADMIN`, `USER`. Xác thực bằng JWT (stateless).

## Vì sao chọn JWT thay vì Session?

- API thuần REST, stateless — không cần server lưu session, dễ scale ngang (nhiều instance) mà không cần sticky session hay session store dùng chung (Redis...).
- Tách rời hoàn toàn frontend/backend — sau này bạn có thể dùng bất kỳ frontend nào (React, Vue, mobile app...) mà không phụ thuộc cookie của trình duyệt.
- Phù hợp quy mô nhỏ/vừa như project này; nếu sau này cần "thu hồi token ngay lập tức" (revoke) thì có thể bổ sung một blacklist token trong Redis, nhưng với scope hiện tại là không bắt buộc.

## Cấu trúc project

```
todolist-backend/
├── pom.xml
├── src/main/java/com/todolist/
│   ├── TodolistBackendApplication.java
│   ├── config/          SecurityConfig, DataInitializer (seed admin mặc định)
│   ├── security/         JwtUtil, JwtAuthenticationFilter, CustomUserDetails(Service)
│   ├── entity/           AppUser, Todo  (map đúng bảng `users`/`todos`, cột snake_case)
│   ├── enums/            Role, UserStatus, TodoStatus
│   ├── repository/       AppUserRepository, TodoRepository + Specifications (search/filter)
│   ├── dto/request|response/
│   ├── service/          AuthService, AppUserService, TodoService
│   ├── controller/       AuthController, UserController (Admin), TodoController (User)
│   └── exception/        GlobalExceptionHandler + custom exceptions
└── src/main/resources/application.yml
```

## Yêu cầu môi trường

- JDK 21+ (project build với Java 21, tương thích ngược với JDK mới hơn như bạn đang dùng)
- Maven (dùng luôn Maven Wrapper tích hợp sẵn trong IntelliJ, hoặc `mvn` nếu đã cài)
- MySQL 8.x (đang chạy sẵn trên máy, port 3306)

## Cài đặt & chạy

### 1. Tạo database

Chạy script `database/schema.sql` (tạo database `todolist_db` + bảng `users`, `todos`):

```bash
mysql -u root -p < database/schema.sql
```

Hoặc chạy trực tiếp trong IntelliJ (xem mục 3).

**Quy tắc đặt tên**: tên bảng chữ thường, số nhiều (`users`, `todos`); tên cột `snake_case`.

| Entity | Bảng | Cột |
|---|---|---|
| `AppUser` | `users` | `user_id`, `username`, `email`, `password`, `role`, `status`, `created_date` |
| `Todo` | `todos` | `todo_id`, `user_id`, `title`, `description`, `status`, `created_date`, `updated_date` |

`ddl-auto` đang để `validate`: Hibernate không tự tạo bảng, chỉ kiểm tra entity có khớp DB không — nếu sai tên bảng/cột, ứng dụng báo lỗi ngay lúc khởi động.

### 2. Cấu hình kết nối

Sửa `src/main/resources/application.yml` (hoặc đặt biến môi trường `DB_USERNAME`, `DB_PASSWORD`):

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/todolist_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh&characterEncoding=UTF-8
    username: root
    password: <mật khẩu MySQL của bạn>
```

### 3. Import vào IntelliJ IDEA và kết nối MySQL

- Open project → chọn thư mục `todolist-backend` (IntelliJ tự nhận là Maven project và tải dependency).
- Kết nối MySQL (cần IntelliJ IDEA **Ultimate**; bản Community không có Database tool): `View → Tool Windows → Database` → `+` → `Data Source` → `MySQL`.
  - Host `localhost`, Port `3306`, User `root`, Password của bạn; ô Database có thể để trống ở bước đầu.
  - Bấm **Download missing driver files** nếu IntelliJ báo thiếu driver → **Test Connection** → OK.
- Tạo database: chuột phải vào data source → `New → Query Console`, mở `database/schema.sql`, chọn hết rồi chạy (Ctrl+Enter). Sau đó bấm Refresh, tab `Schemas` tick chọn `todolist_db`.
- Chạy class `TodolistBackendApplication`.

Hoặc chạy bằng terminal (nếu có Maven):

```bash
mvn spring-boot:run
```

Server chạy ở `http://localhost:8080`.

### 4. Tài khoản Admin mặc định

Lần đầu khởi động, nếu chưa có Admin nào trong DB, hệ thống tự tạo:

- **username**: `admin`
- **password**: `Admin@123`

(cấu hình được trong `application.yml` mục `app.admin.*`) — nhớ đổi password sau khi đăng nhập lần đầu qua API reset-password.

## API Endpoints

### Auth (public)

| Method | Endpoint | Mô tả |
|---|---|---|
| POST | `/api/auth/register` | Đăng ký tài khoản mới (luôn là Role=USER, Status=ACTIVE) |
| POST | `/api/auth/login` | Đăng nhập, trả về JWT |

```json
POST /api/auth/register
{ "username": "nguyenvana", "email": "a@gmail.com", "password": "123456", "confirmPassword": "123456" }

-> 201 Created
{ "userId": 2, "username": "nguyenvana", "email": "a@gmail.com", "role": "USER", "status": "ACTIVE", "createdDate": "2026-10-06T10:00:00" }
```

Quy tắc đăng ký: username 3–50 ký tự (chữ, số, `.`, `_`, `-`); email hợp lệ; password tối thiểu 6 ký tự và phải khớp `confirmPassword`.
Lỗi: `400` dữ liệu không hợp lệ, `409` username/email đã tồn tại. Client **không thể** tự chọn role — request không có field `role`; tài khoản ADMIN chỉ do Admin tạo qua `/api/admin/users`.

```json
POST /api/auth/login
{ "username": "admin", "password": "Admin@123" }

-> 200 OK
{ "token": "eyJhbGciOi...", "tokenType": "Bearer", "userId": 1, "username": "admin", "role": "ADMIN" }
```

Các API bên dưới đều cần header: `Authorization: Bearer <token>`

### Admin quản lý User — `/api/admin/users` (chỉ Role=ADMIN)

| Method | Endpoint | Mô tả |
|---|---|---|
| POST | `/api/admin/users` | Tạo User mới |
| GET | `/api/admin/users?keyword=&role=&status=&page=0&size=10` | Danh sách User (search + filter + phân trang) |
| GET | `/api/admin/users/{id}` | Chi tiết 1 User |
| PUT | `/api/admin/users/{id}` | Cập nhật username/email/role/status |
| PUT | `/api/admin/users/{id}/reset-password` | Đổi/reset password cho User |
| DELETE | `/api/admin/users/{id}` | Xóa User (cascade xóa luôn Todo của User đó) |

### User quản lý Todo — `/api/todos` (chỉ Role=USER, luôn chỉ thao tác Todo của chính mình)

| Method | Endpoint | Mô tả |
|---|---|---|
| POST | `/api/todos` | Tạo Todo mới |
| GET | `/api/todos?keyword=&status=&page=0&size=10&sort=createdDate,desc` | Danh sách Todo (search theo title, filter theo status, sort) |
| GET | `/api/todos/{id}` | Chi tiết 1 Todo |
| PUT | `/api/todos/{id}` | Cập nhật title/description/status |
| DELETE | `/api/todos/{id}` | Xóa Todo |

> Lưu ý: `userId` của Todo **luôn** được lấy từ JWT của người đang đăng nhập, không bao giờ đọc từ body/param — đảm bảo user A không thể thao tác Todo của user B kể cả khi cố tình truyền id.

## Đổi mật khẩu bằng OTP — `/api/account/change-password` (cần đăng nhập, USER hoặc ADMIN)

| Method | Endpoint | Mô tả |
|---|---|---|
| POST | `/api/account/change-password/request-otp` | Không body. Gửi OTP 6 số tới email của tài khoản đang đăng nhập |
| POST | `/api/account/change-password/confirm` | Body `otp`, `newPassword`, `confirmPassword`. Đúng OTP thì đổi mật khẩu (204) |

```json
POST /api/account/change-password/request-otp      (Authorization: Bearer <token>)
-> 200 OK
{ "message": "Đã gửi OTP tới email của bạn", "maskedEmail": "n***@gmail.com", "expiresInSeconds": 300 }

POST /api/account/change-password/confirm          (Authorization: Bearer <token>)
{ "otp": "123456", "newPassword": "NewPass@123", "confirmPassword": "NewPass@123" }
-> 204 No Content
```

Lỗi: `400` OTP sai/hết hạn/sai quá số lần/dữ liệu không hợp lệ/password mới trùng password cũ, `401` thiếu hoặc sai token, `429` xin OTP quá nhanh (kèm header `Retry-After`), `503` không gửi được email.

Quy tắc: OTP hết hạn sau 5 phút, dùng một lần, tối đa 5 lần nhập sai, cách nhau tối thiểu 60 giây giữa 2 lần xin OTP. OTP chỉ được lưu dạng hash BCrypt.

**Cài đặt để chạy được chức năng này**

1. Tạo bảng mới (không ảnh hưởng `users`, `todos`; chạy 1 lần, vì `ddl-auto: validate` sẽ báo lỗi nếu thiếu bảng):
   ```bash
   mysql -u root -p < database/password_otps.sql
   ```
2. Cấu hình SMTP bằng biến môi trường (không ghi credential vào source). Ví dụ Gmail (dùng App Password):
   ```
   SPRING_MAIL_HOST=smtp.gmail.com
   SPRING_MAIL_PORT=587
   SPRING_MAIL_USERNAME=<email gửi>
   SPRING_MAIL_PASSWORD=<app password>
   SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH=true
   SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE=true
   ```
   Chưa cấu hình SMTP thì ứng dụng vẫn chạy bình thường, chỉ `request-otp` trả `503`.
3. (Tùy chọn) tinh chỉnh bằng `app.otp.expiration-minutes` (5), `app.otp.max-attempts` (5), `app.otp.resend-cooldown-seconds` (60), `app.otp.mail-from`.

**Tài liệu liên quan**: sơ đồ tuần tự trong [`docs/UML/`](docs/UML/README.md); Postman Collection import được tại `docs/postman/TodoList-ChangePassword-OTP.postman_collection.json`.

> Lưu ý: JWT đã cấp trước khi đổi mật khẩu vẫn còn hiệu lực đến khi hết hạn (hệ thống chưa có cơ chế thu hồi token).

## Bảo mật đã implement theo đúng yêu cầu phi chức năng (4.2)

- Password mã hóa một chiều bằng **BCrypt** trước khi lưu DB — không bao giờ trả Password trong bất kỳ response nào (`UserResponse` không có field password).
- Phân quyền theo Role ở tầng filter chain: `/api/admin/**` chỉ ADMIN, `/api/todos/**` chỉ USER.
- Mọi thao tác Todo đều kiểm tra `todos.user_id == JWT.userId`; nếu không khớp trả **404** (thay vì 403) để không lộ việc Todo đó có tồn tại hay không.
- Foreign key `todos.user_id -> users.user_id` (ON DELETE CASCADE) được khai báo ở cả entity lẫn `schema.sql`.
- Index cho `username`, `email` (bảng `users`) và `user_id` (bảng `todos`) để tăng tốc tìm kiếm.
- Response lỗi thống nhất dạng JSON (400/401/403/404/409/500) qua `GlobalExceptionHandler`.

## Việc bạn có thể cần làm thêm sau này (ngoài phạm vi hiện tại, tham khảo mục 2.4/2.5 trong tài liệu)

- Refresh token (hiện tại token hết hạn sau 24h phải login lại — có thể chỉnh `app.jwt.expiration-ms`).
- Dùng Flyway/Liquibase để quản lý migration thay cho chạy tay `schema.sql` khi lên production.
- Viết test (unit/integration) cho service & controller nếu bạn cần độ tin cậy cao hơn trước khi release.
