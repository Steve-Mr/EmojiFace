import { openDB, type DBSchema } from 'idb';
import type { Detection, Mask, AppSettings } from '../domain/types';

interface FacemojiDB extends DBSchema {
  state: {
    key: string;
    value: any;
  };
}

const DB_NAME = 'facemoji-editor';
const STORE_NAME = 'state';

class PersistenceRepository {
  private async getDB() {
    return openDB<FacemojiDB>(DB_NAME, 1, {
      upgrade(db) {
        db.createObjectStore(STORE_NAME);
      },
    });
  }

  async saveState(
    image: Blob | null | undefined,
    detections: Detection[],
    masks: Mask[],
    settings: Partial<AppSettings> | any // Using any for now to store generic settings like randomEmojiList
  ): Promise<void> {
    const db = await this.getDB();
    const tx = db.transaction(STORE_NAME, 'readwrite');

    const promises: Promise<unknown>[] = [
      tx.store.put(detections, 'detections'),
      tx.store.put(masks, 'masks'),
      tx.store.put(settings, 'settings'),
    ];

    if (image !== undefined) {
      promises.push(tx.store.put(image, 'image'));
    }

    await Promise.all([...promises, tx.done]);
  }

  async loadState() {
    const db = await this.getDB();
    const tx = db.transaction(STORE_NAME, 'readonly');

    const [image, detections, masks, settings] = await Promise.all([
      tx.store.get('image') as Promise<Blob | undefined>,
      tx.store.get('detections') as Promise<Detection[] | undefined>,
      tx.store.get('masks') as Promise<Mask[] | undefined>,
      tx.store.get('settings') as Promise<any | undefined>,
    ]);

    return {
      image: image || null,
      detections: detections || [],
      masks: masks || [],
      settings: settings || {}
    };
  }

  async clearState() {
    const db = await this.getDB();
    await db.clear(STORE_NAME);
  }
}

export const persistenceRepo = new PersistenceRepository();
