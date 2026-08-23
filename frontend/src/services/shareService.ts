import api from './api'
import type { Share, PublicLink, ResourceType, ShareRole } from '../types'

export const shareService = {
  // User sharing
  async createShare(
    resourceType: ResourceType,
    resourceId: string,
    targetUserEmail: string,
    role: ShareRole
  ): Promise<Share> {
    const response = await api.post<Share>('/shares', {
      resourceType,
      resourceId,
      targetUserEmail,
      role,
    })
    return response.data
  },

  async getSharedWithMe(): Promise<Share[]> {
    const response = await api.get<Share[]>('/shares/shared-with-me')
    return response.data
  },

  async revokeShare(shareId: string): Promise<void> {
    await api.delete(`/shares/${shareId}`)
  },

  // Public links
  async createPublicLink(
    resourceType: ResourceType,
    resourceId: string,
    options?: {
      expiresAt?: string | null
      password?: string | null
    }
  ): Promise<PublicLink> {
    const response = await api.post<PublicLink>('/public-links', {
      resourceType,
      resourceId,
      expiresAt: options?.expiresAt || null,
      password: options?.password || null,
    })
    return response.data
  },

  async getPublicLinkInfo(token: string): Promise<{
    resourceType: ResourceType
    resourceName: string
    ownerName: string
    requiresPassword: boolean
    isExpired: boolean
    isRevoked: boolean
  }> {
    const response = await api.get(`/public-links/${token}`)
    return response.data
  },

  async verifyPublicLinkPassword(token: string, password: string): Promise<{ valid: boolean }> {
    const response = await api.post(`/public-links/${token}/verify`, { password })
    return response.data
  },

  async revokePublicLink(linkId: string): Promise<void> {
    await api.delete(`/public-links/${linkId}`)
  },
}
