/* Earth Now — globe rendering engine (MapLibre GL JS 5, globe projection).
 * All data pushed from Android comes from real providers. No fake data. */
(function () {
  'use strict';

  var EMPTY_FC = { type: 'FeatureCollection', features: [] };
  var BLANK_IMG = 'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==';

  function log(msg) {
    if (window.AndroidBridge && AndroidBridge.onLog) AndroidBridge.onLog(String(msg));
  }
  function bridgeError(msg) {
    if (window.AndroidBridge && AndroidBridge.onError) AndroidBridge.onError(String(msg));
  }

  // ---------- Basemaps (all free / keyless) ----------
  var OPENFREEMAP_TILES = 'https://tiles.openfreemap.org/planet';
  var OPENFREEMAP_GLYPHS = 'https://tiles.openfreemap.org/fonts/{fontstack}/{range}.pbf';
  var OSM_TILES = ['https://tile.openstreetmap.org/{z}/{x}/{y}.png'];
  var EOX_SATELLITE = 'https://tiles.maps.eox.at/wmts?layer=s2cloudless-2020_3857&style=default' +
    '&tilematrixset=GoogleMapsCompatible&Service=WMTS&Request=GetTile&Version=1.0.0' +
    '&Format=image%2Fjpeg&TileMatrix={z}&TileCol={x}&TileRow={y}';

  function darkBaseLayers() {
    return [
      { id: 'base_landcover', type: 'fill', source: 'ofm', 'source-layer': 'landcover',
        filter: ['in', ['get', 'class'], ['literal', ['wood', 'grass', 'scrub', 'snow', 'ice']]],
        paint: { 'fill-color': '#1d2b38', 'fill-opacity': 0.9 } },
      { id: 'base_landuse', type: 'fill', source: 'ofm', 'source-layer': 'landuse',
        paint: { 'fill-color': '#1a2733', 'fill-opacity': 0.7 } },
      { id: 'base_park', type: 'fill', source: 'ofm', 'source-layer': 'park',
        paint: { 'fill-color': '#1b332e', 'fill-opacity': 0.55 } },
      { id: 'base_water', type: 'fill', source: 'ofm', 'source-layer': 'water',
        paint: { 'fill-color': '#0d2b4d' } },
      { id: 'base_boundary', type: 'line', source: 'ofm', 'source-layer': 'boundary',
        filter: ['<=', ['get', 'admin_level'], 3],
        paint: { 'line-color': '#41587a', 'line-width': 0.7, 'line-opacity': 0.8 } },
      { id: 'base_place_country', type: 'symbol', source: 'ofm', 'source-layer': 'place',
        filter: ['==', ['get', 'class'], 'country'],
        layout: {
          'text-field': ['get', 'name'],
          'text-font': ['Noto Sans Regular'],
          'text-size': ['interpolate', ['linear'], ['zoom'], 0, 11, 4, 14],
          'text-transform': 'uppercase',
          'text-letter-spacing': 0.12,
          'text-max-width': 7
        },
        paint: {
          'text-color': '#93a8c6',
          'text-halo-color': 'rgba(2,6,14,0.85)',
          'text-halo-width': 1.1
        } },
      { id: 'base_place_city', type: 'symbol', source: 'ofm', 'source-layer': 'place',
        filter: ['in', ['get', 'class'], ['literal', ['city', 'town']]],
        minzoom: 3,
        layout: {
          'text-field': ['get', 'name'],
          'text-font': ['Noto Sans Regular'],
          'text-size': ['interpolate', ['linear'], ['zoom'], 3, 10, 8, 13],
          'text-max-width': 8
        },
        paint: {
          'text-color': '#7d92b0',
          'text-halo-color': 'rgba(2,6,14,0.85)',
          'text-halo-width': 1
        } }
    ];
  }

  var style = {
    version: 8,
    name: 'Earth Now Space',
    glyphs: OPENFREEMAP_GLYPHS,
    sources: {
      ofm: { type: 'vector', url: OPENFREEMAP_TILES, attribution: '© OpenFreeMap © OpenStreetMap contributors' },
      osm_tiles: { type: 'raster', tiles: OSM_TILES, tileSize: 256, maxzoom: 19, attribution: '© OpenStreetMap contributors' },
      satellite_tiles: { type: 'raster', tiles: [EOX_SATELLITE], tileSize: 256, maxzoom: 14, attribution: 'Sentinel-2 cloudless © EOX' },

      raster_temp: { type: 'geojson', data: EMPTY_FC },
      raster_clouds: { type: 'geojson', data: EMPTY_FC },
      raster_ocean: { type: 'geojson', data: EMPTY_FC },
      raster_aurora: { type: 'geojson', data: EMPTY_FC },

      radar_tiles: { type: 'raster', tiles: ['https://tilecache.rainviewer.com/v2/radar/0/{z}/{x}/{y}/2/1_1.png'], tileSize: 256, maxzoom: 7, attribution: 'RainViewer' },

      wind_lines: { type: 'geojson', data: EMPTY_FC },
      events_eq: { type: 'geojson', data: EMPTY_FC, cluster: true, clusterRadius: 64, clusterMaxZoom: 11 },
      events_fire: { type: 'geojson', data: EMPTY_FC, cluster: true, clusterRadius: 60, clusterMaxZoom: 10 },
      events_volc: { type: 'geojson', data: EMPTY_FC, cluster: true, clusterRadius: 60, clusterMaxZoom: 8 },
      day_night: { type: 'geojson', data: EMPTY_FC }
    },
    layers: [
      { id: 'background_layer', type: 'background', paint: { 'background-color': '#04060c' } }
    ].concat(darkBaseLayers()).concat([
      { id: 'osm_layer', type: 'raster', source: 'osm_tiles', layout: { visibility: 'none' } },
      { id: 'satellite_layer', type: 'raster', source: 'satellite_tiles', layout: { visibility: 'none' } },

      { id: 'layer_day_night', type: 'fill', source: 'day_night',
        paint: { 'fill-color': '#000a14', 'fill-opacity': 0 } },

      { id: 'layer_raster_temp', type: 'fill', source: 'raster_temp', layout: { visibility: 'none' },
        paint: { 'fill-color': ['get', 'c'], 'fill-opacity': 0.72, 'fill-antialias': false } },
      { id: 'layer_raster_clouds', type: 'fill', source: 'raster_clouds', layout: { visibility: 'none' },
        paint: { 'fill-color': ['get', 'c'], 'fill-opacity': 1.0, 'fill-antialias': false } },
      { id: 'layer_raster_ocean', type: 'fill', source: 'raster_ocean', layout: { visibility: 'none' },
        paint: { 'fill-color': ['get', 'c'], 'fill-opacity': 0.80, 'fill-antialias': false } },
      { id: 'layer_raster_aurora', type: 'fill', source: 'raster_aurora', layout: { visibility: 'none' },
        paint: { 'fill-color': ['get', 'c'], 'fill-opacity': 0.85, 'fill-antialias': false } },
      { id: 'layer_radar', type: 'raster', source: 'radar_tiles', layout: { visibility: 'none' },
        paint: { 'raster-opacity': 0.65 } },

      { id: 'layer_wind', type: 'line', source: 'wind_lines', layout: { visibility: 'none', 'line-cap': 'round' },
        paint: { 'line-color': '#7dd3fc', 'line-width': 1.4, 'line-opacity': 0.85, 'line-dasharray': [2, 1.6] } },

      { id: 'layer_eq_cluster', type: 'circle', source: 'events_eq', filter: ['has', 'point_count'],
        paint: {
          'circle-color': '#38bdf8',
          'circle-radius': ['step', ['get', 'point_count'], 11, 25, 15, 100, 20, 500, 26],
          'circle-opacity': 0.75
        } },
      { id: 'layer_eq_cluster_label', type: 'symbol', source: 'events_eq', filter: ['has', 'point_count'],
        layout: { 'text-field': ['get', 'point_count_abbreviated'], 'text-font': ['Noto Sans Regular'], 'text-size': 11 },
        paint: { 'text-color': '#031018' } },
      { id: 'layer_eq_circle', type: 'circle', source: 'events_eq', filter: ['!', ['has', 'point_count']],
        paint: {
          'circle-color': ['step', ['get', 'mag'], '#22c55e', 5.0, '#eab308', 6.5, '#f97316', 7.5, '#ef4444'],
          'circle-radius': ['step', ['get', 'mag'], 5, 5.0, 8, 6.5, 13, 7.5, 20],
          'circle-stroke-color': '#ffffff',
          'circle-stroke-width': 1.5,
          'circle-opacity': 0.9
        } },
      { id: 'layer_eq_label', type: 'symbol', source: 'events_eq', filter: ['!', ['has', 'point_count']],
        layout: {
          'text-field': ['concat', 'M', ['to-string', ['get', 'mag']]],
          'text-font': ['Noto Sans Regular'],
          'text-size': 10.5,
          'text-allow-overlap': false
        },
        paint: { 'text-color': '#ffffff', 'text-halo-color': '#000000', 'text-halo-width': 1.2 } },

      { id: 'layer_fire_cluster', type: 'circle', source: 'events_fire', filter: ['has', 'point_count'],
        paint: { 'circle-color': '#f97316', 'circle-radius': 10, 'circle-opacity': 0.7 } },
      { id: 'layer_fire_circle', type: 'circle', source: 'events_fire', filter: ['!', ['has', 'point_count']],
        paint: {
          'circle-color': '#fb923c', 'circle-radius': 5.5,
          'circle-stroke-color': '#7f1d1d', 'circle-stroke-width': 1.5, 'circle-opacity': 0.95
        } },

      { id: 'layer_volc_cluster', type: 'circle', source: 'events_volc', filter: ['has', 'point_count'],
        paint: { 'circle-color': '#c026d3', 'circle-radius': 10, 'circle-opacity': 0.7 } },
      { id: 'layer_volc_circle', type: 'circle', source: 'events_volc', filter: ['!', ['has', 'point_count']],
        paint: {
          'circle-color': '#e879f9', 'circle-radius': 7,
          'circle-stroke-color': '#ffffff', 'circle-stroke-width': 2, 'circle-opacity': 0.95
        } }
    ])
  };

  var map = new maplibregl.Map({
    container: 'map',
    style: style,
    center: [15, 25],
    zoom: 1.7,
    attributionControl: { compact: true }
  });

  function applyGlobeProjection() {
    try {
      map.setProjection({ type: 'globe' });
      log('globe projection enabled (spec)');
    } catch (e) {
      try {
        map.setProjection('globe');
        log('globe projection enabled (name)');
      } catch (e2) {
        log('globe projection unavailable: ' + e2);
      }
    }
    try {
      map.setSky({
        'sky-color': '#04060c',
        'sky-horizon-blend': 0.7,
        'horizon-color': '#0a1626',
        'horizon-fog-blend': 0.6,
        'fog-color': '#060b14',
        'fog-ground-blend': 0.85
      });
      log('sky enabled');
    } catch (e) { log('sky unavailable: ' + e); }
  }

  var EVENT_LAYERS = ['layer_eq_circle', 'layer_fire_circle', 'layer_volc_circle'];
  var windAnim = null;

  map.on('error', function (e) {
    log('map error: ' + (e && e.error ? e.error.message : 'unknown'));
  });

  map.on('sourcedata', function (e) {
    if (e.sourceId === 'ofm' && e.isSourceLoaded) log('ofm vector tiles loaded');
  });


  map.on('load', function () {
    var canvas = map.getCanvas();
    var gl = null;
    try { gl = canvas.getContext('webgl2') || canvas.getContext('webgl'); } catch (e) {}
    log('diag: canvas=' + canvas.width + 'x' + canvas.height +
        ' inner=' + window.innerWidth + 'x' + window.innerHeight +
        ' dpr=' + window.devicePixelRatio +
        ' webgl=' + (gl ? 'ok' : 'MISSING'));
    try { map.resize(); } catch (e) { log('resize failed: ' + e); }
    applyGlobeProjection();
    postCamera();
    setTimeout(postCamera, 1500);
    if (window.AndroidBridge && AndroidBridge.onReady) AndroidBridge.onReady();
  });

  function postCamera() {
    if (!window.AndroidBridge || !AndroidBridge.onCameraIdle) return;
    var c = map.getCenter();
    var b = map.getBounds();
    var w = b.getWest();
    var e = b.getEast();
    var span = e - w;
    if (span <= 0) span += 360;
    if (span >= 359) {
      // Globe view can report a hair over 360 degrees; normalize to a
      // clean full-world bbox centered on the camera longitude so raster
      // overlays do not show a seam.
      span = 360;
      w = c.lng - 180;
      e = c.lng + 180;
    }
    var south = Math.max(-85, Math.min(85, b.getSouth()));
    var north = Math.max(-85, Math.min(85, b.getNorth()));
    if (north - south < 1) { south = -85; north = 85; }
    AndroidBridge.onCameraIdle(c.lat, c.lng, map.getZoom(), w, south, e, north);
  }
  map.on('moveend', postCamera);

  map.on('click', function (e) {
    if (e.point) {
      var feats = [];
      try { feats = map.queryRenderedFeatures(e.point, { layers: EVENT_LAYERS }); } catch (err) { feats = []; }
      if (feats && feats.length > 0) {
        var f = feats[0];
        var src = f.layer.id.indexOf('eq') >= 0 ? 'events_eq' : (f.layer.id.indexOf('fire') >= 0 ? 'events_fire' : 'events_volc');
        if (f.properties && f.properties.cluster) {
          map.getSource(src).getClusterExpansionZoom(f.properties.cluster_id, function (err2, zoom) {
            if (!err2) map.easeTo({ center: f.geometry.coordinates, zoom: zoom + 0.2, duration: 900 });
          });
        } else if (window.AndroidBridge && AndroidBridge.onEventTap) {
          AndroidBridge.onEventTap(JSON.stringify(f.properties || {}));
        }
        return;
      }
    }
    if (window.AndroidBridge && AndroidBridge.onMapTap) {
      AndroidBridge.onMapTap(e.lngLat.lat, e.lngLat.lng);
    }
  });

  function setVis(id, visible) {
    if (!map.getLayer(id)) return;
    map.setLayoutProperty(id, 'visibility', visible ? 'visible' : 'none');
  }

  function baseGroup(name) {
    return name === 'space' ? ['base_'] : (name === 'satellite' ? ['satellite_layer'] : ['osm_layer']);
  }

  window.EarthNow = {
    setBaseStyle: function (name) {
      var groups = { space: ['base_', 'osm_layer', 'satellite_layer'], satellite: ['base_', 'osm_layer', 'satellite_layer'] };
      setVis('osm_layer', name === 'streets');
      setVis('satellite_layer', name === 'satellite');
      var showBase = (name === 'space');
      var ids = Object.keys(map.getStyle().layers.reduce(function (m, l) { m[l.id] = 1; return m; }, {}));
      ids.forEach(function (id) {
        if (id.indexOf('base_') === 0) setVis(id, showBase);
      });
    },
    setLayerVisible: setVis,
    updateImage: function (id, dataUrl, coords) {
      var src = map.getSource(id);
      if (!src) { log('updateImage: no source ' + id); return; }
      try {
        coords = coords.map(function (c) {
          return [c[0], Math.max(-85, Math.min(85, c[1]))];
        });
        src.updateImage({ url: dataUrl, coordinates: coords });
        log('updateImage ' + id + ' applied');
      } catch (e) { log('updateImage ' + id + ': ' + e); }
    },
    updateEvents: function (id, geojson) {
      var src = map.getSource(id);
      if (!src) { log('updateEvents: no source ' + id); return; }
      try {
        var data = typeof geojson === 'string' ? JSON.parse(geojson) : geojson;
        src.setData(data);
        log('updateEvents ' + id + ': ' + (data.features ? data.features.length : 0) + ' features');
      } catch (e) { log('updateEvents ' + id + ': ' + e); }
    },
    updateRadar: function (url) {
      var src = map.getSource('radar_tiles');
      if (!src) return;
      try { src.setTiles([url]); } catch (e) { log('updateRadar: ' + e); }
    },
    updateWind: function (geojson) {
      var src = map.getSource('wind_lines');
      if (!src) return;
      try { src.setData(typeof geojson === 'string' ? JSON.parse(geojson) : geojson); } catch (e) { log('updateWind: ' + e); }
    },
    setWindAnimated: function (animate) {
      if (windAnim) { clearInterval(windAnim); windAnim = null; }
      if (!animate) return;
      var offset = 0;
      windAnim = setInterval(function () {
        if (!map.getLayer('layer_wind')) return;
        offset = (offset + 0.55) % 6.5;
        map.setPaintProperty('layer_wind', 'line-dasharray', [1.6, 1.4]);
        map.setPaintProperty('layer_wind', 'line-dasharray', [2 - offset % 2, 1.5]);
      }, 120);
    },
    updateDayNight: function (geojson, opacity) {
      var src = map.getSource('day_night');
      if (!src) return;
      try { src.setData(typeof geojson === 'string' ? JSON.parse(geojson) : geojson); } catch (e) { log('updateDayNight: ' + e); }
      if (map.getLayer('layer_day_night')) {
        map.setPaintProperty('layer_day_night', 'fill-opacity', opacity);
      }
    },
    flyTo: function (lat, lng, zoom) {
      map.flyTo({ center: [lng, lat], zoom: zoom || 5, duration: 1600, essential: true });
    },
    setDayNightOpacity: function (opacity) {
      if (map.getLayer('layer_day_night')) {
        map.setPaintProperty('layer_day_night', 'fill-opacity', opacity);
      }
    },
    getZoom: function () { return map.getZoom(); },
    debugState: function () {
      var out = [];
      ['raster_temp', 'raster_clouds', 'raster_ocean', 'raster_aurora'].forEach(function (id) {
        var layerId = 'layer_' + id;
        var layer = map.getLayer(layerId);
        var src = map.getSource(id);
        out.push(id + ': layer=' + (layer ? 'yes' : 'no') +
          ' vis=' + (layer ? map.getLayoutProperty(layerId, 'visibility') : '-') +
          ' src=' + (src ? 'yes' : 'no'));
      });
      log('debugState ' + out.join(' | '));
    }
  };
})();
