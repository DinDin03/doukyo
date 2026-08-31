import { useState } from 'react';
import { StyleSheet, View } from 'react-native';
import { useRouter } from 'expo-router';
import { AppHeader } from '../../src/design/AppHeader';
import { Body, Button, Kicker, Num, Screen } from '../../src/design/ui';
import { Checkbox, Segmented, Tag } from '../../src/design/widgets';
import { colors, ink, radius } from '../../src/design/theme';

const GROUPS = [
  {
    label: 'Today',
    items: [
      { id: '1', name: 'Take out the bins', who: 'Ravi', cadence: 'weekly', status: 'Due' },
      { id: '2', name: 'Wipe the counters', who: 'You', cadence: 'daily', status: 'Your turn' },
    ],
  },
  {
    label: 'This week',
    items: [
      { id: '3', name: 'Bathroom clean', who: 'Sam', cadence: 'weekly', status: 'Fri' },
      { id: '4', name: 'Vacuum the lounge', who: 'Jules', cadence: 'weekly', status: 'Sun' },
    ],
  },
];

const FAIRNESS = [
  { name: 'Ravi', count: 12, pct: '100%' },
  { name: 'You', count: 11, pct: '88%' },
  { name: 'Sam', count: 10, pct: '80%' },
  { name: 'Jules', count: 9, pct: '72%' },
];

export default function ChoresScreen() {
  const router = useRouter();
  const [tab, setTab] = useState<'roster' | 'fairness'>('roster');
  const [done, setDone] = useState<Record<string, boolean>>({});

  return (
    <View style={styles.root}>
      <AppHeader kicker="同居 · Flat 7" title="Chores" onBell={() => router.push('/activity')} />
      <Screen>
        <Segmented
          options={[
            { value: 'roster', label: 'Roster' },
            { value: 'fairness', label: 'Fairness' },
          ]}
          value={tab}
          onChange={setTab}
        />

        {tab === 'roster' ? (
          <>
            {GROUPS.map((g) => (
              <View key={g.label} style={{ marginTop: 18 }}>
                <Kicker color={ink(0.45)} style={styles.groupLabel}>
                  {g.label}
                </Kicker>
                {g.items.map((c) => (
                  <View key={c.id} style={styles.row}>
                    <Checkbox size={26} done={!!done[c.id]} onPress={() => setDone((d) => ({ ...d, [c.id]: !d[c.id] }))} />
                    <View style={{ flex: 1, minWidth: 0 }}>
                      <Body size={14.5} style={done[c.id] ? styles.struck : undefined}>
                        {c.name}
                      </Body>
                      <Body size={11.5} color={ink(0.52)} style={{ marginTop: 1 }}>
                        {c.who} · {c.cadence}
                      </Body>
                    </View>
                    <Tag label={done[c.id] ? 'Done' : c.status} color={done[c.id] ? ink(0.4) : colors.accentRamp[700]} />
                  </View>
                ))}
              </View>
            ))}
            <Button label="New chore — one-off or rotating" variant="secondary" block onPress={() => {}} style={{ marginTop: 22 }} />
          </>
        ) : (
          <View style={{ marginTop: 18 }}>
            <Body size={13} color={ink(0.62)} style={{ marginBottom: 16 }}>
              Chores done in the last four weeks. The rotation evens itself out — this is here to be seen, not to keep score.
            </Body>
            {FAIRNESS.map((f) => (
              <View key={f.name} style={styles.fairRow}>
                <View style={styles.rowBetween}>
                  <Body size={14.5}>{f.name}</Body>
                  <Num size={14} color={ink(0.62)}>
                    {f.count}
                  </Num>
                </View>
                <View style={styles.barTrack}>
                  <View style={[styles.barFill, { width: f.pct as `${number}%` }]} />
                </View>
              </View>
            ))}
          </View>
        )}
      </Screen>
    </View>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: colors.bg },
  groupLabel: { paddingBottom: 7, borderBottomWidth: 1, borderBottomColor: colors.divider },
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    paddingVertical: 14,
    borderBottomWidth: 1,
    borderBottomColor: colors.divider,
  },
  struck: { textDecorationLine: 'line-through', color: ink(0.45) },
  rowBetween: { flexDirection: 'row', alignItems: 'baseline', justifyContent: 'space-between' },
  fairRow: { paddingVertical: 13, borderBottomWidth: 1, borderBottomColor: colors.divider },
  barTrack: {
    height: 8,
    marginTop: 9,
    borderWidth: 1,
    borderColor: colors.divider,
    borderRadius: 2,
    overflow: 'hidden',
  },
  barFill: { position: 'absolute', top: 0, bottom: 0, left: 0, backgroundColor: colors.accentRamp[200], borderRightWidth: 1, borderRightColor: colors.accent },
});
