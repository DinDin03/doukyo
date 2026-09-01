import { Stack } from 'expo-router';

// The auth flow (sign-in / sign-up) — its own stack, shown when nobody's signed in.
export default function AuthLayout() {
  return <Stack screenOptions={{ headerShown: false, animation: 'fade' }} />;
}
