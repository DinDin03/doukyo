import { useState } from 'react';
import { Pressable, StyleSheet, View } from 'react-native';
import { useRouter } from 'expo-router';
import { AppHeader } from '../src/design/AppHeader';
import { Body, Heading, Kicker, Num, Screen } from '../src/design/ui';
import { colors, ink, radius } from '../src/design/theme';

const ROWS = [
  { id: '1', title: 'Sam owes you', sub: 'From 3 shared expenses', amount: '$48.00', owed: true, action: 'Remind' },
  { id: '2', title: 'Jules owes you', sub: 'From 2 shared expenses', amount: '$52.00', owed: true, action: 'Remind' },
  { id: '3', title: 'You owe Ravi', sub: 'Dinner last Sunday', amount: '$24.00', owed: false, action: 'Settle' },
];

export default function BalancesScreen() {
  const router = useRouter();
  const [done, setDone] = useState<Record<string, boolean>>({});

  return (
    <View style={styles.root}>
      <AppHeader kicker="Expenses" title="Balances" onBack={() => router.back()} backLabel="Back" />
      <Screen>
        <View style={styles.hero}>
          <Kicker color={ink(0.5)}>Your net position</Kicker>
          <Num size={56} weight="light" style={{ marginTop: 6, letterSpacing: -1 }}>
            $124.00
          </Num>
          <Body size={12.5} color={ink(0.55)} style={{ marginTop: 4 }}>
            across 4 flatmates
          </Body>
        </View>

        <Kicker color={ink(0.45)} style={{ marginTop: 20, marginBottom: 4 }}>
          The simplest way to settle
        </Kicker>
        {ROWS.map((r) => (
          <View key={r.id} style={styles.row}>
            <View style={{ flex: 1, minWidth: 0 }}>
              <Heading size={17}>{r.title}</Heading>
              <Body size={11.5} color={ink(0.52)} style={{ marginTop: 2 }}>
                {done[r.id] ? 'Settled' : r.sub}
              </Body>
            </View>
            <Num size={18} color={r.owed ? colors.accentRamp[700] : ink(0.6)}>
              {r.amount}
            </Num>
            <Pressable
              onPress={() => setDone((d) => ({ ...d, [r.id]: true }))}
              style={[styles.settle, done[r.id] && { borderColor: colors.divider }]}
            >
              <Body size={12.5} color={done[r.id] ? ink(0.4) : colors.accent} style={styles.settleLabel}>
                {done[r.id] ? 'Done' : r.action}
              </Body>
            </Pressable>
          </View>
        ))}

        <View style={styles.note}>
          <Body size={12.5} color={ink(0.62)}>
            Doukyo nets everyone off so the house settles in the fewest possible payments — three transfers instead of nine.
          </Body>
        </View>
      </Screen>
    </View>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: colors.bg },
  hero: { alignItems: 'center', paddingVertical: 6, paddingBottom: 18, borderBottomWidth: 1, borderBottomColor: colors.divider },
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    paddingVertical: 15,
    borderBottomWidth: 1,
    borderBottomColor: colors.divider,
  },
  settle: { borderWidth: 1, borderColor: colors.accent, borderRadius: radius.pill, paddingHorizontal: 11, paddingVertical: 7 },
  settleLabel: { fontFamily: colors ? undefined : undefined },
  note: { borderWidth: 1, borderColor: colors.divider, borderRadius: radius.md, padding: 14, marginTop: 20 },
});
