import { StatusBar } from 'expo-status-bar';
import { StyleSheet, Text, View } from 'react-native';

// The root component of the app. Everything you see on screen is rendered from
// here. For now it's a static welcome screen — no data, no networking yet.
// Next milestone: we bring in Apollo Client and replace the hint below with a
// value fetched live from the GraphQL backend.
export default function App() {
  return (
    <View style={styles.container}>
      <Text style={styles.title}>Doukyo</Text>
      <Text style={styles.subtitle}>Sharehouse Companion</Text>
      <Text style={styles.hint}>Phase 0 — the app is alive 🎉</Text>
      <StatusBar style="light" />
    </View>
  );
}

// Styles are plain JavaScript objects (React Native uses Flexbox for layout).
const styles = StyleSheet.create({
  container: {
    flex: 1, // fill the whole screen
    backgroundColor: '#0f172a', // slate-900
    alignItems: 'center', // center horizontally
    justifyContent: 'center', // center vertically
    padding: 24,
  },
  title: {
    fontSize: 44,
    fontWeight: '800',
    color: '#f8fafc',
    letterSpacing: 0.5,
  },
  subtitle: {
    fontSize: 16,
    color: '#94a3b8',
    marginTop: 6,
  },
  hint: {
    fontSize: 14,
    color: '#38bdf8',
    marginTop: 28,
  },
});
