import { StyleSheet, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { useRouter } from 'expo-router';
import { Body, Button, Heading } from '../../src/design/ui';
import { colors, ink } from '../../src/design/theme';

export default function HouseholdChoice() {
  const router = useRouter();
  const insets = useSafeAreaInsets();
  return (
    <View style={[styles.root, { paddingTop: insets.top + 60, paddingBottom: insets.bottom + 36 }]}>
      <View style={{ flex: 1 }}>
        <Heading size={64} weight="light" style={{ letterSpacing: -1 }}>
          Doukyo
        </Heading>
        <Heading size={26} weight="light" color={colors.accentRamp[700]} style={styles.kanji}>
          同居
        </Heading>
        <View style={styles.rule} />
        <Body size={14.5} color={ink(0.66)} style={styles.tagline}>
          One more step — create your household, or join one with a code.
        </Body>
      </View>
      <View style={{ gap: 10 }}>
        <Button label="Create a household" block onPress={() => router.push('/(household)/create')} />
        <Button label="Join with a code" variant="secondary" block onPress={() => router.push('/(household)/join')} />
        <Body size={11} color={ink(0.45)} style={styles.foot}>
          No accounts, no admin. Everyone in the house is equal.
        </Body>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: colors.bg, paddingHorizontal: 30 },
  kanji: { letterSpacing: 6, marginTop: 2 },
  rule: { height: 1, backgroundColor: colors.divider, marginVertical: 18, maxWidth: 270 },
  tagline: { maxWidth: 280, lineHeight: 22 },
  foot: { textAlign: 'center', marginTop: 10 },
});
