import React from 'react';
import { useTranslation } from '../i18n/TranslationContext';

interface ConfirmDialogProps {
    isOpen: boolean;
    title: string;
    message: string;
    onConfirm: () => void;
    onCancel: () => void;
}

export const ConfirmDialog: React.FC<ConfirmDialogProps> = ({ isOpen, title, message, onConfirm, onCancel }) => {
    const { t } = useTranslation();
    if (!isOpen) return null;

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm animate-fade-in">
            <div className="bg-white dark:bg-gray-900 rounded-xl shadow-xl max-w-sm w-full mx-4 overflow-hidden animate-zoom-in border border-transparent dark:border-gray-800">
                <div className="p-6">
                    <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-2">{title}</h3>
                    <p className="text-gray-600 dark:text-gray-400">{message}</p>
                </div>
                <div className="bg-gray-50 dark:bg-gray-800/50 px-6 py-4 flex justify-end gap-3 border-t border-gray-100 dark:border-gray-800">
                    <button
                        onClick={onCancel}
                        className="px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-200 dark:text-gray-300 dark:hover:bg-gray-700 rounded-lg transition-colors"
                    >
                        {t.keepIt}
                    </button>
                    <button
                        onClick={onConfirm}
                        className="px-4 py-2 text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 dark:bg-blue-600 dark:hover:bg-blue-500 rounded-lg transition-colors"
                    >
                        {t.clearIt}
                    </button>
                </div>
            </div>
        </div>
    );
};
