package app_kvServer;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class KVServerStatus {
    private ReadWriteLock writeLock;
	private boolean isWriteLocked;

    private ReadWriteLock versionLock;
	private int version;

    public KVServerStatus() {
        this.writeLock = new ReentrantReadWriteLock();
        this.isWriteLocked = false;

        this.versionLock = new ReentrantReadWriteLock();
        this.version = 0;
    }

    public void setWriteLocked(boolean locked) {
        isWriteLocked = locked;
    }

    public void writeWriteLock() {
        writeLock.writeLock().lock();
    }

    public void writeWriteUnlock() {
        writeLock.writeLock().unlock();
    }

    public void writeReadLock() {
        writeLock.readLock().lock();
    }

    public void writeReadUnlock() {
        writeLock.readLock().unlock();
    }

    public void versionWriteLock() {
        versionLock.writeLock().lock();
    }

    public void versionWriteUnlock() {
        versionLock.writeLock().unlock();
    }

    public void versionReadLock() {
        versionLock.readLock().lock();
    }

    public void versionReadUnlock() {
        versionLock.readLock().unlock();
    }

    public void updateVersion() {
        version += 1;
        version %= 10;
    }

    public int getVersion() {
        return version;
    }

    public boolean isWriteLocked() {
        return isWriteLocked;
    }
}
