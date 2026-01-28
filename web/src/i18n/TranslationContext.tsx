import React, { createContext, useContext, useState } from 'react';
import type { TranslationKeys, Locale } from './types';
import { en } from './en';
import { zh } from './zh';

type TranslationContextType = {
    t: TranslationKeys;
    locale: Locale;
    setLocale: (locale: Locale) => void;
};

const TranslationContext = createContext<TranslationContextType | undefined>(undefined);

const translations: Record<Locale, TranslationKeys> = {
    en,
    zh
};

export const TranslationProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
    // Try to get from localStorage or detect from browser
    const getInitialLocale = (): Locale => {
        const saved = localStorage.getItem('facemoji-locale');
        if (saved === 'en' || saved === 'zh') return saved;
        return navigator.language.startsWith('zh') ? 'zh' : 'en';
    };

    const [locale, setLocaleState] = useState<Locale>(getInitialLocale);

    const setLocale = (newLocale: Locale) => {
        setLocaleState(newLocale);
        localStorage.setItem('facemoji-locale', newLocale);
    };

    return (
        <TranslationContext.Provider value={{ t: translations[locale], locale, setLocale }}>
            {children}
        </TranslationContext.Provider>
    );
};

export const useTranslation = () => {
    const context = useContext(TranslationContext);
    if (!context) {
        throw new Error('useTranslation must be used within a TranslationProvider');
    }
    return context;
};
