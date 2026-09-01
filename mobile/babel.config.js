// Babel config for the Expo app. `babel-preset-expo` includes everything Expo
// (and Expo Router) needs to transform the code. Required once expo-router is used.
module.exports = function (api) {
  api.cache(true);
  return {
    presets: ['babel-preset-expo'],
  };
};
