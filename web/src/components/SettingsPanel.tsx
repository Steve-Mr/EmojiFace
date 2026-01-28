import React from 'react';
import { useEditorStore } from '../store/editorStore';
import { Smile, Sparkles, Type, Upload, Github, Moon, Sun, Monitor } from 'lucide-react';
import { useTranslation } from '../i18n/TranslationContext';
import { useTheme } from './ThemeProvider';

interface SettingsPanelProps {
    isOpen: boolean;
    onClose: () => void;
}

export const SettingsPanel: React.FC<SettingsPanelProps> = ({ isOpen, onClose }) => {
    const store = useEditorStore();
    const { t, locale, setLocale } = useTranslation();
    const { theme, setTheme } = useTheme();

    // Handlers
    const handleFontUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
        if (e.target.files && e.target.files[0]) {
            store.uploadFont(e.target.files[0]);
        }
    };

    return (
        <>
        {/* Backdrop for mobile */}
        {isOpen && (
            <div
                className="fixed inset-0 bg-black/20 z-30 md:hidden animate-fade-in"
                onClick={onClose}
            />
        )}

        <div className={`
            fixed inset-y-0 right-0 w-80 bg-white dark:bg-gray-900 shadow-xl z-40
            transition-all duration-300 ease-in-out
            ${isOpen ? 'translate-x-0' : 'translate-x-full'}
            md:translate-x-0 md:static md:h-full md:border-l md:border-gray-200 md:dark:border-gray-800 md:shadow-none
            flex flex-col text-gray-900 dark:text-gray-100
        `}>
            {/* Header */}
            <div className="p-4 pt-[calc(1rem+env(safe-area-inset-top))] border-b border-gray-200 dark:border-gray-800 flex justify-between items-center bg-gray-50 dark:bg-gray-800/50">
                <h2 className="font-semibold text-gray-800 dark:text-gray-200">{t.settings}</h2>
                <button onClick={onClose} className="md:hidden text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-200">
                    ✕
                </button>
            </div>

            <div className="flex-1 overflow-y-auto p-4 space-y-6">

                {/* Language & Theme */}
                <section>
                    <h3 className="text-sm font-medium text-gray-700 dark:text-gray-300 mb-3">{t.language} & {t.theme}</h3>
                    <div className="flex gap-2 mb-3">
                         <button
                            onClick={() => setLocale('en')}
                            className={`flex-1 py-2 px-3 rounded-lg border text-sm font-medium transition-colors
                                ${locale === 'en'
                                    ? 'bg-blue-50 border-blue-500 text-blue-700 dark:bg-blue-900/20 dark:border-blue-500 dark:text-blue-300'
                                    : 'bg-white border-gray-200 text-gray-700 hover:bg-gray-50 dark:bg-gray-800 dark:border-gray-700 dark:text-gray-300 dark:hover:bg-gray-700'
                                }`}
                         >
                            English
                         </button>
                         <button
                            onClick={() => setLocale('zh')}
                            className={`flex-1 py-2 px-3 rounded-lg border text-sm font-medium transition-colors
                                ${locale === 'zh'
                                    ? 'bg-blue-50 border-blue-500 text-blue-700 dark:bg-blue-900/20 dark:border-blue-500 dark:text-blue-300'
                                    : 'bg-white border-gray-200 text-gray-700 hover:bg-gray-50 dark:bg-gray-800 dark:border-gray-700 dark:text-gray-300 dark:hover:bg-gray-700'
                                }`}
                         >
                            中文
                         </button>
                    </div>

                    <div className="grid grid-cols-3 gap-2">
                        {[
                            { id: 'light', icon: Sun, label: t.light },
                            { id: 'dark', icon: Moon, label: t.dark },
                            { id: 'system', icon: Monitor, label: t.system },
                        ].map((item) => (
                             <button
                                key={item.id}
                                onClick={() => setTheme(item.id as any)}
                                className={`flex flex-col items-center gap-1 py-2 px-2 rounded-lg border text-xs font-medium transition-colors
                                    ${theme === item.id
                                        ? 'bg-blue-50 border-blue-500 text-blue-700 dark:bg-blue-900/20 dark:border-blue-500 dark:text-blue-300'
                                        : 'bg-white border-gray-200 text-gray-700 hover:bg-gray-50 dark:bg-gray-800 dark:border-gray-700 dark:text-gray-300 dark:hover:bg-gray-700'
                                    }`}
                             >
                                <item.icon className="w-4 h-4" />
                                <span>{item.label}</span>
                             </button>
                        ))}
                    </div>
                </section>

                {/* Mask Mode */}
                <section>
                    <h3 className="text-sm font-medium text-gray-700 dark:text-gray-300 mb-3">{t.maskMode}</h3>
                    <div className="grid grid-cols-2 gap-3">
                        <button
                            onClick={() => store.setMaskType('emoji')}
                            className={`flex flex-col items-center gap-2 p-3 rounded-xl border-2 transition-all
                                ${store.currentMaskType === 'emoji'
                                    ? 'border-blue-500 bg-blue-50 text-blue-700 dark:bg-blue-900/20 dark:border-blue-500 dark:text-blue-300'
                                    : 'border-transparent bg-gray-100 text-gray-600 hover:bg-gray-200 dark:bg-gray-800 dark:text-gray-400 dark:hover:bg-gray-700'
                                }`}
                        >
                            <Smile className="w-6 h-6" />
                            <span className="text-sm font-medium">{t.emoji}</span>
                        </button>
                        <button
                            onClick={() => store.setMaskType('blur')}
                            className={`flex flex-col items-center gap-2 p-3 rounded-xl border-2 transition-all
                                ${store.currentMaskType === 'blur'
                                    ? 'border-blue-500 bg-blue-50 text-blue-700 dark:bg-blue-900/20 dark:border-blue-500 dark:text-blue-300'
                                    : 'border-transparent bg-gray-100 text-gray-600 hover:bg-gray-200 dark:bg-gray-800 dark:text-gray-400 dark:hover:bg-gray-700'
                                }`}
                        >
                            <Sparkles className="w-6 h-6" />
                            <span className="text-sm font-medium">{t.blur}</span>
                        </button>
                    </div>
                </section>

                {/* Emoji Settings */}
                {store.currentMaskType === 'emoji' && (
                    <div className="space-y-6 animate-fade-in">
                        <section>
                            <div className="flex items-center justify-between mb-2">
                                <label className="text-sm font-medium text-gray-700 dark:text-gray-300">{t.randomEmojiList}</label>
                                <span className="text-xs text-gray-400">{t.commaSeparated}</span>
                            </div>
                            <textarea
                                value={store.randomEmojiList.join(', ')}
                                onChange={(e) => {
                                    const list = e.target.value.split(',').map(s => s.trim()).filter(s => s.length > 0);
                                    store.setRandomEmojiList(list);
                                }}
                                className="w-full h-24 p-3 rounded-lg border border-gray-300 text-xl focus:ring-2 focus:ring-blue-500 focus:border-blue-500 resize-none
                                    bg-white dark:bg-gray-800 dark:border-gray-700 dark:text-gray-100 dark:focus:border-blue-500"
                                placeholder="😂, 😎, 😆..."
                            />
                        </section>

                        <section>
                            <div className="flex items-center gap-2 mb-3 text-sm font-medium text-gray-700 dark:text-gray-300">
                                <Type className="w-4 h-4" />
                                {t.font}
                            </div>
                            <div className="space-y-3">
                                <select
                                    value={store.currentFont}
                                    onChange={(e) => store.setCurrentFont(e.target.value)}
                                    className="w-full p-2.5 rounded-lg border border-gray-300 bg-white text-sm
                                        dark:bg-gray-800 dark:border-gray-700 dark:text-gray-100"
                                >
                                    <option value="">{t.systemDefault}</option>
                                    {store.availableFonts.map(f => (
                                        <option key={f} value={f}>{f}</option>
                                    ))}
                                </select>
                                <label className="flex items-center justify-center gap-2 w-full p-2.5 rounded-lg border border-dashed border-gray-300 text-gray-500 hover:bg-gray-50 hover:border-gray-400 cursor-pointer transition-colors
                                    dark:border-gray-700 dark:text-gray-400 dark:hover:bg-gray-800 dark:hover:border-gray-600">
                                    <Upload className="w-4 h-4" />
                                    <span className="text-sm">{t.uploadFont}</span>
                                    <input type="file" accept=".ttf,.otf,.woff,.woff2" className="hidden" onChange={handleFontUpload} />
                                </label>
                            </div>
                        </section>
                    </div>
                )}

                {/* Blur Settings */}
                {store.currentMaskType === 'blur' && (
                    <div className="space-y-4 animate-fade-in">
                        <section>
                            <h3 className="text-sm font-medium text-gray-700 dark:text-gray-300 mb-3">{t.blurType}</h3>
                            <div className="flex flex-col gap-2">
                                {['gaussian', 'pixelate'].map((type) => (
                                    <label key={type} className={`flex items-center p-3 rounded-lg border cursor-pointer transition-colors
                                        ${store.currentBlurType === type
                                            ? 'border-blue-500 bg-blue-50 dark:bg-blue-900/20 dark:border-blue-500'
                                            : 'border-gray-200 hover:bg-gray-50 dark:border-gray-700 dark:hover:bg-gray-800'
                                        }`}>
                                        <input
                                            type="radio"
                                            name="blurType"
                                            value={type}
                                            checked={store.currentBlurType === type}
                                            onChange={() => store.setBlurType(type as any)}
                                            className="w-4 h-4 text-blue-600 border-gray-300 focus:ring-blue-500 dark:border-gray-600 dark:bg-gray-700"
                                        />
                                        <span className="ml-3 text-sm font-medium text-gray-700 dark:text-gray-300 capitalize">{type === 'gaussian' ? t.gaussian : t.pixelate}</span>
                                    </label>
                                ))}
                            </div>
                        </section>
                    </div>
                )}

                 {/* About Section */}
                 <section className="pt-4 border-t border-gray-100 dark:border-gray-800">
                    <h3 className="text-sm font-medium text-gray-700 dark:text-gray-300 mb-3">{t.about}</h3>
                    <a
                        href="https://github.com/maary/FaceMoji"
                        target="_blank"
                        rel="noopener noreferrer"
                        className="flex items-center gap-2 text-sm text-gray-600 hover:text-blue-600 transition-colors dark:text-gray-400 dark:hover:text-blue-400"
                    >
                        <Github className="w-5 h-5" />
                        <span>{t.visitRepo}</span>
                    </a>
                </section>
            </div>

            <div className="p-4 pb-[calc(1rem+env(safe-area-inset-bottom))] border-t border-gray-200 dark:border-gray-800 bg-gray-50 dark:bg-gray-800/50 text-xs text-center text-gray-400 dark:text-gray-500">
                {t.footer}
            </div>
        </div>
        </>
    );
};
