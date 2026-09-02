import { useState } from 'react';
import { Pressable, ScrollView, StyleSheet, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { useRouter } from 'expo-router';
import { Feather } from '@expo/vector-icons';
import { Body, Button, Field, Heading } from '../../src/design/ui';
import { colors, ink } from '../../src/design/theme';
import { useAuth } from '../../src/auth/AuthContext';

function authError(e: unknown): string {
  const err = e as { errors?: { message: string }[]; graphQLErrors?: { message: string }[]; message?: string };
  return err?.errors?.[0]?.message ?? err?.graphQLErrors?.[0]?.message ?? err?.message ?? 'Something went wrong';
}

export default function SignUp() {
  const insets = useSafeAreaInsets();
  const router = useRouter();
  const { signUp, googleSignIn } = useAuth();
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const submit = async () => {
    setBusy(true);
    setError(null);
    try {
      await signUp(name.trim(), email.trim(), password);
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
      await googleSignIn();
    } catch (e) {
      setError(authError(e));
    } finally {
      setBusy(false);
    }
  };

  return (
    <ScrollView style={styles.root} contentContainerStyle={[styles.content, { paddingTop: insets.top + 20 }]} keyboardShouldPersistTaps="handled">
      <Pressable onPress={() => router.back()} hitSlop={8} style={styles.back}>
        <Feather name="arrow-left" size={21} color={colors.text} />
      </Pressable>

      <Heading size={40} weight="regular" style={{ marginTop: 18 }}>
        Create your account
      </Heading>
      <Body size={14} color={ink(0.6)} style={{ marginTop: 6, lineHeight: 21 }}>
        One account, then create or join a household.
      </Body>

      <View style={styles.form}>
        <Field label="Name" value={name} onChangeText={setName} placeholder="Your name" autoCapitalize="words" />
        <Field label="Email" value={email} onChangeText={setEmail} placeholder="you@example.com" autoCapitalize="none" keyboardType="email-address" />
        <Field label="Password" value={password} onChangeText={setPassword} placeholder="At least 8 characters" secureTextEntry />
      </View>

      {error ? (
        <Body size={13} color={colors.accentRamp[700]} style={{ marginTop: 14 }}>
          {error}
        </Body>
      ) : null}

      <Button label={busy ? 'Creating…' : 'Create account'} block onPress={submit} style={{ marginTop: 22 }} />
      <Button label="Continue with Google" variant="secondary" block onPress={submitGoogle} style={{ marginTop: 10 }} />

      <View style={styles.foot}>
        <Body size={13} color={ink(0.55)}>
          Already have an account?{' '}
        </Body>
        <Pressable onPress={() => router.push('/(auth)/sign-in')} hitSlop={8}>
          <Body size={13} color={colors.accentRamp[700]} style={styles.link}>
            Sign in
          </Body>
        </Pressable>
      </View>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: colors.bg },
  content: { paddingHorizontal: 30, paddingBottom: 40 },
  back: { width: 34, height: 34, alignItems: 'center', justifyContent: 'center', marginLeft: -8 },
  form: { gap: 14, marginTop: 26 },
  foot: { flexDirection: 'row', alignItems: 'center', justifyContent: 'center', marginTop: 26 },
  link: { fontFamily: 'CormorantGaramond_600SemiBold', textDecorationLine: 'underline' },
});
