import { Stack } from 'expo-router';
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
import { useEffect } from 'react';
import { apolloClient } from '../src/apollo';

// Keep the splash screen up until the Classical fonts have loaded, so the UI never
// flashes in a fallback face first.
SplashScreen.preventAutoHideAsync();

// The ROOT layout — wraps the whole app in ApolloProvider and loads the fonts.
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
        <Stack screenOptions={{ headerShown: false }} />
      </ApolloProvider>
    </SafeAreaProvider>
  );
}
