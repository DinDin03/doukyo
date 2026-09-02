import { useState } from 'react';
import { Pressable, StyleSheet, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { useRouter } from 'expo-router';
import { Feather } from '@expo/vector-icons';
import { Body, Button, Divider, Field, Heading } from '../../src/design/ui';
import { colors, ink } from '../../src/design/theme';
import { useHousehold } from '../../src/household/HouseholdContext';

function errorMessage(e: unknown): string {
  const err = e as { errors?: { message: string }[]; graphQLErrors?: { message: string }[]; message?: string };
  return err?.errors?.[0]?.message ?? err?.graphQLErrors?.[0]?.message ?? err?.message ?? 'Something went wrong';
}

export default function CreateHousehold() {
  const router = useRouter();
  const insets = useSafeAreaInsets();
  const { createHousehold } = useHousehold();
  const [name, setName] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const submit = async () => {
    setBusy(true);
    setError(null);
    try {
      await createHousehold(name.trim());
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
        Name your house
      </Heading>
      <Body size={13.5} color={ink(0.6)} style={{ marginTop: 6, lineHeight: 21 }}>
        You'll get a code to invite flatmates once it's created.
      </Body>
      <Divider style={{ marginVertical: 24 }} />

      <Field label="Household name" value={name} onChangeText={setName} placeholder="Flat 7" autoCapitalize="words" />

      {error ? (
        <Body size={13} color={colors.accentRamp[700]} style={{ marginTop: 14 }}>
          {error}
        </Body>
      ) : null}

      <Button
        label={busy ? 'Creating…' : 'Create household'}
        block
        disabled={busy || name.trim().length === 0}
        onPress={submit}
        style={{ marginTop: 20 }}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: colors.bg, paddingHorizontal: 30 },
  back: { width: 34, height: 34, alignItems: 'center', justifyContent: 'center', marginLeft: -8 },
});
