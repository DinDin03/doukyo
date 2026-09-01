import { Pressable, StyleSheet, View } from 'react-native';
import { Feather } from '@expo/vector-icons';
import { useRouter } from 'expo-router';
import { AppHeader } from '../../src/design/AppHeader';
import { Body, Heading, Kicker, Num, Row, Screen } from '../../src/design/ui';
import { colors, ink } from '../../src/design/theme';

const WEEK = [
  { day: 'MON', date: '28', meal: 'Miso salmon', cook: 'Ravi cooks', today: false },
  { day: 'TUE', date: '29', meal: 'Dal & rice', cook: 'You cook', today: false },
  { day: 'WED', date: '30', meal: 'Pasta puttanesca', cook: 'Sam cooks', today: false },
  { day: 'THU', date: '31', meal: 'Miso salmon', cook: 'Ravi cooks', today: true },
  { day: 'FRI', date: '01', meal: 'Leftovers', cook: 'Fend for yourself', today: false },
  { day: 'SAT', date: '02', meal: 'Roast veg tray', cook: 'Jules cooks', today: false },
  { day: 'SUN', date: '03', meal: 'Not planned', cook: 'Tap to add', today: false },
];

const COOKBOOK = [
  { id: '1', name: 'Miso salmon', meta: '30 min · 4 servings', stars: '★★★★☆' },
  { id: '2', name: 'Dal & rice', meta: '45 min · 4 servings', stars: '★★★★★' },
  { id: '3', name: 'Pasta puttanesca', meta: '25 min · 4 servings', stars: '★★★★☆' },
  { id: '4', name: 'Roast veg tray', meta: '50 min · 4 servings', stars: '★★★☆☆' },
];

export default function MealsScreen() {
  const router = useRouter();
  return (
    <View style={styles.root}>
      <AppHeader kicker="同居 · Flat 7" title="Meals" onBell={() => router.push('/activity')} />
      <Screen>
        <View style={styles.weekHead}>
          <Kicker color={ink(0.45)}>This week</Kicker>
          <Pressable onPress={() => {}} hitSlop={6}>
            <Body size={12.5} color={colors.accentRamp[700]} style={styles.link}>
              Build shopping list
            </Body>
          </Pressable>
        </View>

        {WEEK.map((d) => (
          <Row key={d.day} onPress={() => router.push('/recipe')} style={styles.dayRow}>
            <View style={styles.dayCol}>
              <Kicker color={d.today ? colors.accentRamp[700] : ink(0.4)}>{d.day}</Kicker>
              <Num size={17} color={d.today ? colors.accentRamp[700] : ink(0.55)} style={{ marginTop: 1 }}>
                {d.date}
              </Num>
            </View>
            <View style={styles.daySep} />
            <View style={{ flex: 1, minWidth: 0 }}>
              <Heading size={17} color={d.meal === 'Not planned' ? ink(0.4) : colors.text}>
                {d.meal}
              </Heading>
              <Body size={11.5} color={ink(0.5)} style={{ marginTop: 1 }}>
                {d.cook}
              </Body>
            </View>
            <Feather name="chevron-right" size={15} color={ink(0.35)} />
          </Row>
        ))}

        <Heading size={19} style={{ marginTop: 24, marginBottom: 2 }}>
          Cookbook
        </Heading>
        {COOKBOOK.map((r) => (
          <Row key={r.id} onPress={() => router.push('/recipe')} style={styles.bookRow}>
            <View style={{ flex: 1, minWidth: 0 }}>
              <Body size={14.5}>{r.name}</Body>
              <Body size={11.5} color={ink(0.52)} style={{ marginTop: 1 }}>
                {r.meta}
              </Body>
            </View>
            <Body size={12} color={colors.accentRamp[700]}>
              {r.stars}
            </Body>
          </Row>
        ))}
      </Screen>
    </View>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: colors.bg },
  weekHead: {
    flexDirection: 'row',
    alignItems: 'baseline',
    justifyContent: 'space-between',
    paddingBottom: 8,
    borderBottomWidth: 1,
    borderBottomColor: colors.divider,
  },
  link: { fontFamily: 'CormorantGaramond_600SemiBold', textDecorationLine: 'underline' },
  dayRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 14,
    borderBottomWidth: 1,
    borderBottomColor: colors.divider,
  },
  dayCol: { width: 38, alignItems: 'center' },
  daySep: { width: 1, alignSelf: 'stretch', backgroundColor: colors.divider },
  bookRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    borderBottomWidth: 1,
    borderBottomColor: colors.divider,
  },
});
