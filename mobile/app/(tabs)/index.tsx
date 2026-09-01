import { useState } from 'react';
import { StyleSheet, View } from 'react-native';
import { Feather } from '@expo/vector-icons';
import { useRouter } from 'expo-router';
import { AppHeader } from '../../src/design/AppHeader';
import { Body, Button, Card, Divider, Heading, Kicker, Num, Screen } from '../../src/design/ui';
import { Checkbox, Tag } from '../../src/design/widgets';
import { colors, ink } from '../../src/design/theme';

const BALANCE_ROWS = [
  { id: 'a', text: 'Sam owes you', amount: '$48.00', color: colors.accentRamp[700] },
  { id: 'b', text: 'Jules owes you', amount: '$52.00', color: colors.accentRamp[700] },
  { id: 'c', text: 'You owe Ravi', amount: '−$24.00', color: ink(0.5) },
];

const INITIAL_CHORES = [
  { id: '1', name: 'Take out the bins', who: 'Ravi · tonight', done: false, status: 'Due' },
  { id: '2', name: 'Wipe the counters', who: 'You · today', done: false, status: 'Your turn' },
];

export default function HomeScreen() {
  const router = useRouter();
  const [chores, setChores] = useState(INITIAL_CHORES);
  const toggle = (id: string) => setChores((cs) => cs.map((c) => (c.id === id ? { ...c, done: !c.done } : c)));

  return (
    <View style={styles.root}>
      <AppHeader kicker="同居 · Flat 7" title="Home" onBell={() => router.push('/activity')} />
      <Screen>
        <Card onPress={() => router.push('/balances')}>
          <View style={styles.rowBetween}>
            <Kicker>The house owes you</Kicker>
            <Feather name="chevron-right" size={15} color={ink(0.4)} />
          </View>
          <Num size={52} weight="light" style={styles.bigMoney}>
            $124.00
          </Num>
          <Divider style={{ marginVertical: 13 }} />
          <View style={{ gap: 7 }}>
            {BALANCE_ROWS.map((r) => (
              <View key={r.id} style={styles.rowBetween}>
                <Body size={13} color={ink(0.72)}>
                  {r.text}
                </Body>
                <Num size={15} color={r.color}>
                  {r.amount}
                </Num>
              </View>
            ))}
          </View>
        </Card>

        <View style={[styles.rowBetween, { marginTop: 26, marginBottom: 10, alignItems: 'baseline' }]}>
          <Heading size={19}>Today in the house</Heading>
          <Kicker color={ink(0.45)}>Thu 31</Kicker>
        </View>
        <View style={styles.choreList}>
          {chores.map((ch) => (
            <View key={ch.id} style={styles.choreRow}>
              <Checkbox done={ch.done} onPress={() => toggle(ch.id)} />
              <View style={{ flex: 1, minWidth: 0 }}>
                <Body size={14.5} style={ch.done ? styles.struck : undefined}>
                  {ch.name}
                </Body>
                <Body size={11.5} color={ink(0.52)} style={{ marginTop: 1 }}>
                  {ch.who}
                </Body>
              </View>
              <Tag label={ch.done ? 'Done' : ch.status} color={ch.done ? ink(0.4) : colors.accentRamp[700]} />
            </View>
          ))}
        </View>

        <View style={styles.twoCards}>
          <Card onPress={() => router.push('/meals')} style={styles.miniCard}>
            <Kicker color={colors.accentRamp[700]}>Tonight</Kicker>
            <Heading size={20} style={{ marginTop: 8 }}>
              Miso salmon
            </Heading>
            <Body size={11.5} color={ink(0.52)} style={{ marginTop: 'auto' }}>
              Ravi cooks
            </Body>
          </Card>
          <Card onPress={() => router.push('/shopping')} style={styles.miniCard}>
            <Kicker color={colors.accentRamp[700]}>List</Kicker>
            <Heading size={20} style={{ marginTop: 8 }}>
              6 items
            </Heading>
            <Body size={11.5} color={ink(0.52)} style={{ marginTop: 'auto' }}>
              Milk, eggs, rice…
            </Body>
          </Card>
        </View>

        <Button
          label="Add an expense"
          block
          onPress={() => router.push('/add')}
          left={<Feather name="plus" size={16} color={colors.accent} />}
          style={{ marginTop: 24 }}
        />
      </Screen>
    </View>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: colors.bg },
  rowBetween: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
  bigMoney: { marginTop: 8, letterSpacing: -1 },
  choreList: { borderTopWidth: 1, borderTopColor: colors.divider },
  choreRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    paddingVertical: 13,
    borderBottomWidth: 1,
    borderBottomColor: colors.divider,
  },
  struck: { textDecorationLine: 'line-through', color: ink(0.45) },
  twoCards: { flexDirection: 'row', gap: 14, marginTop: 24 },
  miniCard: { flex: 1, minHeight: 132 },
});
