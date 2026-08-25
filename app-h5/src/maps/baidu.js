/**
 * 百度地图 SDK 加载与坐标转换工具。
 * 
 * @module maps/baidu
 */

const X_PI = (Math.PI * 3000.0) / 180.0;
const PI = Math.PI;
const A = 6378245.0; // 长半轴
const EE = 0.00669342162296594323; // 偏心率平方

let loadPromise = null;
let baiduMapLoaded = false;

/**
 * WGS84 转 GCJ02（火星坐标）
 */
function wgs84ToGcj02(lng, lat) {
  if (outOfChina(lng, lat)) {
    return [lng, lat];
  }
  let dLat = transformLat(lng - 105.0, lat - 35.0);
  let dLng = transformLng(lng - 105.0, lat - 35.0);
  const radLat = (lat / 180.0) * PI;
  let magic = Math.sin(radLat);
  magic = 1 - EE * magic * magic;
  const sqrtMagic = Math.sqrt(magic);
  dLat = (dLat * 180.0) / (((A * (1 - EE)) / (magic * sqrtMagic)) * PI);
  dLng = (dLng * 180.0) / ((A / sqrtMagic) * Math.cos(radLat) * PI);
  const mgLat = lat + dLat;
  const mgLng = lng + dLng;
  return [mgLng, mgLat];
}

/**
 * GCJ02 转 BD09（百度坐标）
 */
function gcj02ToBd09(lng, lat) {
  const z = Math.sqrt(lng * lng + lat * lat) + 0.00002 * Math.sin(lat * X_PI);
  const theta = Math.atan2(lat, lng) + 0.000003 * Math.cos(lng * X_PI);
  const bdLng = z * Math.cos(theta) + 0.0065;
  const bdLat = z * Math.sin(theta) + 0.006;
  return [bdLng, bdLat];
}

/**
 * BD09 转 GCJ02
 */
function bd09ToGcj02(bdLng, bdLat) {
  const x = bdLng - 0.0065;
  const y = bdLat - 0.006;
  const z = Math.sqrt(x * x + y * y) - 0.00002 * Math.sin(y * X_PI);
  const theta = Math.atan2(y, x) - 0.000003 * Math.cos(x * X_PI);
  const ggLng = z * Math.cos(theta);
  const ggLat = z * Math.sin(theta);
  return [ggLng, ggLat];
}

/**
 * GCJ02 转 WGS84（粗略逆转换）
 */
function gcj02ToWgs84(lng, lat) {
  if (outOfChina(lng, lat)) {
    return [lng, lat];
  }
  let dLat = transformLat(lng - 105.0, lat - 35.0);
  let dLng = transformLng(lng - 105.0, lat - 35.0);
  const radLat = (lat / 180.0) * PI;
  let magic = Math.sin(radLat);
  magic = 1 - EE * magic * magic;
  const sqrtMagic = Math.sqrt(magic);
  dLat = (dLat * 180.0) / (((A * (1 - EE)) / (magic * sqrtMagic)) * PI);
  dLng = (dLng * 180.0) / ((A / sqrtMagic) * Math.cos(radLat) * PI);
  const mgLat = lat - dLat;
  const mgLng = lng - dLng;
  return [mgLng, mgLat];
}

/**
 * BD09 转 WGS84
 */
function bd09ToWgs84(bdLng, bdLat) {
  const [ggLng, ggLat] = bd09ToGcj02(bdLng, bdLat);
  return gcj02ToWgs84(ggLng, ggLat);
}

/**
 * WGS84 转 BD09（百度坐标）- 主要对外接口
 */
export function wgs84ToBd09(lng, lat) {
  const [gcjLng, gcjLat] = wgs84ToGcj02(lng, lat);
  return gcj02ToBd09(gcjLng, gcjLat);
}

/**
 * BD09 转 WGS84 - 用于地图选点后转回存储坐标
 */
export function bd09ToWgs84Coord(bdLng, bdLat) {
  return bd09ToWgs84(bdLng, bdLat);
}

function transformLat(lng, lat) {
  let ret = -100.0 + 2.0 * lng + 3.0 * lat + 0.2 * lat * lat + 0.1 * lng * lat + 0.2 * Math.sqrt(Math.abs(lng));
  ret += ((20.0 * Math.sin(6.0 * lng * PI) + 20.0 * Math.sin(2.0 * lng * PI)) * 2.0) / 3.0;
  ret += ((20.0 * Math.sin(lat * PI) + 40.0 * Math.sin((lat / 3.0) * PI)) * 2.0) / 3.0;
  ret += ((160.0 * Math.sin((lat / 12.0) * PI) + 320 * Math.sin((lat * PI) / 30.0)) * 2.0) / 3.0;
  return ret;
}

function transformLng(lng, lat) {
  let ret = 300.0 + lng + 2.0 * lat + 0.1 * lng * lng + 0.1 * lng * lat + 0.1 * Math.sqrt(Math.abs(lng));
  ret += ((20.0 * Math.sin(6.0 * lng * PI) + 20.0 * Math.sin(2.0 * lng * PI)) * 2.0) / 3.0;
  ret += ((20.0 * Math.sin(lng * PI) + 40.0 * Math.sin((lng / 3.0) * PI)) * 2.0) / 3.0;
  ret += ((150.0 * Math.sin((lng / 12.0) * PI) + 300.0 * Math.sin((lng / 30.0) * PI)) * 2.0) / 3.0;
  return ret;
}

function outOfChina(lng, lat) {
  return lng < 72.004 || lng > 137.8347 || lat < 0.8293 || lat > 55.8271;
}

/**
 * 动态加载百度地图 JS SDK。
 * 
 * @param {string} ak - 百度地图 AK
 * @returns {Promise<BMap>} 百度地图 API 对象
 */
export function loadBaiduMap(ak) {
  if (baiduMapLoaded && window.BMap) {
    return Promise.resolve(window.BMap);
  }
  if (loadPromise) {
    return loadPromise;
  }

  loadPromise = new Promise((resolve, reject) => {
    const callbackName = '__bmap_init_' + Date.now();
    window[callbackName] = () => {
      baiduMapLoaded = true;
      delete window[callbackName];
      resolve(window.BMap);
    };

    const script = document.createElement('script');
    script.src = `https://api.map.baidu.com/api?v=3.0&ak=${encodeURIComponent(ak)}&callback=${callbackName}`;
    script.onerror = () => {
      delete window[callbackName];
      loadPromise = null;
      reject(new Error('百度地图 SDK 加载失败'));
    };
    document.head.appendChild(script);
  });

  return loadPromise;
}
