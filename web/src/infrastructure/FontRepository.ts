import { openDB, type DBSchema, type IDBPDatabase } from 'idb';

interface FaceMojiDB extends DBSchema {
  fonts: {
    key: string;
    value: {
      name: string;
      buffer: ArrayBuffer;
    };
  };
}

const DB_NAME = 'facemoji-db';
const DB_VERSION = 1;

export class FontRepository {
  private dbPromise: Promise<IDBPDatabase<FaceMojiDB>>;

  constructor() {
    this.dbPromise = openDB<FaceMojiDB>(DB_NAME, DB_VERSION, {
      upgrade(db) {
        if (!db.objectStoreNames.contains('fonts')) {
          db.createObjectStore('fonts', { keyPath: 'name' });
        }
      },
    });
  }

  async saveFont(name: string, buffer: ArrayBuffer): Promise<void> {
    const db = await this.dbPromise;
    await db.put('fonts', { name, buffer });
  }

  async getAllFonts(): Promise<{ name: string; buffer: ArrayBuffer }[]> {
    const db = await this.dbPromise;
    return db.getAll('fonts');
  }

  async deleteFont(name: string): Promise<void> {
    const db = await this.dbPromise;
    await db.delete('fonts', name);
  }
}

export const fontRepo = new FontRepository();
