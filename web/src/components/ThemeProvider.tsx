import React, { createContext, useContext, useEffect, useState } from 'react';

type Theme = 'light' | 'dark' | 'system';

interface ThemeContextType {
    theme: Theme;
    setTheme: (theme: Theme) => void;
    isDark: boolean;
}

const ThemeContext = createContext<ThemeContextType | undefined>(undefined);

export const ThemeProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
    const [theme, setTheme] = useState<Theme>('system');
    const [isDark, setIsDark] = useState(false);

    useEffect(() => {
        const root = window.document.documentElement;
        const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)');

        const applyTheme = () => {
            if (theme === 'system') {
                const systemDark = mediaQuery.matches;
                setIsDark(systemDark);
                if (systemDark) {
                    root.classList.add('dark');
                } else {
                    root.classList.remove('dark');
                }
            } else {
                setIsDark(theme === 'dark');
                if (theme === 'dark') {
                    root.classList.add('dark');
                } else {
                    root.classList.remove('dark');
                }
            }
        };

        applyTheme();

        const listener = () => {
            if (theme === 'system') applyTheme();
        };

        mediaQuery.addEventListener('change', listener);
        return () => mediaQuery.removeEventListener('change', listener);
    }, [theme]);

    useEffect(() => {
        const color = isDark ? '#000000' : '#ffffff';
        const metaTags = document.querySelectorAll('meta[name="theme-color"]');
        metaTags.forEach((tag) => {
            tag.setAttribute('content', color);
            tag.removeAttribute('media');
        });

        if (metaTags.length === 0) {
            const meta = document.createElement('meta');
            meta.name = 'theme-color';
            meta.content = color;
            document.head.appendChild(meta);
        }
    }, [isDark]);

    return (
        <ThemeContext.Provider value={{ theme, setTheme, isDark }}>
            {children}
        </ThemeContext.Provider>
    );
};

export const useTheme = () => {
    const context = useContext(ThemeContext);
    if (!context) {
        throw new Error('useTheme must be used within a ThemeProvider');
    }
    return context;
};
