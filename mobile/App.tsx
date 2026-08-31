import { StatusBar } from 'expo-status-bar';
import { ActivityIndicator, StyleSheet, Text, View } from 'react-native';
import { gql } from '@apollo/client';
import { ApolloProvider, useQuery } from '@apollo/client/react';
import { apolloClient, API_URL } from './src/apollo';

// The GraphQL query. `gql` parses the query string into the format Apollo expects.
// This is the exact same `{ ping }` you ran in GraphiQL — now issued from the phone.
const PING = gql`
  query Ping {
    ping
    greeting
    hello
  }
`;

// A component whose entire job is to fetch and display `ping`.
// useQuery runs the query and re-renders as its state changes.
function PingStatus() {
  const { data, loading, error } = useQuery<{ ping: string; greeting: string; hello: string }>(PING);

  if (loading) return <ActivityIndicator color="#38bdf8" style={{ marginTop: 28 }} />;

  if (error) {
    // Shown when the phone can't reach the backend — the most common issue.
    return (
      <View style={styles.errorBox}>
        <Text style={styles.errorTitle}>Can’t reach the backend</Text>
        <Text style={styles.errorDetail}>{error.message}</Text>
        <Text style={styles.errorDetail}>URL: {API_URL}</Text>
      </View>
    );
  }

  return (
      <View style={{ marginTop: 28, alignItems: 'center' }}>
        <Text style={styles.hint}>backend says: {data?.ping}</Text>
        <Text style={styles.hint}>greeting: {data?.greeting}</Text>
          <Text style={styles.hint}>hello: {data?.hello}</Text>
      </View>
  );
}

export default function App() {
  return (
    // ApolloProvider makes the client available to every component below it,
    // so useQuery (in PingStatus) can find it.
    <ApolloProvider client={apolloClient}>
      <View style={styles.container}>
        <Text style={styles.title}>Doukyo</Text>
        <Text style={styles.subtitle}>Sharehouse Companion</Text>
        <PingStatus />
        <StatusBar style="light" />
      </View>
    </ApolloProvider>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#0f172a',
    alignItems: 'center',
    justifyContent: 'center',
    padding: 24,
  },
  title: { fontSize: 44, fontWeight: '800', color: '#f8fafc', letterSpacing: 0.5 },
  subtitle: { fontSize: 16, color: '#94a3b8', marginTop: 6 },
  hint: { fontSize: 16, color: '#38bdf8', marginTop: 28 },
  errorBox: { marginTop: 28, alignItems: 'center' },
  errorTitle: { fontSize: 16, color: '#f87171', fontWeight: '700' },
  errorDetail: { fontSize: 12, color: '#94a3b8', marginTop: 4, textAlign: 'center' },
});
