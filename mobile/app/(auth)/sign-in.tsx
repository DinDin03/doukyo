import { useState } from 'react';
import { Pressable, ScrollView, StyleSheet, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { useRouter } from 'expo-router';
import { Body, Button, Field, Heading } from '../../src/design/ui';
import { colors, ink } from '../../src/design/theme';
import { useAuth } from '../../src/auth/AuthContext';

function authError(e: unknown): string {
  const err = e as { errors?: { message: string }[]; graphQLErrors?: { message: string }[]; message?: string };
  return err?.errors?.[0]?.message ?? err?.graphQLErrors?.[0]?.message ?? err?.message ?? 'Something went wrong';
}

export default function SignIn() {
  const insets = useSafeAreaInsets();
  const router = useRouter();
  const { signIn, googleSignIn } = useAuth();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const submit = async () => {
    setBusy(true);
    setError(null);
    try {
      await signIn(email.trim(), password);
      // The auth gate (root layout) redirects into the app once `user` is set.
    } catch (e) {
      setError(authError(e));
    } finally {
      setBusy(false);
    }
  };

  const submitGoogle = async () => {
    setBusy(true);
    setError(null);
    try {
      await googleSignIn(); // 'cancelled' just means the user backed out — no error
    } catch (e) {
      setError(authError(e));
    } finally {
      setBusy(false);
    }
  };

  return (
    <ScrollView style={styles.root} contentContainerStyle={[styles.content, { paddingTop: insets.top + 60 }]} keyboardShouldPersistTaps="handled">
      <Heading size={56} weight="light" style={{ letterSpacing: -1 }}>
        Doukyo
      </Heading>
      <Heading size={22} weight="light" color={colors.accentRamp[700]} style={styles.kanji}>
        同居
      </Heading>
      <Body size={14} color={ink(0.6)} style={{ marginTop: 14 }}>
        Welcome back. Sign in to your house.
      </Body>

      <View style={styles.form}>
        <Field label="Email" value={email} onChangeText={setEmail} placeholder="you@example.com" autoCapitalize="none" keyboardType="email-address" />
        <Field label="Password" value={password} onChangeText={setPassword} placeholder="••••••••" secureTextEntry />
      </View>

      {error ? (
        <Body size={13} color={colors.accentRamp[700]} style={{ marginTop: 14 }}>
          {error}
        </Body>
      ) : null}

      <Button label={busy ? 'Signing in…' : 'Sign in'} block onPress={submit} style={{ marginTop: 22 }} />
      <Button label="Continue with Google" variant="secondary" block onPress={submitGoogle} style={{ marginTop: 10 }} />

      <View style={styles.foot}>
        <Body size={13} color={ink(0.55)}>
          New here?{' '}
        </Body>
        <Pressable onPress={() => router.push('/(auth)/sign-up')} hitSlop={8}>
          <Body size={13} color={colors.accentRamp[700]} style={styles.link}>
            Create an account
          </Body>
        </Pressable>
      </View>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: colors.bg },
  content: { paddingHorizontal: 30, paddingBottom: 40 },
  kanji: { letterSpacing: 5, marginTop: 2 },
  form: { gap: 14, marginTop: 30 },
  foot: { flexDirection: 'row', alignItems: 'center', justifyContent: 'center', marginTop: 26 },
  link: { fontFamily: 'CormorantGaramond_600SemiBold', textDecorationLine: 'underline' },
});
