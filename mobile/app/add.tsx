import { useState } from 'react';
import { Pressable, StyleSheet, View } from 'react-native';
import { Feather } from '@expo/vector-icons';
import { useRouter } from 'expo-router';
import { AppHeader } from '../src/design/AppHeader';
import { Body, Button, Divider, Field, Kicker, Num, Screen } from '../src/design/ui';
import { Chip, Segmented, Stepper } from '../src/design/widgets';
import { colors, ink, radius } from '../src/design/theme';

type Mode = 'even' | 'exact' | 'percent' | 'shares';
const MEMBERS = [
  { id: 'you', name: 'You' },
  { id: 'sam', name: 'Sam' },
  { id: 'jules', name: 'Jules' },
  { id: 'ravi', name: 'Ravi' },
];
const CATEGORIES = ['Groceries', 'Bills', 'Dining', 'Household', 'Other'];
const KEYS = ['1', '2', '3', '4', '5', '6', '7', '8', '9', '.', '0', 'del'];
const fmt = (cents: number) => '$' + (cents / 100).toFixed(2);

function split(cents: number, mode: Mode, units: Record<string, number>) {
  const ids = MEMBERS.map((m) => m.id);
  if (mode === 'shares') {
    const total = ids.reduce((s, id) => s + units[id], 0) || 1;
    const alloc = ids.map((id) => Math.floor((cents * units[id]) / total));
    let rem = cents - alloc.reduce((a, b) => a + b, 0);
    for (let i = 0; rem > 0; i++, rem--) alloc[i % ids.length]++;
    return Object.fromEntries(ids.map((id, i) => [id, alloc[i]]));
  }
  const base = Math.floor(cents / ids.length);
  const rem = cents - base * ids.length;
  return Object.fromEntries(ids.map((id, i) => [id, base + (i < rem ? 1 : 0)]));
}

export default function AddExpenseScreen() {
  const router = useRouter();
  const [cents, setCents] = useState(0);
  const [desc, setDesc] = useState('');
  const [cat, setCat] = useState('Groceries');
  const [payer, setPayer] = useState('you');
  const [mode, setMode] = useState<Mode>('even');
  const [units, setUnits] = useState<Record<string, number>>({ you: 1, sam: 1, jules: 1, ravi: 1 });

  const press = (k: string) => {
    if (k === 'del') setCents((c) => Math.floor(c / 10));
    else if (k === '.') return;
    else setCents((c) => Math.min(c * 10 + Number(k), 99_999_999));
  };
  const bump = (id: string, d: number) => setUnits((u) => ({ ...u, [id]: Math.max(0, u[id] + d) }));

  const amounts = split(cents, mode, units);
  const assigned = Object.values(amounts).reduce((a, b) => a + b, 0);
  const remaining = cents - assigned;

  return (
    <View style={styles.root}>
      <AppHeader kicker="Expenses" title="Add expense" onBack={() => router.back()} backLabel="Back" />
      <Screen>
        <View style={styles.card}>
          <Kicker color={ink(0.5)}>Amount</Kicker>
          <Num size={46} weight="light" color={cents ? colors.text : ink(0.35)} style={{ marginTop: 4 }}>
            {fmt(cents)}
          </Num>
          <Divider style={{ marginVertical: 13 }} />
          <Field label="What was it for" value={desc} onChangeText={setDesc} placeholder="Groceries, internet, wine…" />
          <View style={styles.chips}>
            {CATEGORIES.map((c) => (
              <Chip key={c} label={c} active={c === cat} onPress={() => setCat(c)} />
            ))}
          </View>
        </View>

        <Kicker color={ink(0.45)} style={styles.section}>
          Paid by
        </Kicker>
        <View style={styles.payerRow}>
          {MEMBERS.map((m) => {
            const active = m.id === payer;
            return (
              <Pressable
                key={m.id}
                onPress={() => setPayer(m.id)}
                style={[styles.payer, { borderColor: active ? colors.accent : colors.divider }]}
              >
                <Body size={13.5} color={active ? colors.accent : ink(0.65)} style={styles.payerLabel}>
                  {m.name}
                </Body>
              </Pressable>
            );
          })}
        </View>

        <Kicker color={ink(0.45)} style={styles.section}>
          Split
        </Kicker>
        <Segmented
          options={[
            { value: 'even', label: 'Even' },
            { value: 'exact', label: 'Exact' },
            { value: 'percent', label: '%' },
            { value: 'shares', label: 'Shares' },
          ]}
          value={mode}
          onChange={setMode}
        />

        <View style={[styles.card, { marginTop: 12, paddingVertical: 4 }]}>
          {MEMBERS.map((m, i) => (
            <View key={m.id} style={[styles.splitRow, i === MEMBERS.length - 1 && styles.noBorder]}>
              <View style={{ flex: 1 }}>
                <Body size={14}>{m.name}</Body>
                <Body size={11} color={ink(0.5)} style={{ marginTop: 1 }}>
                  {mode === 'shares' ? `${units[m.id]} share${units[m.id] === 1 ? '' : 's'}` : mode === 'percent' ? '25%' : 'Even split'}
                </Body>
              </View>
              {mode === 'shares' ? (
                <Stepper value={units[m.id]} onDec={() => bump(m.id, -1)} onInc={() => bump(m.id, 1)} />
              ) : null}
              <Num size={16} style={styles.splitAmount}>
                {fmt(amounts[m.id])}
              </Num>
            </View>
          ))}
          <View style={styles.remainRow}>
            <Kicker color={remaining === 0 ? ink(0.45) : colors.accentRamp[700]}>
              {remaining === 0 ? 'Splits in full' : 'Remaining'}
            </Kicker>
            <Num size={13} color={remaining === 0 ? ink(0.45) : colors.accentRamp[700]}>
              {fmt(remaining)}
            </Num>
          </View>
        </View>

        <View style={styles.dashedRow}>
          <View style={styles.dashed}>
            <Feather name="camera" size={16} color={ink(0.6)} />
            <Body size={12.5} color={ink(0.6)}>
              Receipt photo
            </Body>
          </View>
          <View style={styles.dashed}>
            <Feather name="repeat" size={16} color={ink(0.6)} />
            <Body size={12.5} color={ink(0.6)}>
              Repeats monthly
            </Body>
          </View>
        </View>

        <Kicker color={ink(0.45)} style={styles.section}>
          Enter amount
        </Kicker>
        <View style={styles.pad}>
          {KEYS.map((k) => (
            <Pressable key={k} onPress={() => press(k)} style={styles.key}>
              {k === 'del' ? (
                <Feather name="delete" size={19} color={colors.text} />
              ) : (
                <Num size={21}>{k}</Num>
              )}
            </Pressable>
          ))}
        </View>

        <Button label="Save expense" block onPress={() => router.back()} style={{ marginTop: 18 }} />
      </Screen>
    </View>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: colors.bg },
  card: { borderWidth: 1, borderColor: colors.divider, borderRadius: radius.md, padding: 16 },
  chips: { flexDirection: 'row', flexWrap: 'wrap', gap: 7, marginTop: 12 },
  section: { marginTop: 22, marginBottom: 8 },
  payerRow: { flexDirection: 'row', gap: 8 },
  payer: { flex: 1, alignItems: 'center', paddingVertical: 10, borderWidth: 1, borderRadius: radius.md },
  payerLabel: { fontFamily: undefined },
  splitRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
    paddingVertical: 12,
    borderBottomWidth: 1,
    borderBottomColor: colors.divider,
  },
  noBorder: { borderBottomWidth: 0 },
  splitAmount: { minWidth: 74, textAlign: 'right' },
  remainRow: { flexDirection: 'row', alignItems: 'baseline', justifyContent: 'space-between', paddingVertical: 11 },
  dashedRow: { flexDirection: 'row', gap: 8, marginTop: 14 },
  dashed: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 7,
    paddingVertical: 12,
    borderWidth: 1,
    borderColor: colors.divider,
    borderStyle: 'dashed',
    borderRadius: radius.md,
  },
  pad: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },
  key: {
    width: '31.5%',
    height: 52,
    alignItems: 'center',
    justifyContent: 'center',
    borderWidth: 1,
    borderColor: colors.divider,
    borderRadius: radius.md,
  },
});
