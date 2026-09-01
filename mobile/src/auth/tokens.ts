import * as SecureStore from 'expo-secure-store';

// Tokens live in the device keychain (iOS Keychain / Android Keystore) — encrypted
// at rest, never in plain AsyncStorage. We also record WHEN the access token was
// saved so we can refresh it before it expires (its TTL is ~15 min server-side).
const ACCESS = 'doukyo.accessToken';
const REFRESH = 'doukyo.refreshToken';
const SAVED_AT = 'doukyo.tokenSavedAt';

const ACCESS_TTL_MS = 15 * 60 * 1000; // must match the backend's access-ttl-minutes

export async function saveTokens(accessToken: string, refreshToken: string): Promise<void> {
  await SecureStore.setItemAsync(ACCESS, accessToken);
  await SecureStore.setItemAsync(REFRESH, refreshToken);
  await SecureStore.setItemAsync(SAVED_AT, String(Date.now()));
}

export function getAccessToken(): Promise<string | null> {
  return SecureStore.getItemAsync(ACCESS);
}

export function getRefreshToken(): Promise<string | null> {
  return SecureStore.getItemAsync(REFRESH);
}

// True when the access token is missing or within a minute of its TTL — time to refresh.
export async function isAccessStale(): Promise<boolean> {
  const savedAt = await SecureStore.getItemAsync(SAVED_AT);
  if (!savedAt) return true;
  return Date.now() - Number(savedAt) > ACCESS_TTL_MS - 60_000;
}

export async function clearTokens(): Promise<void> {
  await Promise.all([ACCESS, REFRESH, SAVED_AT].map((k) => SecureStore.deleteItemAsync(k)));
}
