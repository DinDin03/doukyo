import { Stack } from 'expo-router';

// Create-or-join flow — shown once someone's signed in but belongs to no household.
export default function HouseholdLayout() {
  return <Stack screenOptions={{ headerShown: false, animation: 'fade' }} />;
}
