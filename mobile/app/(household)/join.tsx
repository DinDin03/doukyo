import { useState } from 'react';
import { Pressable, StyleSheet, TextInput, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { useRouter } from 'expo-router';
import { Feather } from '@expo/vector-icons';
import { Body, Button, Divider, Heading } from '../../src/design/ui';
import { colors, ink, radius } from '../../src/design/theme';
import { useHousehold } from '../../src/household/HouseholdContext';

function errorMessage(e: unknown): string {
  const err = e as { errors?: { message: string }[]; graphQLErrors?: { message: string }[]; message?: string };
  return err?.errors?.[0]?.message ?? err?.graphQLErrors?.[0]?.message ?? err?.message ?? 'Something went wrong';
}

export default function JoinHousehold() {
  const router = useRouter();
  const insets = useSafeAreaInsets();
  const { joinHousehold } = useHousehold();
  const [code, setCode] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const submit = async () => {
    setBusy(true);
    setError(null);
    try {
      await joinHousehold(code.trim());
      // The root gate redirects into the app once `households` is non-empty.
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  };

  return (
    <View style={[styles.root, { paddingTop: insets.top + 20, paddingBottom: insets.bottom + 30 }]}>
      <Pressable onPress={() => router.back()} hitSlop={8} style={styles.back}>
        <Feather name="arrow-left" size={21} color={colors.text} />
      </Pressable>

      <Heading size={34} weight="regular" style={{ marginTop: 22 }}>
        Join a house
      </Heading>
      <Body size={13.5} color={ink(0.6)} style={{ marginTop: 6, lineHeight: 21 }}>
        Ask a flatmate for the six-letter code, or open the link they sent.
      </Body>
      <Divider style={{ marginVertical: 24 }} />

      <Body size={12} color={ink(0.7)} style={{ marginBottom: 6 }}>
        Invite code
      </Body>
      <TextInput
        value={code}
        onChangeText={(t) => setCode(t.toUpperCase())}
        placeholder="——————"
        placeholderTextColor={ink(0.3)}
        autoCapitalize="characters"
        maxLength={6}
        style={styles.codeInput}
      />

      {error ? (
        <Body size={13} color={colors.accentRamp[700]} style={{ marginTop: 14 }}>
          {error}
        </Body>
      ) : null}

      <Button
        label={busy ? 'Joining…' : 'Join'}
        block
        disabled={busy || code.trim().length < 6}
        onPress={submit}
        style={{ marginTop: 20 }}
      />

      <Body size={11.5} color={ink(0.45)} style={styles.foot}>
        Joining adds you to the members list. Nothing is shared outside the house.
      </Body>
    </View>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: colors.bg, paddingHorizontal: 30 },
  back: { width: 34, height: 34, alignItems: 'center', justifyContent: 'center', marginLeft: -8 },
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
  foot: { textAlign: 'center', marginTop: 'auto' },
});
