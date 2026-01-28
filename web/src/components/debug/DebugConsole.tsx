import React, { useEffect, useRef } from 'react';
import { useDebugStore } from './debugStore';
import { X, Trash2, Terminal } from 'lucide-react';

export const DebugConsole: React.FC = () => {
  const { logs, isOpen, toggleOpen, config, setConfig, clearLogs } = useDebugStore();
  const logsEndRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    logsEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [logs]);

  if (!isOpen) {
    return (
      <button
        onClick={toggleOpen}
        className="fixed bottom-4 left-4 p-2 bg-gray-800 text-white rounded-full shadow-lg hover:bg-gray-700 z-50 opacity-50 hover:opacity-100 transition-opacity"
        title="Open Debug Console"
      >
        <Terminal size={20} />
      </button>
    );
  }

  return (
    <div className="fixed bottom-4 left-4 w-96 max-h-[500px] flex flex-col bg-gray-900 text-gray-100 rounded-lg shadow-2xl z-50 border border-gray-700 font-mono text-sm">
      {/* Header */}
      <div className="flex items-center justify-between p-3 border-b border-gray-700 bg-gray-800 rounded-t-lg">
        <span className="font-bold flex items-center gap-2">
            <Terminal size={16} /> Debug Console
        </span>
        <div className="flex gap-2">
            <button onClick={clearLogs} className="p-1 hover:bg-gray-700 rounded" title="Clear Logs">
                <Trash2 size={16} />
            </button>
            <button onClick={toggleOpen} className="p-1 hover:bg-gray-700 rounded">
                <X size={16} />
            </button>
        </div>
      </div>

      {/* Settings */}
      <div className="p-3 border-b border-gray-700 bg-gray-800/50 space-y-3">
        <div className="flex items-center justify-between">
            <label className="text-gray-400">Model:</label>
            <div className="flex rounded overflow-hidden border border-gray-600">
                <button
                    onClick={() => setConfig({ model: 'fp32' })}
                    className={`px-2 py-1 text-xs ${config.model === 'fp32' ? 'bg-blue-600 text-white' : 'bg-gray-700 text-gray-300'}`}
                >FP32</button>
                <button
                    onClick={() => setConfig({ model: 'int8' })}
                    className={`px-2 py-1 text-xs ${config.model === 'int8' ? 'bg-blue-600 text-white' : 'bg-gray-700 text-gray-300'}`}
                >INT8</button>
            </div>
        </div>
        <div className="flex items-center justify-between">
            <label className="text-gray-400">Backend:</label>
            <select
                value={config.backend}
                onChange={(e) => setConfig({ backend: e.target.value as any })}
                className="bg-gray-700 border-gray-600 text-white text-xs rounded p-1"
            >
                <option value="webgpu">WebGPU (Auto)</option>
                <option value="wasm-mt">WASM (Multi-Threaded)</option>
                <option value="wasm-st">WASM (Single-Threaded)</option>
            </select>
        </div>
      </div>

      {/* Logs */}
      <div className="flex-1 overflow-y-auto p-3 space-y-1 min-h-[200px]">
        {logs.length === 0 && <div className="text-gray-500 italic text-center py-4">No logs yet...</div>}
        {logs.map((log, i) => (
            <div key={i} className={`flex gap-2 break-all ${log.level === 'error' ? 'text-red-400' : log.level === 'warn' ? 'text-yellow-400' : 'text-green-400'}`}>
                <span className="text-gray-500 shrink-0">[{log.timestamp}]</span>
                <span>{log.message}</span>
            </div>
        ))}
        <div ref={logsEndRef} />
      </div>
    </div>
  );
};
