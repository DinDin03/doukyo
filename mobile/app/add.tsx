import { useState } from 'react';
import { ActivityIndicator, Pressable, StyleSheet, View } from 'react-native';
import { useRouter } from 'expo-router';
import { AppHeader } from '../src/design/AppHeader';
import { Body, Button, Divider, Field, Kicker, Num, Screen } from '../src/design/ui';
import { Checkbox, Chip, Segmented, Stepper } from '../src/design/widgets';
import { colors, ink, radius } from '../src/design/theme';
import { useAuth } from '../src/auth/AuthContext';
import { useHousehold } from '../src/household/HouseholdContext';
import { ExpenseCategory, formatCents, SplitMethod, useExpenses } from '../src/expense/useExpenses';

const CATEGORIES: ExpenseCategory[] = ['GROCERIES', 'BILLS', 'DINING', 'HOUSEHOLD', 'OTHER'];
const KEYS = ['1', '2', '3', '4', '5', '6', '7', '8', '9', '.', '0', 'del'];
const title = (c: string) => c.charAt(0) + c.slice(1).toLowerCase();

function errorMessage(e: unknown): string {
  const err = e as { errors?: { message: string }[]; graphQLErrors?: { message: string }[]; message?: string };
  return err?.errors?.[0]?.message ?? err?.graphQLErrors?.[0]?.message ?? err?.message ?? 'Could not save the expense';
}

export default function AddExpenseScreen() {
  const router = useRouter();
  const { user } = useAuth();
  const { activeHousehold } = useHousehold();
  const { createExpense } = useExpenses(activeHousehold?.id);

  const members = activeHousehold?.members ?? [];
  const [cents, setCents] = useState(0);
  const [description, setDescription] = useState('');
  const [category, setCategory] = useState<ExpenseCategory>('GROCERIES');
  const [paidByIdState, setPaidByIdState] = useState('');
  const paidById = paidByIdState || user?.id || members[0]?.id || '';
  const setPaidById = setPaidByIdState;
  const [method, setMethod] = useState<Extract<SplitMethod, 'EVENLY' | 'WEIGHTS'>>('EVENLY');
  // Absent means default (included, weight 1). Seeding these from `members` would
  // break whenever the household loads after the first render.
  const [included, setIncluded] = useState<Record<string, boolean>>({});
  const [weights, setWeights] = useState<Record<string, number>>({});
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (!activeHousehold) return null;

  const participants = members.filter((m) => included[m.id] ?? true);
  const canSave = cents > 0 && description.trim().length > 0 && participants.length > 0 && !!paidById && !busy;

  const press = (k: string) => {
    if (k === 'del') setCents((c) => Math.floor(c / 10));
    else if (k !== '.') setCents((c) => Math.min(c * 10 + Number(k), 99_999_999));
  };

  const submit = async () => {
    setBusy(true);
    setError(null);
    try {
      await createExpense({
        paidById,
        description: description.trim(),
        amountCents: cents,
        category,
        method,
        participants: participants.map((m) => ({
          userId: m.id,
          ...(method === 'WEIGHTS' ? { value: weights[m.id] ?? 1 } : {}),
        })),
      });
      router.back();
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  };

  return (
    <View style={styles.root}>
      <AppHeader kicker="Expenses" title="Add expense" onBack={() => router.back()} backLabel="Back" />
      <Screen>
        <View style={styles.card}>
          <Kicker color={ink(0.5)}>Amount</Kicker>
          <Num size={46} weight="light" style={{ marginTop: 4, letterSpacing: -1 }}>
            {formatCents(cents)}
          </Num>
        </View>

        <View style={styles.pad}>
          {KEYS.map((k) => (
            <Pressable key={k} onPress={() => press(k)} style={styles.key} disabled={k === '.'}>
              <Num size={22} color={k === '.' ? ink(0.25) : colors.text}>
                {k === 'del' ? '⌫' : k}
              </Num>
            </Pressable>
          ))}
        </View>

        <Field label="Description" value={description} onChangeText={setDescription} placeholder="Groceries" />

        <Kicker color={ink(0.5)} style={styles.label}>
          Category
        </Kicker>
        <View style={styles.chips}>
          {CATEGORIES.map((c) => (
            <Chip key={c} label={title(c)} active={c === category} onPress={() => setCategory(c)} />
          ))}
        </View>

        <Kicker color={ink(0.5)} style={styles.label}>
          Paid by
        </Kicker>
        <View style={styles.chips}>
          {members.map((m) => (
            <Chip
              key={m.id}
              label={m.id === user?.id ? 'You' : m.name}
              active={m.id === paidById}
              onPress={() => setPaidById(m.id)}
            />
          ))}
        </View>

        <Divider style={{ marginVertical: 18 }} />

        <Segmented
          options={[
            { value: 'EVENLY', label: 'Split evenly' },
            { value: 'WEIGHTS', label: 'By shares' },
          ]}
          value={method}
          onChange={setMethod}
        />

        <Kicker color={ink(0.5)} style={styles.label}>
          Who&apos;s sharing
        </Kicker>
        {members.map((m) => (
          <View key={m.id} style={styles.memberRow}>
            <Checkbox
              done={included[m.id] ?? true}
              onPress={() => setIncluded((s) => ({ ...s, [m.id]: !(s[m.id] ?? true) }))}
            />
            <Body size={14.5} style={{ flex: 1 }}>
              {m.id === user?.id ? `${m.name} (you)` : m.name}
            </Body>
            {method === 'WEIGHTS' && (included[m.id] ?? true) ? (
              <Stepper
                value={<Num size={14}>{weights[m.id] ?? 1}</Num>}
                onDec={() => setWeights((w) => ({ ...w, [m.id]: Math.max(1, (w[m.id] ?? 1) - 1) }))}
                onInc={() => setWeights((w) => ({ ...w, [m.id]: (w[m.id] ?? 1) + 1 }))}
              />
            ) : null}
          </View>
        ))}

        {/* Approximate on purpose. The server runs the largest-remainder split and
            is the only authority on the cent — duplicating that maths here would
            eventually disagree with it. */}
        {cents > 0 && participants.length > 0 && method === 'EVENLY' ? (
          <Body size={12} color={ink(0.5)} style={{ marginTop: 10 }}>
            ≈ {formatCents(Math.floor(cents / participants.length))} each · exact shares are
            worked out when you save
          </Body>
        ) : null}

        {error ? (
          <Body size={13} color={colors.accentRamp[700]} style={{ marginTop: 14 }}>
            {error}
          </Body>
        ) : null}

        <Button
          label={busy ? 'Saving…' : 'Save expense'}
          block
          disabled={!canSave}
          onPress={submit}
          style={{ marginTop: 20 }}
        />
        {busy ? <ActivityIndicator color={colors.accent} style={{ marginTop: 10 }} /> : null}
      </Screen>
    </View>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: colors.bg },
  card: {
    borderWidth: 1,
    borderColor: colors.divider,
    borderRadius: radius.md,
    padding: 16,
    alignItems: 'center',
  },
  pad: { flexDirection: 'row', flexWrap: 'wrap', marginVertical: 14 },
  key: { width: '33.33%', alignItems: 'center', paddingVertical: 13 },
  label: { marginTop: 18, marginBottom: 8 },
  chips: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },
  memberRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    paddingVertical: 11,
    borderBottomWidth: 1,
    borderBottomColor: colors.divider,
  },
});
