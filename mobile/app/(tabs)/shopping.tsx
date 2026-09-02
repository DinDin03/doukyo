import { useState } from 'react';
import { Pressable, StyleSheet, TextInput, View } from 'react-native';
import { Feather } from '@expo/vector-icons';
import { useRouter } from 'expo-router';
import { AppHeader } from '../../src/design/AppHeader';
import { Body, Button, Heading, Kicker, Screen } from '../../src/design/ui';
import { Checkbox } from '../../src/design/widgets';
import { colors, ink, radius } from '../../src/design/theme';
import { useHousehold } from '../../src/household/HouseholdContext';

type Item = { id: string; name: string; by: string; done: boolean; aisle: string };
const INITIAL: Item[] = [
  { id: '1', name: 'Spinach', by: 'Ravi', done: false, aisle: 'Produce' },
  { id: '2', name: 'Lemons', by: 'You', done: false, aisle: 'Produce' },
  { id: '3', name: 'Milk', by: 'Sam', done: true, aisle: 'Dairy' },
  { id: '4', name: 'Butter', by: 'Jules', done: false, aisle: 'Dairy' },
  { id: '5', name: 'Arborio rice', by: 'You', done: false, aisle: 'Pantry' },
  { id: '6', name: 'Olive oil', by: 'Ravi', done: false, aisle: 'Pantry' },
];
const AISLES = ['Produce', 'Dairy', 'Pantry', 'Added'];

export default function ShoppingScreen() {
  const router = useRouter();
  const { activeHousehold } = useHousehold();
  const [items, setItems] = useState(INITIAL);
  const [draft, setDraft] = useState('');

  const add = () => {
    if (!draft.trim()) return;
    setItems((xs) => [...xs, { id: String(Date.now()), name: draft.trim(), by: 'You', done: false, aisle: 'Added' }]);
    setDraft('');
  };
  const toggle = (id: string) => setItems((xs) => xs.map((x) => (x.id === id ? { ...x, done: !x.done } : x)));

  return (
    <View style={styles.root}>
      <AppHeader kicker={`同居 · ${activeHousehold?.name ?? ''}`} title="Shopping" onBell={() => router.push('/activity')} />
      <Screen>
        <View style={styles.addBar}>
          <TextInput
            value={draft}
            onChangeText={setDraft}
            onSubmitEditing={add}
            placeholder="Add an item…"
            placeholderTextColor={ink(0.35)}
            style={styles.addInput}
          />
          <Pressable onPress={add} style={styles.addBtn} hitSlop={6}>
            <Feather name="plus" size={18} color={colors.accent} />
          </Pressable>
        </View>

        {AISLES.map((aisle) => {
          const list = items.filter((i) => i.aisle === aisle);
          if (!list.length) return null;
          return (
            <View key={aisle} style={{ marginTop: 20 }}>
              <Kicker color={ink(0.45)} style={styles.aisle}>
                {aisle}
              </Kicker>
              {list.map((it) => (
                <Pressable key={it.id} onPress={() => toggle(it.id)} style={styles.row}>
                  <Checkbox shape="square" size={22} done={it.done} onPress={() => toggle(it.id)} />
                  <Body size={14.5} style={[{ flex: 1 }, it.done ? styles.struck : undefined]}>
                    {it.name}
                  </Body>
                  <Kicker color={ink(0.42)}>{it.by}</Kicker>
                </Pressable>
              ))}
            </View>
          );
        })}

        <View style={styles.splitCard}>
          <Heading size={18}>Bought it all?</Heading>
          <Body size={12.5} color={ink(0.6)} style={{ marginTop: 4 }}>
            Turn this run into a shared expense and it splits evenly, four ways.
          </Body>
          <Button label="Split this run" block onPress={() => router.push('/add')} style={{ marginTop: 12 }} />
        </View>
      </Screen>
    </View>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: colors.bg },
  addBar: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    borderWidth: 1,
    borderColor: colors.divider,
    borderRadius: radius.md,
    paddingLeft: 14,
    paddingRight: 4,
    paddingVertical: 4,
  },
  addInput: { flex: 1, minHeight: 44, fontFamily: 'Lora_400Regular', fontSize: 15, color: colors.text },
  addBtn: {
    width: 40,
    height: 40,
    borderWidth: 1,
    borderColor: colors.accent,
    borderRadius: radius.sm,
    alignItems: 'center',
    justifyContent: 'center',
  },
  aisle: { paddingBottom: 7, borderBottomWidth: 1, borderBottomColor: colors.divider },
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    paddingVertical: 12,
    borderBottomWidth: 1,
    borderBottomColor: colors.divider,
  },
  struck: { textDecorationLine: 'line-through', color: ink(0.4) },
  splitCard: { borderWidth: 1, borderColor: colors.divider, borderRadius: radius.md, padding: 16, marginTop: 22 },
});
