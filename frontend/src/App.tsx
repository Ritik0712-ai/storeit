import { Routes, Route, Navigate } from 'react-router-dom'
import { useAuthStore } from './context/AuthContext'
import { ProtectedRoute } from './components/common/ProtectedRoute'
import { ToastProvider } from './components/common/Toast'
import { useAuthInit } from './hooks/useAuthInit'

// Auth Pages
import LoginPage from './pages/LoginPage'
import SignupPage from './pages/SignupPage'
import OAuthCallbackPage from './pages/OAuthCallbackPage'

// Main App Pages
import Layout from './components/common/Layout'
import DrivePage from './pages/DrivePage'
import SharedPage from './pages/SharedPage'
import StarredPage from './pages/StarredPage'
import TrashPage from './pages/TrashPage'
import SearchPage from './pages/SearchPage'
import ShareLinkPage from './pages/ShareLinkPage'
import NotFoundPage from './pages/NotFoundPage'

function App() {
  const { isAuthenticated } = useAuthStore()
  useAuthInit() // Initialize auth state on app load

  return (
    <ToastProvider>
      <Routes>
        {/* Public auth routes */}
        <Route
          path="/login"
          element={isAuthenticated ? <Navigate to="/drive" replace /> : <LoginPage />}
        />
        <Route
          path="/signup"
          element={isAuthenticated ? <Navigate to="/drive" replace /> : <SignupPage />}
        />
        <Route path="/oauth/callback" element={<OAuthCallbackPage />} />

        {/* Public share link view */}
        <Route path="/share/:token" element={<ShareLinkPage />} />

        {/* Protected routes */}
        <Route element={<ProtectedRoute><Layout /></ProtectedRoute>}>
          <Route path="/drive" element={<DrivePage />} />
          <Route path="/drive/:folderId" element={<DrivePage />} />
          <Route path="/shared" element={<SharedPage />} />
          <Route path="/starred" element={<StarredPage />} />
          <Route path="/trash" element={<TrashPage />} />
          <Route path="/search" element={<SearchPage />} />
        </Route>

        {/* Default redirect */}
        <Route
          path="/"
          element={<Navigate to={isAuthenticated ? '/drive' : '/login'} replace />}
        />

        {/* 404 */}
        <Route path="*" element={<NotFoundPage />} />
      </Routes>
    </ToastProvider>
  )
}

export default App
