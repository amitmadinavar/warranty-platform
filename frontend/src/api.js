const API_BASE = import.meta.env.VITE_API_BASE_URL || '/api';

function clearAuthAndRedirect() {
  localStorage.removeItem('warrantyos_user');
  localStorage.removeItem('warrantyos_token');
  if (window.location.pathname !== '/') {
    window.location.assign('/');
  }
}

export async function api(path, options = {}) {
  const token = localStorage.getItem('warrantyos_token');
  const headers = {
    'Content-Type': 'application/json',
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...(options.headers || {})
  };

  const response = await fetch(`${API_BASE}${path}`, {
    method: options.method || 'GET',
    headers,
    body: options.body === undefined ? undefined : JSON.stringify(options.body)
  });

  const data = await response.json().catch(() => ({}));

  // 401 means the JWT is missing, expired or invalid. Clear the stale
  // client session and return the user to the login screen.
  if (response.status === 401) {
    clearAuthAndRedirect();
  }

  // 403 is a real authorisation failure. Do not silently log the user out;
  // surface the backend's message so role/permission issues are visible.
  if (!response.ok) {
    throw new Error(data?.message || data?.error || `Request failed (${response.status})`);
  }

  return data;
}

export const get = path => api(path);
export const post = (path, body) => api(path, { method: 'POST', body });
export const put = (path, body) => api(path, { method: 'PUT', body });
