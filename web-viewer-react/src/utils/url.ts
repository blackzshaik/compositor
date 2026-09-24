export function resolvePreviewUrl(
  url?: string | null,
  httpBase: string = 'http://localhost:3001',
  timestamp?: number | null
): string {
  if (!url) {
    return '';
  }
  let fullUrl: string;
  if (url.startsWith('http://') || url.startsWith('https://') || url.startsWith('data:')) {
    fullUrl = url;
  } else {
    const cleanUrl = url.startsWith('/') ? url : `/${url}`;
    fullUrl = `${httpBase}${cleanUrl}`;
  }

  if (timestamp && !fullUrl.startsWith('data:') && !fullUrl.includes('?t=') && !fullUrl.includes('&t=')) {
    const separator = fullUrl.includes('?') ? '&' : '?';
    return `${fullUrl}${separator}t=${timestamp}`;
  }

  return fullUrl;
}
