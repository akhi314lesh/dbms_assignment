import { useState, useEffect, useMemo, useCallback } from 'react';
import api from '../services/api';
import { useAuth } from '../context/AuthContext';

/**
 * VisualCrudManager
 * A no-code, form-driven interface for adding, updating, deleting,
 * and viewing database records without requiring any SQL knowledge.
 */
export default function VisualCrudManager({ homeId }) {
  const { oracleUser } = useAuth();
  const currentUserId = oracleUser?.userId || 1;
  const currentHomeId = homeId || 1;

  // Active Entity Tab
  const [activeEntity, setActiveEntity] = useState('DEVICES');
  const [data, setData] = useState([]);
  const [loading, setLoading] = useState(false);
  const [actionLoading, setActionLoading] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [feedback, setFeedback] = useState(null); // { type: 'success' | 'error', message, sql, elapsed }

  // Contextual dropdown data
  const [roomsList, setRoomsList] = useState([]);
  const [devicesList, setDevicesList] = useState([]);

  // Modal State
  const [modalMode, setModalMode] = useState(null); // 'ADD' | 'EDIT' | 'DELETE' | null
  const [selectedRecord, setSelectedRecord] = useState(null);
  const [formData, setFormData] = useState({});
  const [showSqlPreview, setShowSqlPreview] = useState(false);

  const entities = [
    { id: 'DEVICES', label: 'Devices', icon: '📱', desc: 'Hardware & IoT controllers' },
    { id: 'ROOMS', label: 'Rooms', icon: '🚪', desc: 'Home partitions & floors' },
    { id: 'SMART_LIGHTS', label: 'Smart Lights', icon: '💡', desc: 'Dimmable & RGB lighting' },
    { id: 'THERMOSTATS', label: 'Thermostats', icon: '🌡️', desc: 'Climate control & modes' },
    { id: 'SENSOR_READINGS', label: 'Sensor Readings', icon: '📊', desc: 'Live telemetric data' },
    { id: 'ALERTS', label: 'Alerts', icon: '🚨', desc: 'Real-time notifications' },
    { id: 'AUTOMATION_RULES', label: 'Automation Rules', icon: '⚙️', desc: 'Smart rule triggers' },
  ];

  // Helper to escape single quotes in SQL strings
  const esc = (val) => (val == null ? '' : String(val).replace(/'/g, "''"));

  // 1. Fetch contextual dropdowns (Rooms & Devices for current home)
  const fetchDropdownContext = useCallback(async () => {
    try {
      const [roomsRes, devRes] = await Promise.all([
        api.post('/api/sql-console/execute', {
          sql: `SELECT room_id, room_name, floor_number FROM rooms WHERE home_id = ${currentHomeId} ORDER BY floor_number, room_name`,
          homeId: currentHomeId,
        }),
        api.post('/api/sql-console/execute', {
          sql: `SELECT d.device_id, d.device_name, d.device_subtype FROM devices d JOIN rooms r ON d.room_id = r.room_id WHERE r.home_id = ${currentHomeId} ORDER BY d.device_name`,
          homeId: currentHomeId,
        }),
      ]);

      if (roomsRes.data?.success && roomsRes.data?.rows) {
        setRoomsList(roomsRes.data.rows);
      }
      if (devRes.data?.success && devRes.data?.rows) {
        setDevicesList(devRes.data.rows);
      }
    } catch (err) {
      console.warn('Failed to fetch dropdown context', err);
    }
  }, [currentHomeId]);

  // 2. Fetch records for the selected entity
  const fetchEntityData = useCallback(async () => {
    setLoading(true);
    setFeedback(null);
    let query = '';

    switch (activeEntity) {
      case 'DEVICES':
        query = `SELECT d.device_id, d.device_name, d.device_subtype, d.status, r.room_name, d.room_id, d.parent_device_id, d.install_date FROM devices d JOIN rooms r ON d.room_id = r.room_id WHERE r.home_id = ${currentHomeId} ORDER BY d.device_id DESC`;
        break;
      case 'ROOMS':
        query = `SELECT room_id, room_name, floor_number, home_id FROM rooms WHERE home_id = ${currentHomeId} ORDER BY floor_number, room_name`;
        break;
      case 'SMART_LIGHTS':
        query = `SELECT l.device_id, d.device_name, l.brightness, l.color_support, r.room_name FROM smart_lights l JOIN devices d ON l.device_id = d.device_id JOIN rooms r ON d.room_id = r.room_id WHERE r.home_id = ${currentHomeId} ORDER BY l.device_id DESC`;
        break;
      case 'THERMOSTATS':
        query = `SELECT t.device_id, d.device_name, t.target_temperature, t."MODE" as mode_val, r.room_name FROM thermostats t JOIN devices d ON t.device_id = d.device_id JOIN rooms r ON d.room_id = r.room_id WHERE r.home_id = ${currentHomeId} ORDER BY t.device_id DESC`;
        break;
      case 'SENSOR_READINGS':
        query = `SELECT s.reading_id, s.device_id, d.device_name, s.value, s.reading_time FROM sensor_readings s JOIN devices d ON s.device_id = d.device_id JOIN rooms r ON d.room_id = r.room_id WHERE r.home_id = ${currentHomeId} ORDER BY s.reading_time DESC FETCH FIRST 30 ROWS ONLY`;
        break;
      case 'ALERTS':
        query = `SELECT a.alert_id, a.device_id, d.device_name, a.message, a.status, a.alert_time FROM alerts a JOIN devices d ON a.device_id = d.device_id JOIN rooms r ON d.room_id = r.room_id WHERE r.home_id = ${currentHomeId} ORDER BY a.alert_time DESC`;
        break;
      case 'AUTOMATION_RULES':
        query = `SELECT r.rule_id, r.rule_name, r.condition_operator, r.condition_value, d.device_name as condition_device, r.condition_device_id FROM automation_rules r JOIN devices d ON r.condition_device_id = d.device_id JOIN rooms rm ON d.room_id = rm.room_id WHERE rm.home_id = ${currentHomeId} ORDER BY r.rule_id DESC`;
        break;
      default:
        break;
    }

    try {
      const res = await api.post('/api/sql-console/execute', {
        sql: query,
        homeId: currentHomeId,
      });

      if (res.data?.success) {
        setData(res.data.rows || []);
      } else {
        setFeedback({
          type: 'error',
          message: res.data?.message || 'Failed to load records.',
        });
      }
    } catch (err) {
      console.error('Failed to load table data', err);
      setFeedback({
        type: 'error',
        message: err.response?.data?.message || 'Server error loading table data.',
      });
    } finally {
      setLoading(false);
    }
  }, [activeEntity, currentHomeId]);

  useEffect(() => {
    // Run after the effect returns (avoids synchronous setState inside the effect body)
    let active = true;
    Promise.resolve().then(() => {
      if (!active) return;
      fetchDropdownContext();
      fetchEntityData();
    });
    return () => { active = false; };
  }, [fetchDropdownContext, fetchEntityData]);

  // Filtered rows by search query
  const filteredData = useMemo(() => {
    if (!searchQuery.trim()) return data;
    const q = searchQuery.toLowerCase();
    return data.filter((row) =>
      Object.values(row).some((val) =>
        val != null && String(val).toLowerCase().includes(q)
      )
    );
  }, [data, searchQuery]);

  // Open Add Modal with sensible defaults
  const handleOpenAdd = () => {
    setModalMode('ADD');
    setSelectedRecord(null);

    const defaultRoomId = roomsList[0]?.ROOM_ID || 1;
    const defaultDevId = devicesList[0]?.DEVICE_ID || 1;

    switch (activeEntity) {
      case 'DEVICES':
        setFormData({
          device_name: '',
          device_subtype: 'SMART_LIGHT',
          status: 'ONLINE',
          room_id: defaultRoomId,
          parent_device_id: '',
        });
        break;
      case 'ROOMS':
        setFormData({
          room_name: '',
          floor_number: 1,
        });
        break;
      case 'SMART_LIGHTS':
        setFormData({
          device_id: defaultDevId,
          brightness: 80,
          color_support: 'YES',
        });
        break;
      case 'THERMOSTATS':
        setFormData({
          device_id: defaultDevId,
          target_temperature: 22.0,
          mode_val: 'HEAT',
        });
        break;
      case 'SENSOR_READINGS':
        setFormData({
          device_id: defaultDevId,
          value: 23.5,
        });
        break;
      case 'ALERTS':
        setFormData({
          device_id: defaultDevId,
          message: '',
          status: 'ACTIVE',
        });
        break;
      case 'AUTOMATION_RULES':
        setFormData({
          rule_name: '',
          condition_device_id: defaultDevId,
          condition_operator: '>',
          condition_value: '25.0',
        });
        break;
      default:
        setFormData({});
    }
  };

  // Open Edit Modal
  const handleOpenEdit = (record) => {
    setModalMode('EDIT');
    setSelectedRecord(record);
    setFormData({ ...record });
  };

  // Open Delete Modal
  const handleOpenDelete = (record) => {
    setModalMode('DELETE');
    setSelectedRecord(record);
  };

  // Close any active modal
  const handleCloseModal = () => {
    setModalMode(null);
    setSelectedRecord(null);
    setFormData({});
  };

  // Generate SQL statement based on modalMode, activeEntity, and formData
  const generateSql = () => {
    if (modalMode === 'DELETE' && selectedRecord) {
      switch (activeEntity) {
        case 'DEVICES':
          return `DELETE FROM devices WHERE device_id = ${selectedRecord.DEVICE_ID}`;
        case 'ROOMS':
          return `DELETE FROM rooms WHERE room_id = ${selectedRecord.ROOM_ID}`;
        case 'SMART_LIGHTS':
          return `DELETE FROM smart_lights WHERE device_id = ${selectedRecord.DEVICE_ID}`;
        case 'THERMOSTATS':
          return `DELETE FROM thermostats WHERE device_id = ${selectedRecord.DEVICE_ID}`;
        case 'SENSOR_READINGS':
          return `DELETE FROM sensor_readings WHERE device_id = ${selectedRecord.DEVICE_ID} AND reading_id = ${selectedRecord.READING_ID}`;
        case 'ALERTS':
          return `DELETE FROM alerts WHERE alert_id = ${selectedRecord.ALERT_ID}`;
        case 'AUTOMATION_RULES':
          return `DELETE FROM automation_rules WHERE rule_id = ${selectedRecord.RULE_ID}`;
        default:
          return '';
      }
    }

    if (modalMode === 'ADD') {
      switch (activeEntity) {
        case 'DEVICES': {
          const parentVal = formData.parent_device_id ? formData.parent_device_id : 'NULL';
          return `INSERT INTO devices (device_id, device_name, device_subtype, status, install_date, last_restart_time, room_id, parent_device_id) VALUES (seq_device_id.NEXTVAL, '${esc(formData.device_name)}', '${esc(formData.device_subtype)}', '${esc(formData.status)}', SYSDATE, SYSTIMESTAMP, ${formData.room_id}, ${parentVal})`;
        }
        case 'ROOMS':
          return `INSERT INTO rooms (room_id, room_name, floor_number, home_id) VALUES (seq_room_id.NEXTVAL, '${esc(formData.room_name)}', ${formData.floor_number || 1}, ${currentHomeId})`;
        case 'SMART_LIGHTS':
          return `INSERT INTO smart_lights (device_id, brightness, color_support) VALUES (${formData.device_id}, ${formData.brightness || 80}, '${esc(formData.color_support || 'YES')}')`;
        case 'THERMOSTATS':
          return `INSERT INTO thermostats (device_id, target_temperature, "MODE") VALUES (${formData.device_id}, ${formData.target_temperature || 22}, '${esc(formData.mode_val || 'HEAT')}')`;
        case 'SENSOR_READINGS':
          return `INSERT INTO sensor_readings (device_id, reading_id, value, reading_time) VALUES (${formData.device_id}, seq_reading_id.NEXTVAL, ${formData.value || 0}, SYSTIMESTAMP)`;
        case 'ALERTS':
          return `INSERT INTO alerts (alert_id, device_id, category_id, message, alert_time, status) VALUES (seq_alert_id.NEXTVAL, ${formData.device_id}, 1, '${esc(formData.message)}', SYSTIMESTAMP, '${esc(formData.status || 'ACTIVE')}')`;
        case 'AUTOMATION_RULES':
          return `INSERT INTO automation_rules (rule_id, rule_name, condition_operator, condition_value, condition_device_id, created_by_user_id, created_date) VALUES (seq_rule_id.NEXTVAL, '${esc(formData.rule_name)}', '${esc(formData.condition_operator)}', '${esc(formData.condition_value)}', ${formData.condition_device_id}, ${currentUserId}, SYSDATE)`;
        default:
          return '';
      }
    }

    if (modalMode === 'EDIT' && selectedRecord) {
      switch (activeEntity) {
        case 'DEVICES': {
          const parentVal = formData.parent_device_id ? formData.parent_device_id : 'NULL';
          return `UPDATE devices SET device_name = '${esc(formData.device_name || formData.DEVICE_NAME)}', status = '${esc(formData.status || formData.STATUS)}', room_id = ${formData.room_id || formData.ROOM_ID}, parent_device_id = ${parentVal} WHERE device_id = ${selectedRecord.DEVICE_ID}`;
        }
        case 'ROOMS':
          return `UPDATE rooms SET room_name = '${esc(formData.room_name || formData.ROOM_NAME)}', floor_number = ${formData.floor_number || formData.FLOOR_NUMBER} WHERE room_id = ${selectedRecord.ROOM_ID}`;
        case 'SMART_LIGHTS':
          return `UPDATE smart_lights SET brightness = ${formData.brightness ?? formData.BRIGHTNESS}, color_support = '${esc(formData.color_support || formData.COLOR_SUPPORT)}' WHERE device_id = ${selectedRecord.DEVICE_ID}`;
        case 'THERMOSTATS':
          return `UPDATE thermostats SET target_temperature = ${formData.target_temperature ?? formData.TARGET_TEMPERATURE}, "MODE" = '${esc(formData.mode_val || formData.MODE_VAL || 'HEAT')}' WHERE device_id = ${selectedRecord.DEVICE_ID}`;
        case 'ALERTS':
          return `UPDATE alerts SET status = '${esc(formData.status || formData.STATUS)}', message = '${esc(formData.message || formData.MESSAGE)}' WHERE alert_id = ${selectedRecord.ALERT_ID}`;
        case 'AUTOMATION_RULES':
          return `UPDATE automation_rules SET rule_name = '${esc(formData.rule_name || formData.RULE_NAME)}', condition_operator = '${esc(formData.condition_operator || formData.CONDITION_OPERATOR)}', condition_value = '${esc(formData.condition_value || formData.CONDITION_VALUE)}' WHERE rule_id = ${selectedRecord.RULE_ID}`;
        default:
          return '';
      }
    }

    return '';
  };

  // Execute Form Action
  const handleExecuteAction = async () => {
    const sql = generateSql();
    if (!sql) return;

    setActionLoading(true);
    setFeedback(null);

    try {
      const res = await api.post('/api/sql-console/execute', {
        sql,
        homeId: currentHomeId,
      });

      if (res.data?.success) {
        setFeedback({
          type: 'success',
          message: `Operation successful: ${res.data.affectedRows ?? 1} row affected in ${res.data.executionTimeMs}ms.`,
          sql,
          elapsed: res.data.executionTimeMs,
        });
        handleCloseModal();
        await fetchEntityData();
        await fetchDropdownContext();
      } else {
        setFeedback({
          type: 'error',
          message: res.data?.message || 'Database rejected the modification.',
          sql,
        });
      }
    } catch (err) {
      console.error('Action failed', err);
      setFeedback({
        type: 'error',
        message: err.response?.data?.message || 'Server error while executing operation.',
        sql,
      });
    } finally {
      setActionLoading(false);
    }
  };

  return (
    <div className="w-full flex flex-col gap-5">
      {/* Entity Selector Tabs */}
      <div className="flex items-center gap-2 overflow-x-auto pb-2 border-b border-[#2E2520] custom-scrollbar">
        {entities.map((ent) => {
          const isActive = activeEntity === ent.id;
          return (
            <button
              key={ent.id}
              onClick={() => {
                setActiveEntity(ent.id);
                setSearchQuery('');
                setFeedback(null);
              }}
              className={`flex items-center gap-2.5 px-3.5 py-2 rounded-xl text-xs font-medium whitespace-nowrap transition-all border ${
                isActive
                  ? 'bg-gradient-to-r from-[#C88242]/20 to-[#A3642E]/20 border-[#C88242] text-[#FBF9F5] shadow-sm'
                  : 'bg-[#1A1410] border-[#3E322A]/70 text-[#A8988B] hover:text-[#E6D9CD] hover:border-[#524338]'
              }`}
            >
              <span className="text-sm">{ent.icon}</span>
              <span>{ent.label}</span>
            </button>
          );
        })}
      </div>

      {/* Control Bar: Search, Add Button, Refresh */}
      <div className="flex flex-wrap items-center justify-between gap-3">
        {/* Left: Search input */}
        <div className="relative flex-1 min-w-[240px] max-w-md">
          <span className="absolute left-3 top-1/2 -translate-y-1/2 text-xs text-[#A8988B]">
            🔍
          </span>
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder={`Search ${entities.find((e) => e.id === activeEntity)?.label.toLowerCase()}...`}
            className="w-full pl-9 pr-4 py-2 bg-[#1B1613] border border-[#3E322A] rounded-xl text-xs text-[#FBF9F5] placeholder-[#6E5D52] focus:outline-none focus:border-[#C88242] transition-colors"
          />
          {searchQuery && (
            <button
              onClick={() => setSearchQuery('')}
              className="absolute right-3 top-1/2 -translate-y-1/2 text-[10px] text-[#A8988B] hover:text-[#FBF9F5]"
            >
              ✕
            </button>
          )}
        </div>

        {/* Right: Actions */}
        <div className="flex items-center gap-2.5">
          <span className="text-[11px] text-[#A8988B] font-mono mr-1">
            {filteredData.length} {filteredData.length === 1 ? 'record' : 'records'}
          </span>

          <button
            onClick={fetchEntityData}
            disabled={loading}
            title="Refresh table"
            className="p-2 rounded-xl bg-[#1B1613] border border-[#3E322A] text-[#A8988B] hover:text-[#FBF9F5] hover:border-[#C88242] transition-colors disabled:opacity-50"
          >
            <span className={`inline-block ${loading ? 'animate-spin' : ''}`}>🔄</span>
          </button>

          <button
            onClick={handleOpenAdd}
            className="flex items-center gap-2 px-4 py-2 rounded-xl bg-gradient-to-r from-[#C88242] to-[#A3642E] text-white text-xs font-semibold uppercase tracking-wider shadow-md hover:brightness-110 active:scale-[0.98] transition-all"
          >
            <span>+</span>
            <span>Add {entities.find((e) => e.id === activeEntity)?.label.slice(0, -1) || 'Record'}</span>
          </button>
        </div>
      </div>

      {/* Feedback Banner */}
      {feedback && (
        <div
          className={`p-3.5 rounded-xl border text-xs font-mono flex flex-col gap-1.5 transition-all ${
            feedback.type === 'success'
              ? 'bg-emerald-950/40 border-emerald-800/60 text-emerald-200'
              : 'bg-red-950/40 border-red-800/60 text-red-200'
          }`}
        >
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2 font-semibold">
              <span>{feedback.type === 'success' ? '✓' : '⚠'}</span>
              <span>{feedback.type === 'success' ? 'Database Updated' : 'Action Failed'}</span>
            </div>
            <button
              onClick={() => setFeedback(null)}
              className="text-[10px] opacity-70 hover:opacity-100"
            >
              ✕
            </button>
          </div>
          <p className="font-sans text-xs">{feedback.message}</p>
          {feedback.sql && (
            <div className="mt-1 pt-1.5 border-t border-white/10 text-[10px] opacity-75 truncate">
              <span className="text-[#C88242] font-semibold">Executed SQL: </span>
              <span>{feedback.sql}</span>
            </div>
          )}
        </div>
      )}

      {/* Main Table */}
      <div className="rounded-xl border border-[#2E2520] bg-[#140F0C] overflow-hidden">
        {loading ? (
          <div className="py-16 flex flex-col items-center justify-center gap-2 text-[#A8988B]">
            <div className="w-6 h-6 border-2 border-[#C88242] border-t-transparent rounded-full animate-spin"></div>
            <span className="text-xs uppercase tracking-wider">Querying Oracle 21c...</span>
          </div>
        ) : filteredData.length === 0 ? (
          <div className="py-12 text-center text-[#A8988B] text-xs">
            <p className="mb-2">No records found for {entities.find((e) => e.id === activeEntity)?.label}.</p>
            <button
              onClick={handleOpenAdd}
              className="text-[#C88242] underline hover:text-[#E89C57]"
            >
              Click here to add the first record
            </button>
          </div>
        ) : (
          <div className="overflow-x-auto max-h-[460px] overflow-y-auto custom-scrollbar">
            <table className="w-full text-left text-xs border-collapse">
              <thead>
                <tr className="border-b border-[#2E2520] bg-[#1B1613] text-[#C88242] font-mono uppercase tracking-wider text-[10px]">
                  {renderTableHeaders(activeEntity)}
                  <th className="py-2.5 px-4 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[#2E2520]/60">
                {filteredData.map((row, idx) => (
                  <tr
                    key={idx}
                    className="hover:bg-[#1F1813]/60 transition-colors text-[#FBF9F5]"
                  >
                    {renderTableRowCells(activeEntity, row)}
                    <td className="py-2 px-4 text-right whitespace-nowrap">
                      <div className="flex items-center justify-end gap-1.5">
                        {canEditEntity(activeEntity) && (
                          <button
                            onClick={() => handleOpenEdit(row)}
                            className="px-2.5 py-1 rounded-lg bg-[#261F1A] border border-[#3E322A] text-[11px] text-[#E6D9CD] hover:border-[#C88242] hover:text-[#C88242] transition-colors"
                          >
                            ✏️ Edit
                          </button>
                        )}
                        <button
                          onClick={() => handleOpenDelete(row)}
                          className="px-2.5 py-1 rounded-lg bg-[#261F1A] border border-[#3E322A] text-[11px] text-red-400 hover:border-red-600 hover:bg-red-950/30 transition-colors"
                        >
                          🗑️ Delete
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Educational SQL Preview Footer */}
      <div className="flex items-center justify-between text-[11px] text-[#A8988B] px-1">
        <span>
          💡 <strong className="text-[#E6D9CD]">Zero-SQL Operations:</strong> Add, edit, or remove entries using pure interactive forms. All actions execute directly on Oracle 21c schema tables.
        </span>
      </div>

      {/* =========================================================================
          ADD / EDIT MODAL
          ========================================================================= */}
      {(modalMode === 'ADD' || modalMode === 'EDIT') && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-fadeIn"
          onClick={handleCloseModal}
        >
          <div
            className="relative w-full max-w-lg bg-[#18120E] border border-[#3E322A] rounded-2xl shadow-2xl p-6 text-[#FBF9F5]"
            onClick={(e) => e.stopPropagation()}
          >
            {/* Modal Header */}
            <div className="flex items-center justify-between pb-3.5 mb-4 border-b border-[#2E2520]">
              <div className="flex items-center gap-2">
                <span className="text-base">
                  {modalMode === 'ADD' ? '➕' : '✏️'}
                </span>
                <h3 className="text-sm font-semibold tracking-wide text-[#FBF9F5]">
                  {modalMode === 'ADD'
                    ? `Add New ${entities.find((e) => e.id === activeEntity)?.label.slice(0, -1)}`
                    : `Edit ${entities.find((e) => e.id === activeEntity)?.label.slice(0, -1)}`}
                </h3>
              </div>
              <button
                onClick={handleCloseModal}
                className="text-[#A8988B] hover:text-[#FBF9F5] text-sm"
              >
                ✕
              </button>
            </div>

            {/* Dynamic Form Content */}
            <div className="space-y-3.5">
              {renderFormFields({
                activeEntity,
                modalMode,
                formData,
                setFormData,
                roomsList,
                devicesList,
              })}

              {/* Collapsible SQL Preview for transparency */}
              <div className="pt-2">
                <button
                  type="button"
                  onClick={() => setShowSqlPreview(!showSqlPreview)}
                  className="text-[10px] uppercase font-mono tracking-wider text-[#C88242] hover:underline flex items-center gap-1"
                >
                  <span>{showSqlPreview ? '▼ Hide' : '▶ Show'} Generated SQL Preview</span>
                </button>
                {showSqlPreview && (
                  <pre className="mt-2 p-2.5 rounded-lg bg-[#0D0A08] border border-[#2E2520] font-mono text-[11px] text-emerald-400 overflow-x-auto whitespace-pre-wrap leading-relaxed">
                    {generateSql()}
                  </pre>
                )}
              </div>
            </div>

            {/* Modal Buttons */}
            <div className="mt-6 flex items-center justify-end gap-2.5 pt-3 border-t border-[#2E2520]">
              <button
                type="button"
                onClick={handleCloseModal}
                className="px-4 py-2 rounded-xl bg-[#261F1A] border border-[#3E322A] text-xs text-[#A8988B] hover:text-[#FBF9F5] transition-colors"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={handleExecuteAction}
                disabled={actionLoading}
                className="px-5 py-2 rounded-xl bg-gradient-to-r from-[#C88242] to-[#A3642E] text-white text-xs font-semibold uppercase tracking-wider shadow-lg hover:brightness-110 active:scale-[0.98] disabled:opacity-50 transition-all flex items-center gap-2"
              >
                {actionLoading ? (
                  <>
                    <div className="w-3 h-3 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                    <span>Saving...</span>
                  </>
                ) : (
                  <span>{modalMode === 'ADD' ? 'Save & Insert' : 'Save Changes'}</span>
                )}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* =========================================================================
          DELETE CONFIRMATION MODAL
          ========================================================================= */}
      {modalMode === 'DELETE' && selectedRecord && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-fadeIn"
          onClick={handleCloseModal}
        >
          <div
            className="relative w-full max-w-md bg-[#18120E] border border-red-900/60 rounded-2xl shadow-2xl p-6 text-[#FBF9F5]"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="flex items-center gap-3 text-red-400 mb-3">
              <span className="text-2xl">⚠️</span>
              <h3 className="text-sm font-semibold tracking-wide">
                Confirm Deletion
              </h3>
            </div>

            <p className="text-xs text-[#E6D9CD] leading-relaxed mb-4">
              Are you sure you want to delete this record? This change is permanent and will execute immediately on Oracle Database.
            </p>

            <div className="p-3 rounded-xl bg-[#120D0A] border border-[#3E322A] font-mono text-xs text-[#A8988B] space-y-1 mb-4">
              <div>
                <strong className="text-[#FBF9F5]">Entity:</strong> {activeEntity}
              </div>
              <div>
                <strong className="text-[#FBF9F5]">Target ID:</strong>{' '}
                {selectedRecord.DEVICE_ID ||
                  selectedRecord.ROOM_ID ||
                  selectedRecord.ALERT_ID ||
                  selectedRecord.RULE_ID ||
                  selectedRecord.READING_ID}
              </div>
              {selectedRecord.DEVICE_NAME && (
                <div>
                  <strong className="text-[#FBF9F5]">Name:</strong> {selectedRecord.DEVICE_NAME}
                </div>
              )}
              {selectedRecord.ROOM_NAME && (
                <div>
                  <strong className="text-[#FBF9F5]">Room:</strong> {selectedRecord.ROOM_NAME}
                </div>
              )}
            </div>

            {/* Modal Buttons */}
            <div className="flex items-center justify-end gap-2.5 pt-2 border-t border-[#2E2520]">
              <button
                type="button"
                onClick={handleCloseModal}
                className="px-4 py-2 rounded-xl bg-[#261F1A] border border-[#3E322A] text-xs text-[#A8988B] hover:text-[#FBF9F5] transition-colors"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={handleExecuteAction}
                disabled={actionLoading}
                className="px-5 py-2 rounded-xl bg-red-600 hover:bg-red-500 text-white text-xs font-semibold uppercase tracking-wider shadow-lg active:scale-[0.98] disabled:opacity-50 transition-all flex items-center gap-2"
              >
                {actionLoading ? (
                  <>
                    <div className="w-3 h-3 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                    <span>Deleting...</span>
                  </>
                ) : (
                  <span>Delete Record</span>
                )}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

// ----------------------------------------------------------------------
// HELPER FUNCTIONS & RENDERERS
// ----------------------------------------------------------------------

function canEditEntity(entity) {
  // All can be edited except raw readings which are sensor logs
  return entity !== 'SENSOR_READINGS';
}

function renderTableHeaders(entity) {
  switch (entity) {
    case 'DEVICES':
      return (
        <>
          <th className="py-2.5 px-4">ID</th>
          <th className="py-2.5 px-4">Device Name</th>
          <th className="py-2.5 px-4">Subtype</th>
          <th className="py-2.5 px-4">Status</th>
          <th className="py-2.5 px-4">Room</th>
        </>
      );
    case 'ROOMS':
      return (
        <>
          <th className="py-2.5 px-4">Room ID</th>
          <th className="py-2.5 px-4">Room Name</th>
          <th className="py-2.5 px-4">Floor #</th>
        </>
      );
    case 'SMART_LIGHTS':
      return (
        <>
          <th className="py-2.5 px-4">Device ID</th>
          <th className="py-2.5 px-4">Device Name</th>
          <th className="py-2.5 px-4">Brightness</th>
          <th className="py-2.5 px-4">RGB Support</th>
          <th className="py-2.5 px-4">Room</th>
        </>
      );
    case 'THERMOSTATS':
      return (
        <>
          <th className="py-2.5 px-4">Device ID</th>
          <th className="py-2.5 px-4">Device Name</th>
          <th className="py-2.5 px-4">Target Temp</th>
          <th className="py-2.5 px-4">Mode</th>
          <th className="py-2.5 px-4">Room</th>
        </>
      );
    case 'SENSOR_READINGS':
      return (
        <>
          <th className="py-2.5 px-4">Reading ID</th>
          <th className="py-2.5 px-4">Device Name</th>
          <th className="py-2.5 px-4">Value</th>
          <th className="py-2.5 px-4">Timestamp</th>
        </>
      );
    case 'ALERTS':
      return (
        <>
          <th className="py-2.5 px-4">Alert ID</th>
          <th className="py-2.5 px-4">Device Name</th>
          <th className="py-2.5 px-4">Message</th>
          <th className="py-2.5 px-4">Status</th>
          <th className="py-2.5 px-4">Time</th>
        </>
      );
    case 'AUTOMATION_RULES':
      return (
        <>
          <th className="py-2.5 px-4">Rule ID</th>
          <th className="py-2.5 px-4">Rule Name</th>
          <th className="py-2.5 px-4">Condition Device</th>
          <th className="py-2.5 px-4">Condition</th>
        </>
      );
    default:
      return null;
  }
}

function renderTableRowCells(entity, row) {
  switch (entity) {
    case 'DEVICES': {
      const isOnline = row.STATUS === 'ONLINE' || row.STATUS === 'ON';
      return (
        <>
          <td className="py-2.5 px-4 font-mono text-[#C88242]">{row.DEVICE_ID}</td>
          <td className="py-2.5 px-4 font-medium">{row.DEVICE_NAME}</td>
          <td className="py-2.5 px-4">
            <span className="text-[10px] font-mono px-2 py-0.5 rounded bg-[#261F1A] border border-[#3E322A] text-[#E6D9CD]">
              {row.DEVICE_SUBTYPE}
            </span>
          </td>
          <td className="py-2.5 px-4">
            <span
              className={`inline-flex items-center gap-1.5 text-[10px] font-mono px-2 py-0.5 rounded-full ${
                isOnline
                  ? 'bg-emerald-950/60 text-emerald-400 border border-emerald-800/60'
                  : 'bg-stone-800/60 text-stone-400 border border-stone-700/60'
              }`}
            >
              <span className={`w-1.5 h-1.5 rounded-full ${isOnline ? 'bg-emerald-400' : 'bg-stone-500'}`}></span>
              {row.STATUS}
            </span>
          </td>
          <td className="py-2.5 px-4 text-[#A8988B]">{row.ROOM_NAME || `Room #${row.ROOM_ID}`}</td>
        </>
      );
    }
    case 'ROOMS':
      return (
        <>
          <td className="py-2.5 px-4 font-mono text-[#C88242]">{row.ROOM_ID}</td>
          <td className="py-2.5 px-4 font-medium">{row.ROOM_NAME}</td>
          <td className="py-2.5 px-4 text-[#A8988B]">Floor {row.FLOOR_NUMBER}</td>
        </>
      );
    case 'SMART_LIGHTS':
      return (
        <>
          <td className="py-2.5 px-4 font-mono text-[#C88242]">{row.DEVICE_ID}</td>
          <td className="py-2.5 px-4 font-medium">{row.DEVICE_NAME}</td>
          <td className="py-2.5 px-4">
            <span className="font-mono text-amber-300 font-semibold">{row.BRIGHTNESS}%</span>
          </td>
          <td className="py-2.5 px-4 text-[#A8988B]">{row.COLOR_SUPPORT || 'NO'}</td>
          <td className="py-2.5 px-4 text-[#A8988B]">{row.ROOM_NAME}</td>
        </>
      );
    case 'THERMOSTATS':
      return (
        <>
          <td className="py-2.5 px-4 font-mono text-[#C88242]">{row.DEVICE_ID}</td>
          <td className="py-2.5 px-4 font-medium">{row.DEVICE_NAME}</td>
          <td className="py-2.5 px-4">
            <span className="font-mono text-cyan-300 font-semibold">{row.TARGET_TEMPERATURE}°C</span>
          </td>
          <td className="py-2.5 px-4">
            <span className="text-[10px] font-mono px-2 py-0.5 rounded bg-[#261F1A] border border-[#3E322A] text-amber-400 font-bold">
              {row.MODE_VAL || row.MODE}
            </span>
          </td>
          <td className="py-2.5 px-4 text-[#A8988B]">{row.ROOM_NAME}</td>
        </>
      );
    case 'SENSOR_READINGS':
      return (
        <>
          <td className="py-2.5 px-4 font-mono text-[#C88242]">{row.READING_ID}</td>
          <td className="py-2.5 px-4 font-medium">{row.DEVICE_NAME}</td>
          <td className="py-2.5 px-4 font-mono text-emerald-400 font-semibold">{row.VALUE}</td>
          <td className="py-2.5 px-4 font-mono text-[11px] text-[#A8988B]">
            {row.READING_TIME ? String(row.READING_TIME).replace('T', ' ') : 'N/A'}
          </td>
        </>
      );
    case 'ALERTS': {
      const isActive = row.STATUS === 'ACTIVE';
      return (
        <>
          <td className="py-2.5 px-4 font-mono text-[#C88242]">{row.ALERT_ID}</td>
          <td className="py-2.5 px-4 font-medium">{row.DEVICE_NAME}</td>
          <td className="py-2.5 px-4 text-[#E6D9CD] max-w-xs truncate">{row.MESSAGE}</td>
          <td className="py-2.5 px-4">
            <span
              className={`text-[10px] font-mono px-2 py-0.5 rounded-full ${
                isActive
                  ? 'bg-red-950/60 text-red-400 border border-red-800/60'
                  : 'bg-emerald-950/60 text-emerald-400 border border-emerald-800/60'
              }`}
            >
              {row.STATUS}
            </span>
          </td>
          <td className="py-2.5 px-4 font-mono text-[10px] text-[#A8988B]">
            {row.ALERT_TIME ? String(row.ALERT_TIME).slice(0, 19).replace('T', ' ') : 'N/A'}
          </td>
        </>
      );
    }
    case 'AUTOMATION_RULES':
      return (
        <>
          <td className="py-2.5 px-4 font-mono text-[#C88242]">{row.RULE_ID}</td>
          <td className="py-2.5 px-4 font-medium">{row.RULE_NAME}</td>
          <td className="py-2.5 px-4 text-[#A8988B]">{row.CONDITION_DEVICE || `Dev #${row.CONDITION_DEVICE_ID}`}</td>
          <td className="py-2.5 px-4 font-mono text-amber-300">
            {row.CONDITION_OPERATOR} {row.CONDITION_VALUE}
          </td>
        </>
      );
    default:
      return null;
  }
}

function renderFormFields({ activeEntity, modalMode, formData, setFormData, roomsList, devicesList }) {
  const updateField = (key, val) => setFormData((prev) => ({ ...prev, [key]: val }));

  switch (activeEntity) {
    case 'DEVICES':
      return (
        <div className="flex flex-col gap-3">
          <div>
            <label className="text-[10px] font-mono uppercase tracking-wider text-[#A8988B] block mb-1">
              Device Name *
            </label>
            <input
              type="text"
              value={formData.device_name ?? formData.DEVICE_NAME ?? ''}
              onChange={(e) => updateField('device_name', e.target.value)}
              placeholder="e.g. Master Bedroom Ceiling Fan"
              className="w-full px-3 py-2 bg-[#120D0A] border border-[#3E322A] rounded-xl text-xs text-[#FBF9F5] focus:outline-none focus:border-[#C88242]"
            />
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="text-[10px] font-mono uppercase tracking-wider text-[#A8988B] block mb-1">
                Subtype
              </label>
              <select
                disabled={modalMode === 'EDIT'}
                value={formData.device_subtype ?? formData.DEVICE_SUBTYPE ?? 'SMART_LIGHT'}
                onChange={(e) => updateField('device_subtype', e.target.value)}
                className="w-full px-3 py-2 bg-[#120D0A] border border-[#3E322A] rounded-xl text-xs text-[#FBF9F5] focus:outline-none focus:border-[#C88242] disabled:opacity-50"
              >
                <option value="SMART_LIGHT">Smart Light</option>
                <option value="THERMOSTAT">Thermostat</option>
                <option value="TEMPERATURE_SENSOR">Temperature Sensor</option>
                <option value="MOTION_SENSOR">Motion Sensor</option>
                <option value="CAMERA">Camera</option>
              </select>
            </div>

            <div>
              <label className="text-[10px] font-mono uppercase tracking-wider text-[#A8988B] block mb-1">
                Status
              </label>
              <select
                value={formData.status ?? formData.STATUS ?? 'ONLINE'}
                onChange={(e) => updateField('status', e.target.value)}
                className="w-full px-3 py-2 bg-[#120D0A] border border-[#3E322A] rounded-xl text-xs text-[#FBF9F5] focus:outline-none focus:border-[#C88242]"
              >
                <option value="ONLINE">ONLINE</option>
                <option value="OFFLINE">OFFLINE</option>
                <option value="STANDBY">STANDBY</option>
              </select>
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="text-[10px] font-mono uppercase tracking-wider text-[#A8988B] block mb-1">
                Room *
              </label>
              <select
                value={formData.room_id ?? formData.ROOM_ID ?? (roomsList[0]?.ROOM_ID || '')}
                onChange={(e) => updateField('room_id', e.target.value)}
                className="w-full px-3 py-2 bg-[#120D0A] border border-[#3E322A] rounded-xl text-xs text-[#FBF9F5] focus:outline-none focus:border-[#C88242]"
              >
                {roomsList.map((r) => (
                  <option key={r.ROOM_ID} value={r.ROOM_ID}>
                    {r.ROOM_NAME} (Fl {r.FLOOR_NUMBER})
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="text-[10px] font-mono uppercase tracking-wider text-[#A8988B] block mb-1">
                Parent Controller (Optional)
              </label>
              <select
                value={formData.parent_device_id ?? formData.PARENT_DEVICE_ID ?? ''}
                onChange={(e) => updateField('parent_device_id', e.target.value)}
                className="w-full px-3 py-2 bg-[#120D0A] border border-[#3E322A] rounded-xl text-xs text-[#FBF9F5] focus:outline-none focus:border-[#C88242]"
              >
                <option value="">None (Independent Device)</option>
                {devicesList
                  .filter((d) => d.DEVICE_ID !== (formData.device_id ?? formData.DEVICE_ID))
                  .map((d) => (
                    <option key={d.DEVICE_ID} value={d.DEVICE_ID}>
                      {d.DEVICE_NAME} (#{d.DEVICE_ID})
                    </option>
                  ))}
              </select>
            </div>
          </div>
        </div>
      );

    case 'ROOMS':
      return (
        <div className="flex flex-col gap-3">
          <div>
            <label className="text-[10px] font-mono uppercase tracking-wider text-[#A8988B] block mb-1">
              Room Name *
            </label>
            <input
              type="text"
              value={formData.room_name ?? formData.ROOM_NAME ?? ''}
              onChange={(e) => updateField('room_name', e.target.value)}
              placeholder="e.g. Media Lounge"
              className="w-full px-3 py-2 bg-[#120D0A] border border-[#3E322A] rounded-xl text-xs text-[#FBF9F5] focus:outline-none focus:border-[#C88242]"
            />
          </div>
          <div>
            <label className="text-[10px] font-mono uppercase tracking-wider text-[#A8988B] block mb-1">
              Floor Number *
            </label>
            <input
              type="number"
              value={formData.floor_number ?? formData.FLOOR_NUMBER ?? 1}
              onChange={(e) => updateField('floor_number', parseInt(e.target.value, 10))}
              className="w-full px-3 py-2 bg-[#120D0A] border border-[#3E322A] rounded-xl text-xs text-[#FBF9F5] focus:outline-none focus:border-[#C88242]"
            />
          </div>
        </div>
      );

    case 'SMART_LIGHTS':
      return (
        <div className="flex flex-col gap-3">
          {modalMode === 'ADD' && (
            <div>
              <label className="text-[10px] font-mono uppercase tracking-wider text-[#A8988B] block mb-1">
                Select Base Device *
              </label>
              <select
                value={formData.device_id ?? (devicesList[0]?.DEVICE_ID || '')}
                onChange={(e) => updateField('device_id', e.target.value)}
                className="w-full px-3 py-2 bg-[#120D0A] border border-[#3E322A] rounded-xl text-xs text-[#FBF9F5] focus:outline-none focus:border-[#C88242]"
              >
                {devicesList
                  .filter((d) => d.DEVICE_SUBTYPE === 'SMART_LIGHT')
                  .map((d) => (
                    <option key={d.DEVICE_ID} value={d.DEVICE_ID}>
                      {d.DEVICE_NAME} (#{d.DEVICE_ID})
                    </option>
                  ))}
              </select>
            </div>
          )}

          <div>
            <div className="flex justify-between items-center mb-1">
              <label className="text-[10px] font-mono uppercase tracking-wider text-[#A8988B]">
                Brightness: {formData.brightness ?? formData.BRIGHTNESS ?? 80}%
              </label>
            </div>
            <input
              type="range"
              min="0"
              max="100"
              value={formData.brightness ?? formData.BRIGHTNESS ?? 80}
              onChange={(e) => updateField('brightness', parseFloat(e.target.value))}
              className="w-full accent-[#C88242]"
            />
          </div>

          <div>
            <label className="text-[10px] font-mono uppercase tracking-wider text-[#A8988B] block mb-1">
              Color (RGB) Support
            </label>
            <select
              value={formData.color_support ?? formData.COLOR_SUPPORT ?? 'YES'}
              onChange={(e) => updateField('color_support', e.target.value)}
              className="w-full px-3 py-2 bg-[#120D0A] border border-[#3E322A] rounded-xl text-xs text-[#FBF9F5] focus:outline-none focus:border-[#C88242]"
            >
              <option value="YES">YES (Full RGB spectrum)</option>
              <option value="NO">NO (Warm / Cool White only)</option>
            </select>
          </div>
        </div>
      );

    case 'THERMOSTATS':
      return (
        <div className="flex flex-col gap-3">
          {modalMode === 'ADD' && (
            <div>
              <label className="text-[10px] font-mono uppercase tracking-wider text-[#A8988B] block mb-1">
                Select Base Device *
              </label>
              <select
                value={formData.device_id ?? (devicesList[0]?.DEVICE_ID || '')}
                onChange={(e) => updateField('device_id', e.target.value)}
                className="w-full px-3 py-2 bg-[#120D0A] border border-[#3E322A] rounded-xl text-xs text-[#FBF9F5] focus:outline-none focus:border-[#C88242]"
              >
                {devicesList
                  .filter((d) => d.DEVICE_SUBTYPE === 'THERMOSTAT')
                  .map((d) => (
                    <option key={d.DEVICE_ID} value={d.DEVICE_ID}>
                      {d.DEVICE_NAME} (#{d.DEVICE_ID})
                    </option>
                  ))}
              </select>
            </div>
          )}

          <div>
            <label className="text-[10px] font-mono uppercase tracking-wider text-[#A8988B] block mb-1">
              Target Temperature (°C) *
            </label>
            <input
              type="number"
              step="0.5"
              value={formData.target_temperature ?? formData.TARGET_TEMPERATURE ?? 22.0}
              onChange={(e) => updateField('target_temperature', parseFloat(e.target.value))}
              className="w-full px-3 py-2 bg-[#120D0A] border border-[#3E322A] rounded-xl text-xs text-[#FBF9F5] focus:outline-none focus:border-[#C88242]"
            />
          </div>

          <div>
            <label className="text-[10px] font-mono uppercase tracking-wider text-[#A8988B] block mb-1">
              HVAC Mode
            </label>
            <select
              value={formData.mode_val ?? formData.MODE_VAL ?? formData.MODE ?? 'HEAT'}
              onChange={(e) => updateField('mode_val', e.target.value)}
              className="w-full px-3 py-2 bg-[#120D0A] border border-[#3E322A] rounded-xl text-xs text-[#FBF9F5] focus:outline-none focus:border-[#C88242]"
            >
              <option value="HEAT">HEAT</option>
              <option value="COOL">COOL</option>
              <option value="AUTO">AUTO</option>
              <option value="ECO">ECO</option>
              <option value="OFF">OFF</option>
            </select>
          </div>
        </div>
      );

    case 'SENSOR_READINGS':
      return (
        <div className="flex flex-col gap-3">
          <div>
            <label className="text-[10px] font-mono uppercase tracking-wider text-[#A8988B] block mb-1">
              Target Sensor *
            </label>
            <select
              value={formData.device_id ?? (devicesList[0]?.DEVICE_ID || '')}
              onChange={(e) => updateField('device_id', e.target.value)}
              className="w-full px-3 py-2 bg-[#120D0A] border border-[#3E322A] rounded-xl text-xs text-[#FBF9F5] focus:outline-none focus:border-[#C88242]"
            >
              {devicesList.map((d) => (
                <option key={d.DEVICE_ID} value={d.DEVICE_ID}>
                  {d.DEVICE_NAME} ({d.DEVICE_SUBTYPE})
                </option>
              ))}
            </select>
          </div>
          <div>
            <label className="text-[10px] font-mono uppercase tracking-wider text-[#A8988B] block mb-1">
              Metric Value *
            </label>
            <input
              type="number"
              step="0.1"
              value={formData.value ?? 24.5}
              onChange={(e) => updateField('value', parseFloat(e.target.value))}
              placeholder="e.g. 24.5"
              className="w-full px-3 py-2 bg-[#120D0A] border border-[#3E322A] rounded-xl text-xs text-[#FBF9F5] focus:outline-none focus:border-[#C88242]"
            />
          </div>
        </div>
      );

    case 'ALERTS':
      return (
        <div className="flex flex-col gap-3">
          {modalMode === 'ADD' && (
            <div>
              <label className="text-[10px] font-mono uppercase tracking-wider text-[#A8988B] block mb-1">
                Target Device *
              </label>
              <select
                value={formData.device_id ?? (devicesList[0]?.DEVICE_ID || '')}
                onChange={(e) => updateField('device_id', e.target.value)}
                className="w-full px-3 py-2 bg-[#120D0A] border border-[#3E322A] rounded-xl text-xs text-[#FBF9F5] focus:outline-none focus:border-[#C88242]"
              >
                {devicesList.map((d) => (
                  <option key={d.DEVICE_ID} value={d.DEVICE_ID}>
                    {d.DEVICE_NAME} (#{d.DEVICE_ID})
                  </option>
                ))}
              </select>
            </div>
          )}

          <div>
            <label className="text-[10px] font-mono uppercase tracking-wider text-[#A8988B] block mb-1">
              Alert Message *
            </label>
            <textarea
              rows={2}
              value={formData.message ?? formData.MESSAGE ?? ''}
              onChange={(e) => updateField('message', e.target.value)}
              placeholder="e.g. Temperature threshold exceeded (32.4°C)"
              className="w-full px-3 py-2 bg-[#120D0A] border border-[#3E322A] rounded-xl text-xs text-[#FBF9F5] focus:outline-none focus:border-[#C88242] resize-none"
            />
          </div>

          <div>
            <label className="text-[10px] font-mono uppercase tracking-wider text-[#A8988B] block mb-1">
              Alert Status
            </label>
            <select
              value={formData.status ?? formData.STATUS ?? 'ACTIVE'}
              onChange={(e) => updateField('status', e.target.value)}
              className="w-full px-3 py-2 bg-[#120D0A] border border-[#3E322A] rounded-xl text-xs text-[#FBF9F5] focus:outline-none focus:border-[#C88242]"
            >
              <option value="ACTIVE">ACTIVE</option>
              <option value="ACKNOWLEDGED">ACKNOWLEDGED</option>
              <option value="RESOLVED">RESOLVED</option>
            </select>
          </div>
        </div>
      );

    case 'AUTOMATION_RULES':
      return (
        <div className="flex flex-col gap-3">
          <div>
            <label className="text-[10px] font-mono uppercase tracking-wider text-[#A8988B] block mb-1">
              Rule Name *
            </label>
            <input
              type="text"
              value={formData.rule_name ?? formData.RULE_NAME ?? ''}
              onChange={(e) => updateField('rule_name', e.target.value)}
              placeholder="e.g. Overheat Auto-Cooling"
              className="w-full px-3 py-2 bg-[#120D0A] border border-[#3E322A] rounded-xl text-xs text-[#FBF9F5] focus:outline-none focus:border-[#C88242]"
            />
          </div>

          {modalMode === 'ADD' && (
            <div>
              <label className="text-[10px] font-mono uppercase tracking-wider text-[#A8988B] block mb-1">
                Condition Sensor Device *
              </label>
              <select
                value={formData.condition_device_id ?? (devicesList[0]?.DEVICE_ID || '')}
                onChange={(e) => updateField('condition_device_id', e.target.value)}
                className="w-full px-3 py-2 bg-[#120D0A] border border-[#3E322A] rounded-xl text-xs text-[#FBF9F5] focus:outline-none focus:border-[#C88242]"
              >
                {devicesList.map((d) => (
                  <option key={d.DEVICE_ID} value={d.DEVICE_ID}>
                    {d.DEVICE_NAME} ({d.DEVICE_SUBTYPE})
                  </option>
                ))}
              </select>
            </div>
          )}

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="text-[10px] font-mono uppercase tracking-wider text-[#A8988B] block mb-1">
                Operator
              </label>
              <select
                value={formData.condition_operator ?? formData.CONDITION_OPERATOR ?? '>'}
                onChange={(e) => updateField('condition_operator', e.target.value)}
                className="w-full px-3 py-2 bg-[#120D0A] border border-[#3E322A] rounded-xl text-xs text-[#FBF9F5] focus:outline-none focus:border-[#C88242]"
              >
                <option value=">">&gt; (Greater than)</option>
                <option value="<">&lt; (Less than)</option>
                <option value="=">= (Equal to)</option>
                <option value=">=">&gt;= (Greater or equal)</option>
                <option value="<=">&lt;= (Less or equal)</option>
              </select>
            </div>

            <div>
              <label className="text-[10px] font-mono uppercase tracking-wider text-[#A8988B] block mb-1">
                Threshold Value *
              </label>
              <input
                type="text"
                value={formData.condition_value ?? formData.CONDITION_VALUE ?? '25.0'}
                onChange={(e) => updateField('condition_value', e.target.value)}
                placeholder="e.g. 25.0"
                className="w-full px-3 py-2 bg-[#120D0A] border border-[#3E322A] rounded-xl text-xs text-[#FBF9F5] focus:outline-none focus:border-[#C88242]"
              />
            </div>
          </div>
        </div>
      );

    default:
      return null;
  }
}
