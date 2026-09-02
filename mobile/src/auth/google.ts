import { GoogleSignin } from '@react-native-google-signin/google-signin';

// Must match the backend's doukyo.security.google-client-id — this is the
// audience the ID token is minted for.
const WEB_CLIENT_ID = '1029240944963-t6fhppcjrqco70mqmo19ds9d4rhkh8qi.apps.googleusercontent.com';

GoogleSignin.configure({ webClientId: WEB_CLIENT_ID });

// Runs the native Google sign-in sheet and returns the ID token, or null if the
// user dismissed it. Throws for real failures (no Play Services, network, etc).
export async function getGoogleIdToken(): Promise<string | null> {
  await GoogleSignin.hasPlayServices();
  const result = await GoogleSignin.signIn();
  if (result.type !== 'success') return null;
  return result.data.idToken;
}

// Clears the SDK's own cached "last used account" for this app — without this,
// signIn() silently reuses that account instead of showing the picker again.
// Does NOT sign the user out of Google itself, only out of Doukyo's use of it.
export async function signOutOfGoogle(): Promise<void> {
  try {
    await GoogleSignin.signOut();
  } catch {
    // Not signed in with Google, or SDK not ready — fine to ignore.
  }
}
