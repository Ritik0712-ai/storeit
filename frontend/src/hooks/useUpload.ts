import { useCallback } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { fileService } from '../services/fileService'
import { useAppStore } from '../context/AppContext'
import { useToast } from '../components/common/Toast'

export function useUploadFiles() {
  const queryClient = useQueryClient()
  const { currentFolderId, addUpload, updateUpload } = useAppStore()
  const { showToast } = useToast()

  const pickFiles = useCallback(async () => {
    const input = document.createElement('input')
    input.type = 'file'
    input.multiple = true
    input.onchange = async (e) => {
      const files = (e.target as HTMLInputElement).files
      if (files?.length) {
        await uploadFiles(Array.from(files))
      }
    }
    input.click()
  }, [currentFolderId])

  const uploadFiles = useCallback(
    async (files: File[]) => {
      for (const file of files) {
        try {
          // Initialize upload
          const { fileId, uploadUrl, storagePath } = await fileService.initUpload(
            file.name,
            file.type,
            file.size,
            currentFolderId
          )

          addUpload({
            fileId,
            fileName: file.name,
            progress: 0,
            status: 'uploading',
          })

          // Upload directly to Supabase Storage
          const xhr = new XMLHttpRequest()
          xhr.upload.onprogress = (e) => {
            if (e.lengthComputable) {
              const progress = Math.round((e.loaded / e.total) * 100)
              updateUpload(fileId, { progress })
            }
          }

          xhr.onload = async () => {
            if (xhr.status >= 200 && xhr.status < 300) {
              // Complete upload
              await fileService.completeUpload(fileId)
              updateUpload(fileId, { progress: 100, status: 'complete' })
              showToast('success', `${file.name} uploaded successfully`)

              // Refresh folder contents
              queryClient.invalidateQueries({ queryKey: ['folder', currentFolderId || 'root'] })
              queryClient.invalidateQueries({ queryKey: ['files'] })
              queryClient.invalidateQueries({ queryKey: ['storage'] })
            } else {
              updateUpload(fileId, { status: 'error', error: 'Upload failed' })
              showToast('error', `Failed to upload ${file.name}`)
            }
          }

          xhr.onerror = () => {
            updateUpload(fileId, { status: 'error', error: 'Network error' })
            showToast('error', `Network error uploading ${file.name}`)
          }

          xhr.open('PUT', uploadUrl)
          xhr.setRequestHeader('Content-Type', file.type)
          xhr.send(file)
        } catch (error) {
          showToast('error', `Failed to start upload for ${file.name}`)
        }
      }
    },
    [currentFolderId, addUpload, updateUpload, queryClient, showToast]
  )

  const createFolder = useCallback(
    async (name: string, parentId: string | null) => {
      try {
        await fileService.createFolder(name, parentId)
        showToast('success', `Folder "${name}" created`)
        queryClient.invalidateQueries({ queryKey: ['folder', parentId || 'root'] })
      } catch (error) {
        showToast('error', 'Failed to create folder')
      }
    },
    [queryClient, showToast]
  )

  return { pickFiles, uploadFiles, createFolder }
}
