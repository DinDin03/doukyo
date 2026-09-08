import { useState } from 'react';
import { Pressable, ScrollView, StyleSheet, TextInput, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { useRouter } from 'expo-router';
import { Feather } from '@expo/vector-icons';
import { Body, Button, Field, Heading } from '../../src/design/ui';
import { colors, ink, radius } from '../../src/design/theme';
import { useAuth } from '../../src/auth/AuthContext';

function authError(e: unknown): string {
  const err = e as { errors?: { message: string }[]; graphQLErrors?: { message: string }[]; message?: string };
  return err?.errors?.[0]?.message ?? err?.graphQLErrors?.[0]?.message ?? err?.message ?? 'Something went wrong';
}

export default function SignUp() {
  const insets = useSafeAreaInsets();
  const router = useRouter();
  const { startSignUp, confirmSignUp, googleSignIn } = useAuth();

  // Two phases in one screen rather than a second route — the details never leave
  // this component, so there is nothing to pass between screens.
  const [codeSent, setCodeSent] = useState(false);
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [code, setCode] = useState('');
  const [busy, setBusy] = useState(false);
  const [notice, setNotice] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const run = async (action: () => Promise<void>) => {
    setBusy(true);
    setError(null);
    try {
      await action();
    } catch (e) {
      setError(authError(e));
    } finally {
      setBusy(false);
    }
  };

  // Optimistic: move to the code screen immediately and let the request finish in
  // the background. The response carries no information — startSignUp always
  // answers the same thing — so there is nothing to wait for. Waiting would pin
  // the user on "Sending…" for the length of an SMTP round trip.
  const sendCode = () => {
    setError(null);
    setNotice(null);
    setCodeSent(true);
    startSignUp(name.trim(), email.trim(), password).catch((e) => setError(authError(e)));
  };

  const resend = () => {
    setError(null);
    setNotice('If it hasn’t arrived, give it a minute before trying again.');
    startSignUp(name.trim(), email.trim(), password).catch((e) => setError(authError(e)));
  };

  // The auth gate takes over once this sets a user.
  const verify = () => run(() => confirmSignUp(email.trim(), code.trim()));

  return (
    <ScrollView
      style={styles.root}
      contentContainerStyle={[styles.content, { paddingTop: insets.top + 20 }]}
      keyboardShouldPersistTaps="handled"
    >
      <Pressable
        onPress={() => (codeSent ? setCodeSent(false) : router.back())}
        hitSlop={8}
        style={styles.back}
      >
        <Feather name="arrow-left" size={21} color={colors.text} />
      </Pressable>

      {!codeSent ? (
        <>
          <Heading size={40} weight="regular" style={{ marginTop: 18 }}>
            Create your account
          </Heading>
          <Body size={14} color={ink(0.6)} style={{ marginTop: 6, lineHeight: 21 }}>
            We&apos;ll email you a code to confirm the address is yours.
          </Body>

          <View style={styles.form}>
            <Field label="Name" value={name} onChangeText={setName} placeholder="Your name" autoCapitalize="words" />
            <Field
              label="Email"
              value={email}
              onChangeText={setEmail}
              placeholder="you@example.com"
              autoCapitalize="none"
              keyboardType="email-address"
            />
            <Field
              label="Password"
              value={password}
              onChangeText={setPassword}
              placeholder="At least 8 characters"
              secureTextEntry
            />
          </View>

          {error ? (
            <Body size={13} color={colors.accentRamp[700]} style={{ marginTop: 14 }}>
              {error}
            </Body>
          ) : null}

          <Button
            label="Send code"
            block
            disabled={!name.trim() || !email.trim() || password.length < 8}
            onPress={sendCode}
            style={{ marginTop: 22 }}
          />
          <Button
            label="Continue with Google"
            variant="secondary"
            block
            onPress={() => run(async () => void (await googleSignIn()))}
            style={{ marginTop: 10 }}
          />

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
        </>
      ) : (
        <>
          <Heading size={40} weight="regular" style={{ marginTop: 18 }}>
            Check your email
          </Heading>
          {/* Deliberately non-committal: saying "we sent a code" would confirm the
              address is unregistered, which is the leak the uniform response closes. */}
          <Body size={14} color={ink(0.6)} style={{ marginTop: 6, lineHeight: 21 }}>
            If {email.trim()} can receive mail, a six-digit code is on its way. It
            expires in 10 minutes.
          </Body>

          <Body size={12} color={ink(0.7)} style={{ marginTop: 26, marginBottom: 6 }}>
            Verification code
          </Body>
          <TextInput
            value={code}
            onChangeText={(t) => setCode(t.replace(/\D/g, '').slice(0, 6))}
            placeholder="——————"
            placeholderTextColor={ink(0.3)}
            keyboardType="number-pad"
            maxLength={6}
            style={styles.codeInput}
          />

          {error ? (
            <Body size={13} color={colors.accentRamp[700]} style={{ marginTop: 14 }}>
              {error}
            </Body>
          ) : notice ? (
            <Body size={13} color={ink(0.55)} style={{ marginTop: 14 }}>
              {notice}
            </Body>
          ) : null}

          <Button
            label={busy ? 'Checking…' : 'Verify'}
            block
            disabled={busy || code.length < 6}
            onPress={verify}
            style={{ marginTop: 20 }}
          />
          <Button
            label="Resend code"
            variant="secondary"
            block
            onPress={resend}
            style={{ marginTop: 10 }}
          />

          <Body size={11.5} color={ink(0.45)} style={styles.hint}>
            Wrong address? Go back and change it.
          </Body>
        </>
      )}
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
  codeInput: {
    minHeight: 56,
    borderWidth: 1,
    borderColor: colors.divider,
    borderRadius: radius.md,
    fontFamily: 'CormorantGaramond_400Regular',
    fontSize: 26,
    letterSpacing: 12,
    textAlign: 'center',
    color: colors.text,
  },
  hint: { textAlign: 'center', marginTop: 22 },
});
