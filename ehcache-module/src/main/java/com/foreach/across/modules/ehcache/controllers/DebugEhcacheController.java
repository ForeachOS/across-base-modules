/*
 * Copyright 2014 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.foreach.across.modules.ehcache.controllers;

import com.foreach.across.modules.debugweb.DebugWeb;
import com.foreach.across.modules.debugweb.mvc.DebugMenuEvent;
import com.foreach.across.modules.debugweb.mvc.DebugWebController;
import com.foreach.across.modules.web.resource.WebResource;
import com.foreach.across.modules.web.resource.WebResourceRegistry;
import com.foreach.across.modules.web.resource.WebResourceRule;
import lombok.RequiredArgsConstructor;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.distribution.CacheManagerPeerProvider;
import net.sf.ehcache.distribution.CachePeer;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.rmi.RemoteException;
import java.util.*;

@DebugWebController
@RequiredArgsConstructor
class DebugEhcacheController
{
	private static final Logger LOG = LoggerFactory.getLogger( DebugEhcacheController.class );

	private final DebugWeb debugWeb;

	@EventListener
	public void buildMenu( DebugMenuEvent event ) {
		event.builder()
		     .group( "/cache/ehcache", "Ehcache" )
		     .and()
		     .item( "/cache/ehcache/overview", "Ehcache managers", "/ehcache" );
	}

	@ModelAttribute
	public void init( WebResourceRegistry registry ) {
		registry.apply(
				WebResourceRule.add( WebResource.css( "/css/ehcache/ehcache.css" ) ).toBucket( WebResource.CSS ) );
	}

	@ModelAttribute
	public CacheManager cacheManager( @RequestParam(required = false, defaultValue = "") String managerName,
	                                  Model model ) {
		model.addAttribute( "cacheManagers", CacheManager.ALL_CACHE_MANAGERS );

		for ( CacheManager cacheManager : CacheManager.ALL_CACHE_MANAGERS ) {
			if ( StringUtils.equals( managerName, cacheManager.getName() ) ) {
				return cacheManager;
			}
		}

		return CacheManager.ALL_CACHE_MANAGERS.get( 0 );
	}

	@RequestMapping(value = "/ehcache", method = RequestMethod.GET)
	public String listCaches( @ModelAttribute("cacheManager") CacheManager cacheManager, Model model ) {
		model.addAttribute( "cacheList", cacheList( cacheManager ) );

		//cacheManager.getCacheManagerEventListenerRegistry().getRegisteredListeners().iterator().next()
		Map<String, CacheManagerPeerProvider> cacheManagerPeerProviders = cacheManager.getCacheManagerPeerProviders();
		model.addAttribute( "cacheManagerProviders", cacheManagerPeerProviders.keySet() );

		return "th/ehcache/cacheList";
	}

	private List<Cache> cacheList( CacheManager cacheManager ) {
		List<String> names = Arrays.asList( cacheManager.getCacheNames() );
		Collections.sort( names );

		List<Cache> caches = new ArrayList<>( names.size() );

		for ( String name : names ) {
			caches.add( cacheManager.getCache( name ) );
		}

		return caches;
	}

	@RequestMapping(value = "/ehcache/heap", method = RequestMethod.GET)
	public String cacheHeapSize( @ModelAttribute("cacheManager") CacheManager cacheManager, Model model ) {
		List<CacheHeapInfo> heapInfoList = new ArrayList<>();

		long totalHeap = 0, maxHeap = 0;
		long totalItems = 0, maxItems = 0;

		for ( Cache cache : cacheList( cacheManager ) ) {
			totalItems += cache.getSize();
			maxItems += cache.getCacheConfiguration().getMaxEntriesLocalHeap();

			CacheHeapInfo heapInfo = new CacheHeapInfo();
			heapInfo.setCache( cache );

			heapInfoList.add( heapInfo );

			try {
				heapInfo.setSize( cache.getStatistics().getExtended().localHeapSizeInBytes().value() );
				totalHeap += heapInfo.getSize().longValue();
			}
			catch ( Exception e ) {
				// Exception calculating heap size (classloading exception)
				LOG.warn( "Unable to calculate heap size for cache {}", cache.getName(), e );
			}
		}

		for ( CacheHeapInfo info : heapInfoList ) {
			if ( info.getSize() != null ) {
				if ( info.getSize().longValue() == 0 ) {
					info.setPercentageOfTotal( 0 );
				}
				else {
					info.setPercentageOfTotal(
							BigDecimal.valueOf( info.getSize().longValue() )
							          .divide( BigDecimal.valueOf( totalHeap ), 4,
							                   RoundingMode.HALF_UP )
					);

					info.setEstimatedMax(
							BigDecimal.valueOf( info.getSize().longValue() )
							          .divide( BigDecimal.valueOf( info.getCache().getSize() ), 4,
							                   RoundingMode.HALF_UP )
							          .multiply( BigDecimal.valueOf(
									          info.getCache().getCacheConfiguration()
									              .getMaxEntriesLocalHeap() ) )
					);

					maxHeap += info.getEstimatedMax().longValue();
				}
			}
		}

		model.addAttribute( "totalItems", totalItems );
		model.addAttribute( "maxItems", maxItems );
		model.addAttribute( "totalHeap", totalHeap );
		model.addAttribute( "maxHeap", maxHeap );
		model.addAttribute( "heapInfoList", heapInfoList );

		return "th/ehcache/cacheHeapDetail";
	}

	@RequestMapping(value = "/ehcache/flush", method = RequestMethod.GET)
	public String flushCache( @ModelAttribute("cacheManager") CacheManager cacheManager,
	                          @RequestParam(value = "cache", required = false) String cacheName,
	                          @RequestParam(value = "from", required = false) String from,
	                          @RequestParam(value = "replicate", required = false,
			                          defaultValue = "false") String replicate ) {
		String[] cachesToFlush = cacheName == null ? cacheManager.getCacheNames() : new String[] { cacheName };

		for ( String cache : cachesToFlush ) {
			if ( StringUtils.equalsIgnoreCase( replicate, "true" ) ) {
				cacheManager.getCache( cache ).removeAll();
			}
			else {
				cacheManager.getCache( cache ).flush();
			}
		}

		return debugWeb.redirect(
				"/ehcache?flushed=" + cachesToFlush.length + "&managerName=" + cacheManager.getName() );
	}

	@RequestMapping(value = "/ehcache/view", method = RequestMethod.GET)
	public String showCache( @ModelAttribute("cacheManager") CacheManager cacheManager,
	                         @RequestParam("cache") String cacheName,
	                         @RequestParam(value = "listPeers", defaultValue = StringUtils.EMPTY) String listPeers,
	                         Model model ) {
		Cache cache = cacheManager.getCache( cacheName );

		List<CacheEntry> cacheEntries = new ArrayList<>();
		for ( Object key : cache.getKeys() ) {
			Element cacheElement = cache.getQuiet( key );

			if ( cacheElement != null && !cacheElement.isExpired() ) {
				long age = System.currentTimeMillis() - cacheElement.getLatestOfCreationAndUpdateTime();
				long accessed = System.currentTimeMillis() - cacheElement.getLastAccessTime();

				CacheEntry cacheEntry = new CacheEntry();
				cacheEntry.setKey( key.toString() );
				cacheEntry.setValue( cacheElement.getObjectValue() );
				cacheEntry.setAge( DurationFormatUtils.formatDurationHMS( age ) );
				cacheEntry.setLastAccessed( DurationFormatUtils.formatDurationHMS( accessed ) );
				cacheEntry.setHits( cacheElement.getHitCount() );
				cacheEntries.add( cacheEntry );
			}
		}

		model.addAttribute( "cache", cache );
		model.addAttribute( "cacheEntries", cacheEntries );

		if ( StringUtils.equalsIgnoreCase( "true", listPeers ) ) {
			List<String> cachePeers = new ArrayList<>();
			Map<String, CacheManagerPeerProvider> cacheManagerPeerProviders =
					cacheManager.getCacheManagerPeerProviders();
			for ( Map.Entry<String, CacheManagerPeerProvider> cacheManagerPeerProviderEntry : cacheManagerPeerProviders
					.entrySet() ) {
				List cachePeersList = cacheManagerPeerProviderEntry.getValue().listRemoteCachePeers( cache );
				for ( Object object : cachePeersList ) {
					if ( object instanceof CachePeer ) {
						CachePeer cachePeer = (CachePeer) object;
						String cachePeerItem = cacheManagerPeerProviderEntry.getKey();
						try {
							cachePeerItem = ", " + cachePeer.getUrl() + " " + cachePeer.getGuid();
						}
						catch ( RemoteException e ) {
							cachePeerItem += " - remote exception occurred";
						}
						cachePeers.add( cacheManagerPeerProviderEntry.getKey() + ", " + cachePeerItem );
					}
				}
			}
			model.addAttribute( "cachePeers", cachePeers );
		}
		else {
			model.addAttribute( "cachePeers", "none" );
		}

		return "th/ehcache/cacheDetail";
	}

	private static class CacheHeapInfo
	{
		private Cache cache;
		private Number size, percentageOfTotal, estimatedMax;

		public Cache getCache() {
			return cache;
		}

		public void setCache( Cache cache ) {
			this.cache = cache;
		}

		public Number getSize() {
			return size;
		}

		public void setSize( Number size ) {
			this.size = size;
		}

		public Number getPercentageOfTotal() {
			return percentageOfTotal;
		}

		public void setPercentageOfTotal( Number percentageOfTotal ) {
			this.percentageOfTotal = percentageOfTotal;
		}

		public Number getEstimatedMax() {
			return estimatedMax;
		}

		public void setEstimatedMax( Number estimatedMax ) {
			this.estimatedMax = estimatedMax;
		}
	}

	private static class CacheEntry
	{
		private String key;
		private Object value;
		private String age;
		private String lastAccessed;
		private Long hits;

		public String getKey() {
			return key;
		}

		public void setKey( String key ) {
			this.key = key;
		}

		public Object getValue() {
			return value;
		}

		public void setValue( Object value ) {
			this.value = value;
		}

		public boolean isNullValue() {
			return value == null;
		}

		public String getAge() {
			return age;
		}

		public void setAge( String age ) {
			this.age = age;
		}

		public String getLastAccessed() {
			return lastAccessed;
		}

		public void setLastAccessed( String lastAccessed ) {
			this.lastAccessed = lastAccessed;
		}

		public Long getHits() {
			return hits;
		}

		public void setHits( Long hits ) {
			this.hits = hits;
		}
	}
}
