// SPDX-License-Identifier: GPL-3.0-or-later
const preview = document.querySelector('#garden-preview');
const caption = document.querySelector('#preview-caption');
const buttons = document.querySelectorAll('[data-theme]');
const labels = { light: 'light', dark: 'dark', amoled: 'true-black AMOLED' };
buttons.forEach((button) => button.addEventListener('click', () => {
  const theme = button.dataset.theme;
  if (!Object.hasOwn(labels, theme)) return;
  preview.src = `screenshots/world_${theme}.png`;
  preview.alt = `ScreenGarden in ${labels[theme]} mode: a growing field with camera controls and a daily score.`;
  caption.textContent = `Your world, in ${labels[theme]} mode.`;
  buttons.forEach((item) => item.setAttribute('aria-pressed', String(item === button)));
}));
