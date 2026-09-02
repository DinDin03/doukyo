import { useState } from 'react';
import { StyleSheet, View } from 'react-native';
import { Feather } from '@expo/vector-icons';
import { useRouter } from 'expo-router';
import { AppHeader } from '../../src/design/AppHeader';
import { Body, Button, Kicker, Num, Row, Screen } from '../../src/design/ui';
import { Avatar, Segmented } from '../../src/design/widgets';
import { colors, ink } from '../../src/design/theme';
import { useHousehold } from '../../src/household/HouseholdContext';

const GROUPS = [
  {
    label: 'This week',
    items: [
      { id: '1', initial: 'R', desc: 'Groceries — Woolworths', meta: 'Ravi paid · Fri', amount: '$84.20', share: 'You owe $21.05' },
      { id: '2', initial: 'S', desc: 'Internet', meta: 'Sam paid · Wed', amount: '$79.00', share: 'You owe $19.75' },
    ],
  },
  {
    label: 'Last week',
    items: [
      { id: '3', initial: 'Y', desc: 'Dinner — Thai', meta: 'You paid · Sun', amount: '$96.00', share: 'You are owed $72.00' },
      { id: '4', initial: 'J', desc: 'Cleaning supplies', meta: 'Jules paid · Sat', amount: '$32.40', share: 'You owe $8.10' },
    ],
  },
];

export default function ExpensesScreen() {
  const router = useRouter();
  const { activeHousehold } = useHousehold();
  const [tab, setTab] = useState<'all' | 'balances'>('all');

  return (
    <View style={styles.root}>
      <AppHeader kicker={`同居 · ${activeHousehold?.name ?? ''}`} title="Expenses" onBell={() => router.push('/activity')} />
      <Screen>
        <Segmented
          options={[
            { value: 'all', label: 'All expenses' },
            { value: 'balances', label: 'Balances' },
          ]}
          value={tab}
          onChange={(v) => (v === 'balances' ? router.push('/balances') : setTab(v))}
        />

        {GROUPS.map((g) => (
          <View key={g.label} style={{ marginTop: 18 }}>
            <Kicker color={ink(0.45)} style={styles.groupLabel}>
              {g.label}
            </Kicker>
            {g.items.map((e) => {
              const owed = e.share.startsWith('You are owed');
              return (
                <Row key={e.id} onPress={() => {}} style={styles.expRow}>
                  <Avatar initial={e.initial} />
                  <View style={{ flex: 1, minWidth: 0 }}>
                    <Body size={14.5} numberOfLines={1}>
                      {e.desc}
                    </Body>
                    <Body size={11.5} color={ink(0.52)} style={{ marginTop: 1 }}>
                      {e.meta}
                    </Body>
                  </View>
                  <View style={{ alignItems: 'flex-end' }}>
                    <Num size={16.5}>{e.amount}</Num>
                    <Kicker color={owed ? colors.accentRamp[700] : ink(0.5)} style={{ marginTop: 2 }}>
                      {e.share}
                    </Kicker>
                  </View>
                </Row>
              );
            })}
          </View>
        ))}

        <Button
          label="Add an expense"
          block
          onPress={() => router.push('/add')}
          left={<Feather name="plus" size={16} color={colors.accent} />}
          style={{ marginTop: 22 }}
        />
      </Screen>
    </View>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: colors.bg },
  groupLabel: { paddingBottom: 7, borderBottomWidth: 1, borderBottomColor: colors.divider },
  expRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    borderBottomWidth: 1,
    borderBottomColor: colors.divider,
  },
});
