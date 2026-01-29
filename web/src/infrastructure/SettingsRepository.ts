import type { AppSettings } from '../domain/types';

const STORAGE_KEY = 'facemoji_settings';

const DEFAULT_SETTINGS: AppSettings = {
  defaultEmoji: '😊',
  defaultBlurType: 'gaussian',
};

export class SettingsRepository {
  getSettings(): AppSettings {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return DEFAULT_SETTINGS;
    try {
      return { ...DEFAULT_SETTINGS, ...JSON.parse(raw) };
    } catch {
      return DEFAULT_SETTINGS;
    }
  }

  saveSettings(settings: Partial<AppSettings>) {
    const current = this.getSettings();
    const newSettings = { ...current, ...settings };
    localStorage.setItem(STORAGE_KEY, JSON.stringify(newSettings));
  }
}

export const settingsRepo = new SettingsRepository();
