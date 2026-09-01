import { useState } from 'react';
import { StyleSheet, View } from 'react-native';
import { useRouter } from 'expo-router';
import { AppHeader } from '../src/design/AppHeader';
import { Body, Button, Kicker, Num, Screen } from '../src/design/ui';
import { Stepper } from '../src/design/widgets';
import { colors, ink, radius } from '../src/design/theme';

const INGREDIENTS = [
  { qty: '2 fillets', name: 'Salmon' },
  { qty: '2 tbsp', name: 'White miso' },
  { qty: '1 tbsp', name: 'Mirin' },
  { qty: '1 tbsp', name: 'Soy sauce' },
  { qty: '2 cups', name: 'Steamed rice' },
  { qty: '1 bunch', name: 'Bok choy' },
];

const STEPS = [
  'Whisk the miso, mirin and soy into a smooth glaze.',
  'Coat the salmon and let it rest for ten minutes.',
  'Grill skin-side down until the glaze caramelises at the edges.',
  'Steam the bok choy and serve everything over warm rice.',
];

export default function RecipeScreen() {
  const router = useRouter();
  const [servings, setServings] = useState(4);

  return (
    <View style={styles.root}>
      <AppHeader kicker="Cookbook" title="Miso salmon" onBack={() => router.back()} backLabel="Meals" />
      <Screen>
        <View style={styles.servings}>
          <View>
            <Kicker color={ink(0.5)}>Servings</Kicker>
            <Num size={22} style={{ marginTop: 2 }}>
              {servings}
            </Num>
          </View>
          <Stepper onDec={() => setServings((s) => Math.max(1, s - 1))} onInc={() => setServings((s) => s + 1)} />
        </View>

        <Kicker color={ink(0.45)} style={styles.section}>
          Ingredients
        </Kicker>
        {INGREDIENTS.map((i) => (
          <View key={i.name} style={styles.ingRow}>
            <Num size={13.5} color={colors.accentRamp[700]} style={styles.qty}>
              {i.qty}
            </Num>
            <Body size={14} style={{ flex: 1 }}>
              {i.name}
            </Body>
          </View>
        ))}

        <Kicker color={ink(0.45)} style={styles.section}>
          Method
        </Kicker>
        {STEPS.map((s, i) => (
          <View key={i} style={styles.stepRow}>
            <Num size={19} weight="light" color={colors.accentRamp[400]} style={styles.stepNum}>
              {i + 1}
            </Num>
            <Body size={14} style={{ flex: 1 }}>
              {s}
            </Body>
          </View>
        ))}

        <Button label="Add ingredients to the list" block onPress={() => router.push('/shopping')} style={{ marginTop: 22 }} />
      </Screen>
    </View>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: colors.bg },
  servings: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    borderWidth: 1,
    borderColor: colors.divider,
    borderRadius: radius.md,
    padding: 14,
  },
  section: { marginTop: 22, marginBottom: 6 },
  ingRow: { flexDirection: 'row', alignItems: 'baseline', gap: 10, paddingVertical: 9, borderBottomWidth: 1, borderBottomColor: colors.divider },
  qty: { width: 82 },
  stepRow: { flexDirection: 'row', gap: 14, paddingVertical: 11, borderBottomWidth: 1, borderBottomColor: colors.divider },
  stepNum: { width: 22 },
});
