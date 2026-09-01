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
import { ActivityIndicator, View } from 'react-native';
import { useEffect } from 'react';
import { apolloClient } from '../src/apollo';
import { AuthProvider, useAuth } from '../src/auth/AuthContext';
import { colors } from '../src/design/theme';

SplashScreen.preventAutoHideAsync();

// The auth GATE: redirect based on whether someone is signed in and where they are.
function RootNavigator() {
  const { user, loading } = useAuth();
  const segments = useSegments();
  const router = useRouter();

  useEffect(() => {
    if (loading) return; // wait until we've restored the session
    const inAuthGroup = segments[0] === '(auth)';
    if (!user && !inAuthGroup) {
      router.replace('/(auth)/sign-in'); // not signed in -> to the sign-in screen
    } else if (user && inAuthGroup) {
      router.replace('/(tabs)'); // signed in but on an auth screen -> into the app
    }
  }, [user, loading, segments, router]);

  if (loading) {
    return (
      <View style={styles.loading}>
        <ActivityIndicator color={colors.accent} />
      </View>
    );
  }
  return <Stack screenOptions={{ headerShown: false }} />;
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
          <RootNavigator />
        </AuthProvider>
      </ApolloProvider>
    </SafeAreaProvider>
  );
}

const styles = {
  loading: { flex: 1, backgroundColor: colors.bg, alignItems: 'center' as const, justifyContent: 'center' as const },
};
