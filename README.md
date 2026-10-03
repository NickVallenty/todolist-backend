# TodoList Backend API

Backend REST API cho ứng dụng Web TodoList, xây dựng bằng **Spring Boot 3 + Spring Security (JWT) + Spring Data JPA + SQL Server**, theo đúng scope đã mô tả:

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
│   ├── entity/           AppUser, Todo  (map đúng theo bảng Users/Todos đã thiết kế)
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
- SQL Server 2019 (đang chạy sẵn theo bạn xác nhận)

## Cài đặt & chạy

### 1. Tạo database

Trong SSMS, tạo database rỗng:

```sql
CREATE DATABASE TodoListDB;
```

Bạn **không cần** tự tạo bảng `Users`/`Todos` — Hibernate sẽ tự tạo theo đúng Entity nhờ cấu hình `ddl-auto: update` (đã map đúng tên cột như 2 ảnh schema bạn gửi: `UserId`, `Username`, `Email`, `Password`, `Role`, `Status`, `CreatedDate` / `TodoId`, `UserId`, `Title`, `Description`, `Status`, `CreatedDate`, `UpdatedDate`).

Nếu bạn đã có sẵn 2 bảng này rồi thì cứ để nguyên, Hibernate sẽ chỉ đồng bộ thêm nếu thiếu cột/index, không xóa dữ liệu.

### 2. Cấu hình kết nối

Sửa `src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:sqlserver://localhost:1433;databaseName=TodoListDB;encrypt=true;trustServerCertificate=true
    username: sa
    password: <mật khẩu SQL Server của bạn>
```

### 3. Import vào IntelliJ IDEA

- Open project → chọn thư mục `todolist-backend` (IntelliJ tự nhận là Maven project và tải dependency).
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
| POST | `/api/auth/login` | Đăng nhập, trả về JWT |

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

## Bảo mật đã implement theo đúng yêu cầu phi chức năng (4.2)

- Password mã hóa một chiều bằng **BCrypt** trước khi lưu DB — không bao giờ trả Password trong bất kỳ response nào (`UserResponse` không có field password).
- Phân quyền theo Role ở tầng filter chain: `/api/admin/**` chỉ ADMIN, `/api/todos/**` chỉ USER.
- Mọi thao tác Todo đều kiểm tra `Todo.UserId == JWT.userId`; nếu không khớp trả **404** (thay vì 403) để không lộ việc Todo đó có tồn tại hay không.
- Foreign key `Todos.UserId -> Users.UserId` được khai báo tường minh ở entity.
- Index cho `Username`, `Email` (bảng Users) và `UserId` (bảng Todos) để tăng tốc tìm kiếm.
- Response lỗi thống nhất dạng JSON (400/401/403/404/409/500) qua `GlobalExceptionHandler`.

## Việc bạn có thể cần làm thêm sau này (ngoài phạm vi hiện tại, tham khảo mục 2.4/2.5 trong tài liệu)

- Refresh token (hiện tại token hết hạn sau 24h phải login lại — có thể chỉnh `app.jwt.expiration-ms`).
- Đổi `ddl-auto: update` → `validate` + script migration (Flyway/Liquibase) khi lên production.
- Viết test (unit/integration) cho service & controller nếu bạn cần độ tin cậy cao hơn trước khi release.
