import React from 'react';

export default function DeviceCard({ device }) {
  if (!device) return null;

  const isOnline =
    device.status === 'ONLINE' || device.status === 'ON';
  const subtype = (device.deviceSubtype || 'UNKNOWN').toUpperCase();

  const getSubtypeBadgeColor = () => {
    switch (subtype) {
      case 'HUB':
        return 'bg-amber-950/60 text-amber-300 border-amber-800/60';
      case 'SMART_LIGHT':
        return 'bg-yellow-950/60 text-yellow-300 border-yellow-800/60';
      case 'THERMOSTAT':
        return 'bg-orange-950/60 text-orange-300 border-orange-800/60';
      case 'TEMPERATURE_SENSOR':
        return 'bg-rose-950/60 text-rose-300 border-rose-800/60';
      case 'MOTION_SENSOR':
        return 'bg-blue-950/60 text-blue-300 border-blue-800/60';
      case 'CAMERA':
        return 'bg-emerald-950/60 text-emerald-300 border-emerald-800/60';
      default:
        return 'bg-[#2E2520] text-[#E6D9CD] border-[#3E322A]';
    }
  };

  const getIcon = () => {
    switch (subtype) {
      case 'HUB':
        return '⬡';
      case 'SMART_LIGHT':
        return '☼';
      case 'THERMOSTAT':
        return '◉';
      case 'TEMPERATURE_SENSOR':
        return '♨';
      case 'MOTION_SENSOR':
        return '⌁';
      case 'CAMERA':
        return '◌';
      default:
        return '❖';
    }
  };

  return (
    <div className="flex flex-col justify-between p-5 rounded-2xl bg-[#1B1613] border border-[#2E2520] hover:border-[#C88242]/70 transition-all duration-300 hover:shadow-xl group">
      {/* Top Bar: Icon + Status */}
      <div>
        <div className="flex items-center justify-between mb-3">
          <div className="flex items-center gap-2.5">
            <span className="w-8 h-8 rounded-lg bg-[#261F1A] border border-[#3E322A] flex items-center justify-center text-base text-[#C88242]">
              {getIcon()}
            </span>
            <span
              className={`text-[10px] font-semibold tracking-wider uppercase px-2 py-0.5 rounded border ${getSubtypeBadgeColor()}`}
            >
              {subtype.replace('_', ' ')}
            </span>
          </div>

          <div className="flex items-center gap-1.5">
            <span
              className={`w-2 h-2 rounded-full ${
                isOnline ? 'bg-emerald-400 shadow-[0_0_8px_rgba(52,211,153,0.6)] animate-pulse' : 'bg-red-400'
              }`}
            ></span>
            <span
              className={`text-[11px] font-medium tracking-wide uppercase ${
                isOnline ? 'text-emerald-400' : 'text-red-400'
              }`}
            >
              {device.status || 'UNKNOWN'}
            </span>
          </div>
        </div>

        {/* Device Name */}
        <h4 className="text-base font-medium text-[#FBF9F5] mb-1 group-hover:text-[#F3BA82] transition-colors">
          {device.deviceName}
        </h4>

        {device.roomName && (
          <p className="text-xs text-[#A8988B] mb-3">
            Room: {device.roomName} {device.floorNumber != null ? `(Floor ${device.floorNumber})` : ''}
          </p>
        )}

        {/* Subtype-Specific Metrics */}
        <div className="mt-3 pt-3 border-t border-[#2E2520] space-y-2">
          {/* Smart Light */}
          {subtype === 'SMART_LIGHT' && (
            <div className="grid grid-cols-2 gap-2 text-xs">
              <div>
                <span className="text-[#A8988B] block text-[10px] uppercase">Brightness</span>
                <span className="text-[#FBF9F5] font-medium">
                  {device.brightness != null ? `${device.brightness}%` : 'N/A'}
                </span>
              </div>
              <div>
                <span className="text-[#A8988B] block text-[10px] uppercase">Color Support</span>
                <span className="text-[#FBF9F5] font-medium">
                  {device.colorSupport || 'Standard'}
                </span>
              </div>
            </div>
          )}

          {/* Thermostat */}
          {subtype === 'THERMOSTAT' && (
            <div className="grid grid-cols-2 gap-2 text-xs">
              <div>
                <span className="text-[#A8988B] block text-[10px] uppercase">Target Temp</span>
                <span className="text-[#FBF9F5] font-medium">
                  {device.targetTemperature != null ? `${device.targetTemperature}°` : 'N/A'}
                </span>
              </div>
              <div>
                <span className="text-[#A8988B] block text-[10px] uppercase">Mode</span>
                <span className="text-[#FBF9F5] font-medium uppercase">
                  {device.thermostatMode || 'AUTO'}
                </span>
              </div>
            </div>
          )}

          {/* Temperature Sensor */}
          {subtype === 'TEMPERATURE_SENSOR' && (
            <div className="space-y-1.5 text-xs">
              <div className="flex justify-between items-baseline">
                <span className="text-[#A8988B] text-[10px] uppercase">Latest Reading</span>
                <span className="text-[#FBF9F5] font-semibold text-sm">
                  {device.latestReadingValue != null
                    ? `${device.latestReadingValue} ${device.unit || '°C'}`
                    : 'No readings'}
                </span>
              </div>
              {device.latestReadingTime && (
                <div className="flex justify-between text-[11px] text-[#A8988B]">
                  <span>Recorded:</span>
                  <span>{new Date(device.latestReadingTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</span>
                </div>
              )}
              <div className="flex justify-between text-[11px] text-[#A8988B]">
                <span>Operating Range:</span>
                <span>
                  {device.minRange != null ? device.minRange : '--'} to{' '}
                  {device.maxRange != null ? device.maxRange : '--'} {device.unit || '°C'}
                </span>
              </div>
            </div>
          )}

          {/* Motion Sensor */}
          {subtype === 'MOTION_SENSOR' && (
            <div className="grid grid-cols-2 gap-2 text-xs">
              <div>
                <span className="text-[#A8988B] block text-[10px] uppercase">Sensitivity</span>
                <span className="text-[#FBF9F5] font-medium uppercase">
                  {device.sensitivityLevel || 'MEDIUM'}
                </span>
              </div>
              <div>
                <span className="text-[#A8988B] block text-[10px] uppercase">Detection Range</span>
                <span className="text-[#FBF9F5] font-medium">
                  {device.detectionRange != null ? `${device.detectionRange} m` : 'N/A'}
                </span>
              </div>
            </div>
          )}

          {/* Camera */}
          {subtype === 'CAMERA' && (
            <div className="grid grid-cols-2 gap-2 text-xs">
              <div>
                <span className="text-[#A8988B] block text-[10px] uppercase">Resolution</span>
                <span className="text-[#FBF9F5] font-medium">
                  {device.resolution || '1080p'}
                </span>
              </div>
              <div>
                <span className="text-[#A8988B] block text-[10px] uppercase">Night Vision</span>
                <span className="text-[#FBF9F5] font-medium uppercase">
                  {device.nightVisionSupport || 'YES'}
                </span>
              </div>
            </div>
          )}

          {/* Hub */}
          {subtype === 'HUB' && (
            <div className="space-y-1 text-xs">
              <div className="flex justify-between text-[#A8988B]">
                <span className="text-[10px] uppercase">Role:</span>
                <span className="text-[#FBF9F5] font-medium">Primary Room Coordinator</span>
              </div>
              {device.parentDeviceName && (
                <div className="flex justify-between text-[#A8988B]">
                  <span className="text-[10px] uppercase">Parent:</span>
                  <span className="text-[#FBF9F5]">{device.parentDeviceName}</span>
                </div>
              )}
            </div>
          )}
        </div>
      </div>

      {/* Bottom Uptime from Oracle PL/SQL Function FN_CALCULATE_DEVICE_UPTIME */}
      <div className="mt-4 pt-3 border-t border-[#2E2520]/80 flex items-center justify-between text-[11px] text-[#A8988B]">
        <span className="uppercase tracking-wider text-[10px]">Uptime:</span>
        <span className="font-mono text-[#E6D9CD] truncate max-w-[180px] text-right" title={device.uptime}>
          {device.uptime || 'Unknown'}
        </span>
      </div>
    </div>
  );
}
