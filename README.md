# CloudVault

A production-structured cloud file storage and sharing service built with Java Spring Boot and React.

## Features

- **User Authentication**: Email/password registration and login, plus Google OAuth2 integration
- **File Management**: Upload, download, rename, move, and delete files
- **Folder Organization**: Create nested folders with full navigation
- **Sharing**: Share files and folders with other users (viewer/editor roles)
- **Public Links**: Generate shareable public links with optional password protection and expiry
- **Starred Items**: Mark files and folders for quick access
- **Trash**: Soft-delete with restore capability and permanent deletion
- **Search**: Full-text search across files and folders

## Tech Stack

### Backend
- **Java 17** + **Spring Boot 3.2**
- **Spring Security** with JWT authentication
- **Spring Data JPA** with **PostgreSQL** (via Supabase)
- **Flyway** for database migrations
- **Supabase Storage** for file storage

### Frontend
- **React 18** + **TypeScript**
- **Vite** for build tooling
- **React Router 6** for navigation
- **TanStack Query** for data fetching
- **Tailwind CSS** for styling
- **Zustand** for state management

## Project Structure

```
cloudvault/
├── backend/                    # Spring Boot backend
│   ├── src/main/java/com/cloudvault/
│   │   ├── config/           # Configuration classes
│   │   ├── controller/        # REST API controllers
│   │   ├── dto/              # Data transfer objects
│   │   ├── entity/           # JPA entities
│   │   ├── repository/       # Data repositories
│   │   ├── security/         # JWT and authentication
│   │   └── service/          # Business logic
│   └── src/main/resources/
│       ├── application.yml   # Spring configuration
│       └── db/migration/     # Flyway migrations
├── frontend/                  # React frontend
│   ├── src/
│   │   ├── components/      # React components
│   │   ├── context/         # Zustand stores
│   │   ├── hooks/          # Custom React hooks
│   │   ├── pages/          # Page components
│   │   ├── services/       # API services
│   │   └── types/          # TypeScript types
│   └── ...
└── ...
```

## Environment Setup

### Backend

Copy `.env.example` to `.env` and configure:

```bash
cd backend
cp .env.example .env
```

Required variables:
- `DATABASE_URL` - PostgreSQL connection string (from Supabase)
- `JWT_SECRET` - JWT signing secret (min 256 bits)
- `SUPABASE_URL` - Your Supabase project URL
- `SUPABASE_SERVICE_ROLE_KEY` - Supabase service role key
- `GOOGLE_CLIENT_ID` - Google OAuth client ID
- `GOOGLE_CLIENT_SECRET` - Google OAuth client secret

### Frontend

Copy `.env.example` to `.env`:

```bash
cd frontend
cp .env.example .env
```

Required variables:
- `VITE_SUPABASE_URL` - Supabase project URL
- `VITE_SUPABASE_ANON_KEY` - Supabase anon key
- `VITE_API_URL` - Backend API URL (default: http://localhost:8080/api/v1)

## Running Locally

### Backend

```bash
cd backend
./mvnw spring-boot:run
```

Or with Maven:
```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.config.additional-location=file:.env"
```

### Frontend

```bash
cd frontend
npm install
npm run dev
```

## API Endpoints

### Authentication
- `POST /api/v1/auth/register` - Register new user
- `POST /api/v1/auth/login` - Login user
- `POST /api/v1/auth/refresh` - Refresh access token
- `POST /api/v1/auth/logout` - Logout user
- `GET /api/v1/auth/me` - Get current user

### Files
- `POST /api/v1/files/init-upload` - Initialize upload
- `POST /api/v1/files/{id}/complete-upload` - Complete upload
- `GET /api/v1/files/{id}` - Get file info
- `GET /api/v1/files/{id}/download-url` - Get download URL
- `PATCH /api/v1/files/{id}` - Update file
- `DELETE /api/v1/files/{id}` - Delete file (soft delete)

### Folders
- `POST /api/v1/folders` - Create folder
- `GET /api/v1/folders/{id}` - Get folder with children
- `PATCH /api/v1/folders/{id}` - Update folder
- `DELETE /api/v1/folders/{id}` - Delete folder

### Sharing
- `POST /api/v1/shares` - Share with user
- `GET /api/v1/shares/shared-with-me` - Get shared items
- `DELETE /api/v1/shares/{id}` - Revoke share

### Public Links
- `POST /api/v1/public-links` - Create public link
- `GET /api/v1/public-links/{token}` - Get link info
- `DELETE /api/v1/public-links/{id}` - Revoke link

### Other
- `GET /api/v1/search?q=&type=` - Search files/folders
- `GET /api/v1/stars` - Get starred items
- `POST /api/v1/stars/{type}/{id}` - Star item
- `DELETE /api/v1/stars/{type}/{id}` - Unstar item
- `GET /api/v1/trash` - Get trash items
- `POST /api/v1/trash/{type}/{id}/restore` - Restore item
- `DELETE /api/v1/trash/{type}/{id}` - Permanently delete

## Permission Model

Access is resolved in this order:

1. **Owner** → Full access (edit)
2. **Active Share** → Access based on role (viewer/editor)
3. **Active Public Link** → View-only access
4. **Deny** → No access

For folders, permissions also cascade from parent folders.

## Architecture

### Backend
- **Controllers**: Handle HTTP requests/responses
- **Services**: Business logic and permission checks
- **Repositories**: Data access via JPA
- **Entities**: JPA-managed database models

### Frontend
- **Pages**: Route-level components
- **Components**: Reusable UI components
- **Hooks**: Custom React logic (upload, auth)
- **Services**: API communication
- **Context**: Global state (auth, app state)

## License

MIT
