import api from './api'
import { setAccessToken } from './api'
import type { AuthResponse, LoginRequest, RegisterRequest, User } from '../types'

export const authService = {
  async register(data: RegisterRequest): Promise<AuthResponse> {
    const response = await api.post<AuthResponse>('/auth/register', data)
    setAccessToken(response.data.accessToken)
    return response.data
  },

  async login(data: LoginRequest): Promise<AuthResponse> {
    const response = await api.post<AuthResponse>('/auth/login', data)
    setAccessToken(response.data.accessToken)
    return response.data
  },

  async logout(): Promise<void> {
    await api.post('/auth/logout')
    setAccessToken(null)
  },

  async getMe(): Promise<User> {
    const response = await api.get<User>('/auth/me')
    return response.data
  },

  async refreshToken(): Promise<{ accessToken: string }> {
    const response = await api.post<{ accessToken: string }>('/auth/refresh')
    const { accessToken } = response.data
    setAccessToken(accessToken)
    return { accessToken }
  },

  getGoogleOAuthUrl(): string {
    const clientId = import.meta.env.VITE_GOOGLE_CLIENT_ID
    const redirectUri = `${window.location.origin}/oauth/callback`
    return `https://accounts.google.com/o/oauth2/v2/auth?client_id=${clientId}&redirect_uri=${encodeURIComponent(redirectUri)}&response_type=code&scope=openid%20email%20profile`
  },

  async updateProfile(data: { displayName?: string; mobileNumber?: string; profilePictureUrl?: string }): Promise<User> {
    const response = await api.patch<User>('/auth/profile', data)
    return response.data
  },

  async uploadProfilePicture(file: File): Promise<{ profilePictureUrl: string }> {
    const formData = new FormData()
    formData.append('file', file)
    const response = await api.post<{ profilePictureUrl: string }>('/auth/profile/picture', formData, {
      headers: {
        'Content-Type': undefined,
      },
    })
    return response.data
  },
}
