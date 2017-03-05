package app_kvServer;

import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import app_kvEcs.*;
import common.*;

public class KVServerStatus {
    private ReadWriteLock writeLock;
	private boolean isWriteLocked;

    private ReadWriteLock versionLock;
	private int version;

    private ReadWriteLock metadataLock;

    private ReadWriteLock readLock;
    private boolean rerouteReads;
    private boolean stopServer;

    private int port;

    private TreeSet<ECSNode> metadata;

    public KVServerStatus(int port, TreeSet<ECSNode> metadata) {
        this.writeLock = new ReentrantReadWriteLock();
        this.isWriteLocked = false;

        this.versionLock = new ReentrantReadWriteLock();
        this.version = 0;

        this.metadataLock = new ReentrantReadWriteLock();

        this.readLock = new ReentrantReadWriteLock();
        this.rerouteReads = false;
        this.stopServer = false;

        this.port = port; 
        this.metadata = metadata;
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

    public void metadataWriteLock() {
        metadataLock.writeLock().lock();
    }

    public void metadataWriteUnlock() {
        metadataLock.writeLock().unlock();
    }

    public void metadataReadLock() {
        metadataLock.readLock().lock();
    }

    public void metadataReadUnlock() {
        metadataLock.readLock().unlock();
    }

    public void readWriteLock() {
        readLock.writeLock().lock();
    }

    public void readWriteUnlock() {
        readLock.writeLock().unlock();
    }

    public void readReadLock() {
        readLock.readLock().lock();
    }

    public void readReadUnlock() {
        readLock.readLock().unlock();
    }

    public void setRerouteReads(boolean reroute) {
        rerouteReads = reroute;
    }

    public void setStopServer(boolean stop) {
        stopServer = stop;
    }

    public boolean rerouteReads() {
        return rerouteReads;
    }

    public boolean stopServer() {
        return stopServer;
    }

    public int getPort() {
        return port;
    }

    public void setMetadata(TreeSet<ECSNode> metadata) {
        this.metadata = metadata;
    }

    public TreeSet<ECSNode> getMetadata() {
        return metadata;
    }
}
