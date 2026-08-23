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
│   └── public/             # Static assets
├── .env.example             # Environment variables template
└── README.md
```

## Prerequisites

- **Java 17+** (for backend)
- **Node.js 18+** and **npm** (for frontend)
- **Maven 3.8+** (for backend build)
- **Supabase account** (for database and storage)
- **Google Cloud Console project** (for OAuth)

## Setup

### 1. Clone and Configure

```bash
# Clone the repository
git clone https://github.com/Ritik0712-ai/storeit.git
cd storeit

# Copy environment files
cp backend/.env.example backend/.env
cp frontend/.env.example frontend/.env
```

### 2. Configure Backend Environment

Edit `backend/.env` with your credentials:

```env
# Database (from Supabase)
DATABASE_URL=postgresql://postgres:[PASSWORD]@db.[PROJECT-REF].supabase.co:5432/postgres
DATABASE_USER=postgres
DATABASE_PASSWORD=[PASSWORD]

# JWT - Generate with: openssl rand -base64 32
JWT_SECRET=[YOUR-256-BIT-SECRET]

# Supabase
SUPABASE_URL=https://[PROJECT-REF].supabase.co
SUPABASE_SERVICE_ROLE_KEY=[YOUR-ANON-KEY]
SUPABASE_STORAGE_BUCKET=cloudvault-files

# Google OAuth (from Google Cloud Console)
GOOGLE_CLIENT_ID=[YOUR-CLIENT-ID].apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=[YOUR-CLIENT-SECRET]

# Frontend Origin
FRONTEND_ORIGIN=http://localhost:5173
```

### 3. Configure Frontend Environment

Edit `frontend/.env`:

```env
VITE_SUPABASE_URL=https://[PROJECT-REF].supabase.co
VITE_SUPABASE_ANON_KEY=[YOUR-ANON-KEY]
VITE_GOOGLE_CLIENT_ID=[YOUR-CLIENT-ID].apps.googleusercontent.com
VITE_API_URL=http://localhost:8080/api/v1
```

### 4. Supabase Setup

1. Create a new Supabase project
2. Enable **Email Auth** in Authentication settings
3. Create a storage bucket named `cloudvault-files`
4. Set bucket to private (authenticated access only)
5. Add storage policies for authenticated uploads/downloads

### 5. Google OAuth Setup

1. Go to [Google Cloud Console](https://console.cloud.google.com)
2. Create a new project or select existing
3. Enable the **Google+ API**
4. Create OAuth 2.0 credentials (Web application type)
5. Add authorized redirect URI: `{FRONTEND_ORIGIN}/oauth/callback`

## Running the Application

### Backend

```bash
cd backend

# Install dependencies and run
./mvnw spring-boot:run

# Or build and run JAR
./mvnw clean package
java -jar target/cloudvault-backend-1.0.0.jar
```

The backend runs on **http://localhost:8080**

### Frontend

```bash
cd frontend

# Install dependencies
npm install

# Run development server
npm run dev
```

The frontend runs on **http://localhost:5173**

## API Endpoints

### Authentication
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/auth/register` | Register new user |
| POST | `/api/v1/auth/login` | Login |
| POST | `/api/v1/auth/refresh` | Refresh access token |
| POST | `/api/v1/auth/logout` | Logout |
| GET | `/api/v1/auth/me` | Get current user |

### Folders
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/folders` | Create folder |
| GET | `/api/v1/folders/:id` | Get folder with children |
| PATCH | `/api/v1/folders/:id` | Update folder |
| DELETE | `/api/v1/folders/:id` | Move to trash |

### Files
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/files/init-upload` | Initialize upload |
| POST | `/api/v1/files/:id/complete-upload` | Complete upload |
| GET | `/api/v1/files/:id` | Get file info |
| GET | `/api/v1/files/:id/download-url` | Get download URL |
| PATCH | `/api/v1/files/:id` | Update file |
| DELETE | `/api/v1/files/:id` | Move to trash |

### Sharing
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/shares` | Create share |
| GET | `/api/v1/shares/shared-with-me` | Get shared items |
| DELETE | `/api/v1/shares/:id` | Revoke share |
| POST | `/api/v1/public-links` | Create public link |
| GET | `/api/v1/public-links/:token` | Get link info |

### Search & Organization
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/search?q=&type=` | Search files/folders |
| GET | `/api/v1/stars` | Get starred items |
| POST | `/api/v1/stars/:type/:id` | Star item |
| DELETE | `/api/v1/stars/:type/:id` | Unstar item |
| GET | `/api/v1/trash` | Get trash items |
| POST | `/api/v1/trash/:type/:id/restore` | Restore item |
| DELETE | `/api/v1/trash/:type/:id` | Permanent delete |

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
