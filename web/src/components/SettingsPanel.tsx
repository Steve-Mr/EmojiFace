import React, { useRef, useEffect, useState } from 'react';
import { useEditorStore } from '../store/editorStore';
import { useDebugStore } from './debug/debugStore';
import { Smile, Sparkles, Type, Upload, Github, Moon, Sun, Monitor, Terminal, Trash2, ChevronRight, ChevronDown } from 'lucide-react';
import { useTranslation } from '../i18n/TranslationContext';
import { useTheme } from './ThemeProvider';

interface SettingsPanelProps {
    isOpen: boolean;
    onClose: () => void;
}

export const SettingsPanel: React.FC<SettingsPanelProps> = ({ isOpen, onClose }) => {
    const store = useEditorStore();
    const debugStore = useDebugStore();
    const { t, locale, setLocale } = useTranslation();
    const { theme, setTheme } = useTheme();

    const [isDebugExpanded, setIsDebugExpanded] = useState(false);
    const [randomEmojiText, setRandomEmojiText] = useState(() => store.randomEmojiList.join(', '));
    const logsEndRef = useRef<HTMLDivElement>(null);

    // Auto-scroll logs
    useEffect(() => {
        if (isDebugExpanded) {
            logsEndRef.current?.scrollIntoView({ behavior: 'smooth' });
        }
    }, [debugStore.logs, isDebugExpanded]);

    useEffect(() => {
        const storeList = store.randomEmojiList;
        // 解析当前文本框里的内容
        const currentList = randomEmojiText.split(',').map(s => s.trim()).filter(s => s.length > 0);
        
        // 比较两边的数组是否不同
        const isDifferent = storeList.length !== currentList.length || 
                            storeList.some((item, index) => item !== currentList[index]);

        // 只有在发生实质性变化时，才同步 store 的值到文本框
        // 这样可以避免用户输入逗号/空格等中间状态时被强制重置覆盖
        if (isDifferent) {
            setRandomEmojiText(storeList.join(', '));
        }
    }, [store.randomEmojiList, randomEmojiText]);

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
                                value={randomEmojiText}
                                onChange={(e) => {
                                    const nextValue = e.target.value;
                                    const nextList = nextValue.split(',').map(s => s.trim()).filter(s => s.length > 0);
                                    setRandomEmojiText(nextValue);

                                    const currentList = store.randomEmojiList;
                                    const hasChanged =
                                        nextList.length !== currentList.length ||
                                        nextList.some((item, index) => item !== currentList[index]);

                                    if (hasChanged) {
                                        store.setRandomEmojiList(nextList);
                                    }
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

                {/* Debug Section */}
                <section className="pt-4 border-t border-gray-100 dark:border-gray-800">
                    <button
                        onClick={() => setIsDebugExpanded(!isDebugExpanded)}
                        className="flex items-center justify-between w-full text-sm font-medium text-gray-700 dark:text-gray-300 mb-3 hover:text-blue-600 dark:hover:text-blue-400 transition-colors"
                    >
                        <div className="flex items-center gap-2">
                            <Terminal className="w-4 h-4" />
                            <span>Debug & Logs</span>
                        </div>
                        {isDebugExpanded ? <ChevronDown className="w-4 h-4" /> : <ChevronRight className="w-4 h-4" />}
                    </button>

                    {isDebugExpanded && (
                        <div className="space-y-3 animate-fade-in rounded-lg bg-gray-50 dark:bg-gray-950 p-3 border border-gray-200 dark:border-gray-800">
                             {/* Settings */}
                            <div className="space-y-2">
                                <div className="flex items-center justify-between">
                                    <label className="text-xs text-gray-500 dark:text-gray-400">Model:</label>
                                    <div className="flex rounded overflow-hidden border border-gray-300 dark:border-gray-700">
                                        <button
                                            onClick={() => debugStore.setConfig({ model: 'fp32' })}
                                            className={`px-2 py-1 text-[10px] font-medium ${debugStore.config.model === 'fp32' ? 'bg-blue-600 text-white' : 'bg-gray-100 text-gray-600 dark:bg-gray-800 dark:text-gray-400'}`}
                                        >FP32</button>
                                        <button
                                            onClick={() => debugStore.setConfig({ model: 'int8' })}
                                            className={`px-2 py-1 text-[10px] font-medium ${debugStore.config.model === 'int8' ? 'bg-blue-600 text-white' : 'bg-gray-100 text-gray-600 dark:bg-gray-800 dark:text-gray-400'}`}
                                        >INT8</button>
                                    </div>
                                </div>
                                <div className="flex items-center justify-between">
                                    <label className="text-xs text-gray-500 dark:text-gray-400">Backend:</label>
                                    <span className="text-[10px] text-gray-400 dark:text-gray-500">WASM (Single-Threaded)</span>
                                </div>
                            </div>

                             {/* Logs Header */}
                            <div className="flex items-center justify-between pt-2 border-t border-gray-200 dark:border-gray-800">
                                <span className="text-xs font-medium text-gray-500">Logs</span>
                                <button onClick={debugStore.clearLogs} className="p-1 hover:bg-gray-200 dark:hover:bg-gray-800 rounded text-gray-500 transition-colors" title="Clear Logs">
                                    <Trash2 size={12} />
                                </button>
                            </div>

                            {/* Logs */}
                            <div className="h-40 overflow-y-auto p-2 bg-white dark:bg-gray-900 rounded border border-gray-200 dark:border-gray-800 font-mono text-[10px] leading-tight space-y-1">
                                {debugStore.logs.length === 0 && <div className="text-gray-400 italic text-center py-4">No logs yet...</div>}
                                {debugStore.logs.map((log, i) => (
                                    <div key={i} className={`flex gap-2 break-all ${log.level === 'error' ? 'text-red-500' : log.level === 'warn' ? 'text-yellow-500' : 'text-green-600 dark:text-green-400'}`}>
                                        <span className="text-gray-400 shrink-0 opacity-50">[{log.timestamp}]</span>
                                        <span>{log.message}</span>
                                    </div>
                                ))}
                                <div ref={logsEndRef} />
                            </div>
                        </div>
                    )}
                </section>

                 {/* About Section */}
                 <section className="pt-4 border-t border-gray-100 dark:border-gray-800">
                    <h3 className="text-sm font-medium text-gray-700 dark:text-gray-300 mb-3">{t.about}</h3>
                    <a
                        href="https://github.com/Steve-Mr/EmojiFace"
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
