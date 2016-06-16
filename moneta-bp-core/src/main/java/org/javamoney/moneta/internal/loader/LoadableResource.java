/**
 * Copyright (c) 2012, 2014, Credit Suisse (Anatole Tresch), Werner Keil and others by the @author tag.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.javamoney.moneta.internal.loader;

import org.javamoney.moneta.spi.LoaderService;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class represent a resource that automatically is reloaded, if needed.
 *
 * @author Anatole Tresch
 */
public class LoadableResource {

    /**
     * The logger used.
     */
    private static final Logger LOG = Logger.getLogger(LoadableResource.class.getName());
    /**
     * Lock for this instance.
     */
    private final Object LOCK = new Object();
    /**
     * resource id.
     */
    private final String resourceId;
    /**
     * The remote URLs to be looked up (first wins).
     */
    private final List<URI> remoteResources = new ArrayList<>();
    /**
     * The fallback location (classpath).
     */
    private final URI fallbackLocation;
    /**
     * The cache used.
     */
    private final ResourceCache cache;
    /**
     * How many times this resource was successfully loaded.
     */
    private final AtomicInteger loadCount = new AtomicInteger();
    /**
     * How many times this resource was accessed.
     */
    private final AtomicInteger accessCount = new AtomicInteger();
    /**
     * The current data array.
     */
    private volatile SoftReference<byte[]> data;
    /**
     * THe timestamp of the last successful load.
     */
    private long lastLoaded;
    /**
     * The time to live (TTL) of cache entries in milliseconds, by default 24 h.
     */
    private long cacheTTLMillis = 3600000L * 24; // 24 h

    /**
     * The required update policy for this resource.
     */
    private final LoaderService.UpdatePolicy updatePolicy;
    /**
     * The resource configuration.
     */
    private final Map<String, String> properties;


    /**
     * Create a new instance.
     *
     * @param resourceId       The dataId.
     * @param cache            The cache to be used for storing remote data locally.
     * @param properties       The configuration properties.
     * @param fallbackLocation teh fallback ULR, not null.
     * @param locations        the remote locations, not null (but may be empty!)
     */
    public LoadableResource(String resourceId, ResourceCache cache, LoaderService.UpdatePolicy updatePolicy,
                            Map<String, String> properties, URI fallbackLocation, URI... locations) {
        Objects.requireNonNull(resourceId, "resourceId required");
        Objects.requireNonNull(properties, "properties required");
        Objects.requireNonNull(updatePolicy, "updatePolicy required");
        String val = properties.get("cacheTTLMillis");
        if (val != null) {
            this.cacheTTLMillis = Long.parseLong(val);
        }
        this.cache = cache;
        this.resourceId = resourceId;
        this.updatePolicy = updatePolicy;
        this.properties = properties;
        this.fallbackLocation = fallbackLocation;
        this.remoteResources.addAll(Arrays.asList(locations));
    }

    /**
     * Get the UpdatePolicy of this resource.
     *
     * @return the UpdatePolicy of this resource, never null.
     */
    public LoaderService.UpdatePolicy getUpdatePolicy() {
        return updatePolicy;
    }

    /**
     * Get the configuration properties of this resource.
     *
     * @return the  configuration properties of this resource, never null.
     */
    public Map<String, String> getProperties() {
        return properties;
    }

    /**
     * Loads the resource, first from the remote resources, if that fails from
     * the fallback location.
     *
     * @return true, if load succeeded.
     */
    public boolean load() {
        if ((lastLoaded + cacheTTLMillis) <= System.currentTimeMillis()) {
            clearCache();
        }
        if (!readCache()) {
            if (shouldReadDataFromCallBack()) {
                return loadFallback();
            }
        }
        return true;
    }

    private boolean shouldReadDataFromCallBack() {
         return LoaderService.UpdatePolicy.NEVER.equals(updatePolicy) || !loadRemote();
    }

    /**
     * Get the resourceId.
     *
     * @return the resourceId
     */
    public final String getResourceId() {
        return resourceId;
    }

    /**
     * Get the remote locations.
     *
     * @return the remote locations, maybe empty.
     */
    public final List<URI> getRemoteResources() {
        return Collections.unmodifiableList(remoteResources);
    }

    /**
     * Return the fallback location.
     *
     * @return the fallback location, or null.
     */
    public final URI getFallbackResource() {
        return fallbackLocation;
    }

    /**
     * Get the number of active loads of this resource (InputStream).
     *
     * @return the number of successful loads.
     */
    public final int getLoadCount() {
        return loadCount.get();
    }

    /**
     * Get the number of successful accesses.
     *
     * @return the number of successful accesses.
     */
    public final int getAccessCount() {
        return accessCount.get();
    }

    /**
     * Get the resource data as input stream.
     *
     * @return the input stream.
     */
    public InputStream getDataStream() {
        return new WrappedInputStream(new ByteArrayInputStream(getData()));
    }

    /**
     * Get the timestamp of the last succesful load.
     *
     * @return the lastLoaded
     */
    public final long getLastLoaded() {
        return lastLoaded;
    }

    /**
     * Try to load the resource from the remote locations.
     *
     * @return true, on success.
     */
    public boolean loadRemote() {
        for (URI itemToLoad : remoteResources) {
            try {
                return !load(itemToLoad, false);
            } catch (Exception e) {
                LOG.log(Level.INFO, "Failed to load resource: " + itemToLoad, e);
            }
        }
        return true;
    }

    /**
     * Try to load the resource from the fallback resources. This will override
     * any remote data already loaded, and also will clear the cached data.
     *
     * @return true, on success.
     */
    public boolean loadFallback() {
        try {
            if (fallbackLocation == null) {
                Logger.getLogger(getClass().getName()).warning("No fallback resource for " + this +
                        ", loadFallback not supported.");
                return false;
            }
            load(fallbackLocation, true);
            clearCache();
            return true;
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Failed to load fallback resource: " + fallbackLocation, e);
        }
        return false;
    }

    /**
     * This method is called when the cached data should be removed, e.g. after an explicit fallback reload, or
     * a clear operation.
     */
    protected void clearCache() {
        if (this.cache != null) {
            this.cache.clear(resourceId);
        }
    }

    /**
     * This method is called when the data should be loaded from the cache. This method abstracts the effective
     * caching mechanism implemented. By default it tries to read a file from the current user's home directory.
     * If the data could be read, #setData(byte[]) should be called to apply the data read.
     *
     * @return true, if data could be read and applied from the cache sucdcessfully.
     */
    protected boolean readCache() {
        if (this.cache != null) {
            if (this.cache.isCached(resourceId)) {
                byte[] data = this.cache.read(resourceId);
                if (data != null) {
                    setData(data);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * This method is called after data could be successfully loaded from a non fallback resource. This method by
     * default writes an file containing the data into the user's local home directory, so subsequent or later calls,
     * even after a VM restart, should be able to recover this information.
     */
    protected void writeCache() {
        if (this.cache != null) {
            byte[] data = this.data == null ? null : this.data.get();
            if (data == null) {
                return;
            }
            this.cache.write(resourceId, data);
        }
    }

    /**
     * Tries to load the data from the given location. The location hereby can be a remote location or a local
     * location. Also it can be an URL pointing to a current dataset, or an url directing to fallback resources,
     * e.g. within the cuzrrent classpath.
     *
     * @param itemToLoad   the target {@link URL}
     * @param fallbackLoad true, for a fallback URL.
     * @return true, if load was successful.
     */
    protected boolean load(URI itemToLoad, boolean fallbackLoad) {
        InputStream is = null;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            URLConnection conn = itemToLoad.toURL().openConnection();
            byte[] data = new byte[4096];
            is = conn.getInputStream();
            int read = is.read(data);
            while (read > 0) {
                bos.write(data, 0, read);
                read = is.read(data);
            }
            setData(bos.toByteArray());
            if (!fallbackLoad) {
                writeCache();
            }
            if (!fallbackLoad) {
                lastLoaded = System.currentTimeMillis();
                loadCount.incrementAndGet();
            }
            return true;
        } catch (Exception e) {
            LOG.log(Level.INFO, "Failed to load resource input for " + resourceId + " from " + itemToLoad, e);
        } finally {
            if (is!=null) {
                try {
                    is.close();
                } catch (Exception e) {
                    LOG.log(Level.INFO, "Error closing resource input for " + resourceId, e);
                }
            }
            try {
                bos.close();
            } catch (IOException e) {
                LOG.log(Level.INFO, "Error closing resource input for " + resourceId, e);
            }
        }
        return false;
    }

    /**
     * Get the resource data. This will trigger a full load, if the resource is
     * not loaded, e.g. for LAZY resources.
     *
     * @return the data to load.
     */
    public final byte[] getData() {
        return getData(true);
    }

    protected byte[] getData(boolean loadIfNeeded) {
        byte[] result = this.data == null ? null : this.data.get();
        if (result == null && loadIfNeeded) {
            accessCount.incrementAndGet();
            byte[] currentData = this.data == null ? null : this.data.get();
            if (currentData==null) {
                synchronized (LOCK) {
                    currentData = this.data == null ? null : this.data.get();
                    if (currentData==null) {
                        if (shouldReadDataFromCallBack()) {
                            loadFallback();
                        }
                    }
                }
            }
            currentData = this.data == null ? null : this.data.get();
            if (currentData==null) {
                throw new IllegalStateException("Failed to load remote as well as fallback resources for " + this);
            }
            return currentData.clone();
        }
        return result;
    }

    protected final void setData(byte[] bytes) {
        this.data = new SoftReference<>(bytes);
    }


    public void unload() {
        synchronized (LOCK) {
            int count = accessCount.decrementAndGet();
            if (count == 0) {
                this.data = null;
            }
        }
    }

    /**
     * Explicitly override the resource wih the fallback context and resets the
     * load counter.
     *
     * @return true on success.
     */
    public boolean resetToFallback(){
        if (loadFallback()) {
            loadCount.set(0);
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return "LoadableResource [resourceId=" + resourceId + ", fallbackLocation=" +
                fallbackLocation + ", remoteResources=" + remoteResources +
                ", loadCount=" + loadCount + ", accessCount=" + accessCount + ", lastLoaded=" + lastLoaded + ']';
    }

    /**
     * InputStream , that helps managing the load count.
     *
     * @author Anatole
     */
    private final class WrappedInputStream extends InputStream {

        private final InputStream wrapped;

        public WrappedInputStream(InputStream wrapped) {
            this.wrapped = wrapped;
        }

        @Override
        public int read() throws IOException {
            return wrapped.read();
        }

        @Override
        public void close() throws IOException {
            try {
                wrapped.close();
                super.close();
            } finally {
                unload();
            }
        }

    }

}
