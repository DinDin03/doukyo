import { Stack, useRouter, useSegments } from 'expo-router';
import { ApolloProvider } from '@apollo/client/react';
import { useFonts } from 'expo-font';
import {
  CormorantGaramond_300Light,
  CormorantGaramond_400Regular,
  CormorantGaramond_500Medium,
  CormorantGaramond_600SemiBold,
} from '@expo-google-fonts/cormorant-garamond';
import { Lora_400Regular, Lora_500Medium, Lora_600SemiBold } from '@expo-google-fonts/lora';
import * as SplashScreen from 'expo-splash-screen';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import { ActivityIndicator, StyleSheet, View } from 'react-native';
import { useEffect, useRef } from 'react';
import { apolloClient } from '../src/apollo';
import { AuthProvider, useAuth } from '../src/auth/AuthContext';
import { HouseholdProvider, useHousehold } from '../src/household/HouseholdContext';
import { colors } from '../src/design/theme';
import { useBubbleTransition } from '../src/design/BubbleTransition';

SplashScreen.preventAutoHideAsync();

// The GATE: three destinations, decided by auth + household state.
//   no user             -> (auth)      sign in / sign up
//   user, no household  -> (household) create or join
//   user, has household -> (tabs)      the app
function RootNavigator() {
  const { user, loading: authLoading } = useAuth();
  const { households, loading: householdLoading } = useHousehold();
  const segments = useSegments();
  const router = useRouter();
  const { bubble, play } = useBubbleTransition();
  const hasRevealed = useRef(false);

  // Household state only matters once we know who the user is — don't wait on it
  // while there's nobody signed in.
  const settled = !authLoading && (!user || !householdLoading);

  useEffect(() => {
    if (!settled) return;
    const group = segments[0];
    const inAuth = group === '(auth)';
    const inHousehold = group === '(household)';

    // Ask "is this a place I'm ALLOWED to be", not "is this the one place I
    // belong". A single target would treat every standalone route (/add,
    // /balances, /activity, /recipe) as misplaced and bounce it straight back
    // to the tabs — which looks exactly like the app reloading.
    const misplaced = !user
      ? !inAuth // signed out: only the auth screens
      : households.length === 0
        ? !inHousehold // no household yet: only the create/join flow
        : inAuth || inHousehold; // signed in with a household: anywhere but those two

    const swap = () => {
      if (!user) router.replace('/(auth)/sign-in')
      // Named leaf rather than an index route: "/" already resolves to (tabs)/index,
      // so a second group-level index would be a duplicate claim on the same path.
      else if (households.length === 0) router.replace('/(household)/choose');
      else router.replace('/(tabs)');
    };

    if (!hasRevealed.current) {
      hasRevealed.current = true;
      play(() => {
        if (misplaced) swap();
      }); // first reveal always plays, even with no redirect needed
    } else if (misplaced) {
      play(swap);
    }
  }, [user, households.length, settled, segments, router, play]);

  // The Stack must render on the very first pass: expo-router queues any
  // navigation made before a navigator mounts, and flushes it against an
  // empty root state. Loading is an overlay, never a replacement.
  return (
    <>
      <Stack screenOptions={{ headerShown: false }} />
      {!settled && (
        <View style={styles.loading}>
          <ActivityIndicator color={colors.accent} />
        </View>
      )}
      {bubble}
    </>
  );
}

export default function RootLayout() {
  const [fontsLoaded] = useFonts({
    CormorantGaramond_300Light,
    CormorantGaramond_400Regular,
    CormorantGaramond_500Medium,
    CormorantGaramond_600SemiBold,
    Lora_400Regular,
    Lora_500Medium,
    Lora_600SemiBold,
  });

  useEffect(() => {
    if (fontsLoaded) SplashScreen.hideAsync();
  }, [fontsLoaded]);

  if (!fontsLoaded) return null;

  return (
    <SafeAreaProvider>
      <ApolloProvider client={apolloClient}>
        <AuthProvider>
          <HouseholdProvider>
            <RootNavigator />
          </HouseholdProvider>
        </AuthProvider>
      </ApolloProvider>
    </SafeAreaProvider>
  );
}

const styles = {
  loading: {
    ...StyleSheet.absoluteFillObject,
    backgroundColor: colors.bg,
    alignItems: 'center' as const,
    justifyContent: 'center' as const,
    zIndex: 40,
  },
};
