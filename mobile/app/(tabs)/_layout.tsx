import { Tabs } from 'expo-router';
import { Feather } from '@expo/vector-icons';
import { colors, fonts, ink } from '../../src/design/theme';

// The bottom tab bar, styled to the Classical system: quiet ground, a hairline
// top rule, the brass accent for the active tab, Lora labels. Headers are hidden
// here because each screen draws its own editorial header.
export default function TabsLayout() {
  return (
    <Tabs
      screenOptions={{
        headerShown: false,
        tabBarActiveTintColor: colors.accent,
        tabBarInactiveTintColor: ink(0.42),
        tabBarStyle: {
          backgroundColor: colors.bg,
          borderTopColor: colors.divider,
          borderTopWidth: 1,
          height: 86,
          paddingTop: 8,
          paddingBottom: 24,
        },
        tabBarLabelStyle: { fontFamily: fonts.body, fontSize: 10, letterSpacing: 0.3 },
      }}
    >
      <Tabs.Screen
        name="index"
        options={{ title: 'Home', tabBarIcon: ({ color }) => <Feather name="home" color={color} size={19} /> }}
      />
      <Tabs.Screen
        name="expenses"
        options={{ title: 'Expenses', tabBarIcon: ({ color }) => <Feather name="dollar-sign" color={color} size={19} /> }}
      />
      <Tabs.Screen
        name="chores"
        options={{ title: 'Chores', tabBarIcon: ({ color }) => <Feather name="check-square" color={color} size={19} /> }}
      />
      <Tabs.Screen
        name="shopping"
        options={{ title: 'Shopping', tabBarIcon: ({ color }) => <Feather name="shopping-cart" color={color} size={19} /> }}
      />
      <Tabs.Screen
        name="meals"
        options={{ title: 'Meals', tabBarIcon: ({ color }) => <Feather name="book-open" color={color} size={19} /> }}
      />
      <Tabs.Screen
        name="more"
        options={{ title: 'More', tabBarIcon: ({ color }) => <Feather name="more-horizontal" color={color} size={19} /> }}
      />
    </Tabs>
  );
}
