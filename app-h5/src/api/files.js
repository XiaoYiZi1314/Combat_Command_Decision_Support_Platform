import { apiRequest, authHeader, withRefresh } from './client.js';

export function fetchBizFile(fileId) {
  return withRefresh(() => apiRequest(`/files/${fileId}`, {
    headers: authHeader()
  }));
}

export function deleteBizFile(fileId) {
  return withRefresh(() => apiRequest(`/files/${fileId}`, {
    method: 'DELETE',
    headers: authHeader()
  }));
}

export async function uploadBizFile(bizType, bizId, file) {
  const presign = await withRefresh(() => apiRequest('/files/presign', {
    method: 'POST',
    headers: authHeader(),
    body: {
      bizType,
      bizId,
      fileName: file.name,
      contentType: file.type || 'application/octet-stream',
      sizeBytes: file.size
    }
  }));
  try {
    const response = await fetch(presign.uploadUrl, {
      method: 'PUT',
      headers: { 'Content-Type': file.type || 'application/octet-stream' },
      body: file
    });
    if (!response.ok) {
      const error = new Error('文件上传失败，请稍后重试');
      error.code = 'FILE_UPLOAD_FAILED';
      throw error;
    }
    return await withRefresh(() => apiRequest(`/files/${presign.fileId}/confirm`, {
      method: 'POST',
      headers: authHeader(),
      body: { confirmTicket: presign.confirmTicket }
    }));
  } catch (err) {
    try {
      await deleteBizFile(presign.fileId);
    } catch (cleanupErr) {
      console.warn('file upload cleanup failed', cleanupErr.code || cleanupErr.name);
    }
    if (err.code === 'FILE_UPLOAD_FAILED') {
      throw err;
    }
    if (err.code && err.code !== 'NETWORK') {
      throw err;
    }
    const error = new Error('文件上传失败，请检查网络');
    error.code = 'NETWORK';
    throw error;
  }
}
