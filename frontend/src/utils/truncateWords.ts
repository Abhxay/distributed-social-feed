export function truncateWords(text: string, limit: number): { text: string; truncated: boolean } {
  const words = text.trim().split(/\s+/).filter(Boolean);
  if (words.length <= limit) {
    return { text, truncated: false };
  }
  return { text: `${words.slice(0, limit).join(' ')}…`, truncated: true };
}
