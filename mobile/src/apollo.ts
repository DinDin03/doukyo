import { ApolloClient, InMemoryCache, HttpLink } from '@apollo/client';
import Constants from 'expo-constants';

// --- The "localhost problem" -------------------------------------------------
// On a phone, `localhost` means the PHONE, not your Mac. The backend runs on your
// Mac at port 8080, so the app must reach it by the Mac's LAN IP.
//
// We don't hardcode that IP (it changes between networks). Expo Go already
// connected to Metro using your Mac's IP, and Expo exposes that host as
// `Constants.expoConfig.hostUri` (e.g. "192.168.20.19:8081"). We take its IP and
// swap Metro's port (8081) for the backend's port (8080). So the URL follows your
// Mac automatically. Falls back to localhost (useful when running on web).
const metroHost = Constants.expoConfig?.hostUri?.split(':')[0] ?? 'localhost';
export const API_URL = `http://${metroHost}:8080/graphql`;

// The Apollo client: WHERE to send queries (link) + how to cache them (cache).
export const apolloClient = new ApolloClient({
  link: new HttpLink({ uri: API_URL }),
  cache: new InMemoryCache(),
});
