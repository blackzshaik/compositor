export function resolvePreviewUrl(
  url?: string | null,
  httpBase: string = 'http://localhost:3001'
): string {
  if (!url) {
    return `${httpBase}/api/preview/latest.png?v=${Date.now()}`;
  }
  if (url.startsWith('http://') || url.startsWith('https://') || url.startsWith('data:')) {
    return url;
  }
  const cleanUrl = url.startsWith('/') ? url : `/${url}`;
  return `${httpBase}${cleanUrl}`;
}
