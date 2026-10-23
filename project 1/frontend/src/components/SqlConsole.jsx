import React, { useState } from 'react';
import api from '../services/api';
import { useAuth } from '../context/AuthContext';

export default function SqlConsole({ homeId }) {
  const { oracleUser } = useAuth();
  const isAdmin = oracleUser?.role === 'ADMIN';
  const currentUserId = oracleUser?.userId || 1;
  const currentHomeId = homeId || 1;

  const [activeCategory, setActiveCategory] = useState('SELECT');
  const [sql, setSql] = useState('SELECT device_id, device_name, device_subtype, status, room_id FROM devices');
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState(null);
  const [error, setError] = useState(null);
  const [confirmPending, setConfirmPending] = useState(null);

  // PL/SQL Specific State
  const [selectedPlSqlRoutine, setSelectedPlSqlRoutine] = useState('FN_CALCULATE_DEVICE_UPTIME');
  const [plSqlParams, setPlSqlParams] = useState({
    deviceId: '1',
    alertId: '1',
    homeId: String(currentHomeId),
    value: '24.5',
  });

  const categories = ['SELECT', 'INSERT', 'UPDATE', 'DELETE', 'PL/SQL'];

  const templates = {
    SELECT: [
      {
        label: 'Devices in Home',
        query: 'SELECT device_id, device_name, device_subtype, status, room_id FROM devices',
      },
      {
        label: 'Device Status & Uptime',
        query: 'SELECT device_name, device_subtype, status, last_restart_time FROM devices',
      },
      {
        label: 'Rooms & Layout',
        query: 'SELECT room_id, room_name, floor_number, home_id FROM rooms ORDER BY floor_number, room_name',
      },
      {
        label: 'Active Alerts',
        query: "SELECT alert_id, device_id, message, alert_time, status FROM alerts WHERE status = 'ACTIVE' ORDER BY alert_time DESC",
      },
      {
        label: 'Recent Sensor Readings',
        query: 'SELECT device_id, reading_id, value, reading_time FROM sensor_readings ORDER BY reading_time DESC FETCH FIRST 20 ROWS ONLY',
      },
      {
        label: 'Thermostat Modes',
        query: 'SELECT d.device_id, d.device_name, t.target_temperature, t."MODE" FROM devices d JOIN thermostats t ON d.device_id = t.device_id',
      },
      {
        label: 'Smart Light Brightness',
        query: 'SELECT d.device_id, d.device_name, l.brightness, l.color_support FROM devices d JOIN smart_lights l ON d.device_id = l.device_id',
      },
      {
        label: 'Parent/Child Hierarchy',
        query: 'SELECT child.device_id, child.device_name, child.device_subtype, parent.device_name AS controller FROM devices child LEFT JOIN devices parent ON child.parent_device_id = parent.device_id',
      },
      {
        label: 'Automation Rules',
        query: 'SELECT r.rule_id, r.rule_name, r.condition_operator, r.condition_value, a.action_type, a.action_value FROM automation_rules r LEFT JOIN rule_actions a ON r.rule_id = a.rule_id',
      },
      {
        label: 'Accessible Homes',
        query: 'SELECT home_id, home_name, street, city, pincode FROM homes',
      },
    ],
    INSERT: [
      {
        label: 'Add Room',
        query: `INSERT INTO rooms (room_id, room_name, floor_number, home_id) VALUES (seq_room_id.NEXTVAL, 'Guest Suite', 1, ${currentHomeId})`,
      },
      {
        label: 'Add Device (Base)',
        query: `INSERT INTO devices (device_id, device_name, device_subtype, status, install_date, last_restart_time, room_id, parent_device_id) VALUES (seq_device_id.NEXTVAL, 'Balcony Light', 'SMART_LIGHT', 'ONLINE', SYSDATE, SYSTIMESTAMP, 1, NULL)`,
      },
      {
        label: 'Add Smart Light Subtype',
        query: `INSERT INTO smart_lights (device_id, brightness, color_support) VALUES (1, 80.00, 'YES')`,
      },
      {
        label: 'Add Thermostat Subtype',
        query: `INSERT INTO thermostats (device_id, target_temperature, "MODE") VALUES (1, 22.50, 'HEAT')`,
      },
      {
        label: 'Record Sensor Reading',
        query: `INSERT INTO sensor_readings (device_id, reading_id, value, reading_time) VALUES (1, seq_reading_id.NEXTVAL, 23.40, SYSTIMESTAMP)`,
      },
      {
        label: 'Create Automation Rule',
        query: `INSERT INTO automation_rules (rule_id, rule_name, condition_operator, condition_value, condition_device_id, created_by_user_id, created_date) VALUES (seq_rule_id.NEXTVAL, 'Night Temp Regulation', '>', '26.0', 1, ${currentUserId}, SYSDATE)`,
      },
      {
        label: 'Create Rule Action',
        query: `INSERT INTO rule_actions (action_id, rule_id, target_device_id, action_type, action_value) VALUES (seq_action_id.NEXTVAL, 1, 1, 'SET_TEMP', '21.0')`,
      },
      {
        label: 'Notification Preference',
        query: `INSERT INTO notification_preferences (user_id, device_id, category_id, channel, enabled) VALUES (${currentUserId}, 1, 1, 'EMAIL', 1)`,
      },
    ],
    UPDATE: [
      {
        label: 'Set Device Offline',
        query: `UPDATE devices SET status = 'OFFLINE' WHERE device_id = 1`,
      },
      {
        label: 'Rename Device',
        query: `UPDATE devices SET device_name = 'Master Bedroom Pendant' WHERE device_id = 1`,
      },
      {
        label: 'Update Light Brightness',
        query: `UPDATE smart_lights SET brightness = 75.00 WHERE device_id = 1`,
      },
      {
        label: 'Update Thermostat Target',
        query: `UPDATE thermostats SET target_temperature = 21.00, "MODE" = 'COOL' WHERE device_id = 1`,
      },
      {
        label: 'Rename Room',
        query: `UPDATE rooms SET room_name = 'Executive Study' WHERE room_id = 1`,
      },
      {
        label: 'Acknowledge Alert',
        query: `UPDATE alerts SET status = 'ACKNOWLEDGED', acknowledged_by = ${currentUserId}, acknowledged_timestamp = SYSTIMESTAMP WHERE alert_id = 1`,
      },
      {
        label: 'Disable Notification Pref',
        query: `UPDATE notification_preferences SET enabled = 0 WHERE user_id = ${currentUserId} AND device_id = 1 AND category_id = 1`,
      },
    ],
    DELETE: [
      {
        label: 'Delete Alert',
        query: `DELETE FROM alerts WHERE alert_id = 999`,
      },
      {
        label: 'Delete Rule Action',
        query: `DELETE FROM rule_actions WHERE action_id = 999`,
      },
      {
        label: 'Delete Notification Pref',
        query: `DELETE FROM notification_preferences WHERE user_id = ${currentUserId} AND device_id = 1 AND category_id = 1`,
      },
      {
        label: 'Delete Sensor Reading',
        query: `DELETE FROM sensor_readings WHERE device_id = 1 AND reading_id = 999`,
      },
      {
        label: 'Delete Light Subtype',
        query: `DELETE FROM smart_lights WHERE device_id = 999`,
      },
      {
        label: 'Delete Device',
        query: `DELETE FROM devices WHERE device_id = 999`,
      },
      {
        label: 'Delete Room',
        query: `DELETE FROM rooms WHERE room_id = 999`,
      },
    ],
  };

  const plSqlRoutines = [
    {
      type: 'FUNCTION',
      name: 'FN_CALCULATE_DEVICE_UPTIME',
      title: 'Calculate Device Uptime',
      desc: 'Executes Oracle function to compute derived device uptime based on last restart/install timestamp.',
      fields: [{ key: 'deviceId', label: 'Device ID', type: 'number', placeholder: 'e.g. 1' }],
    },
    {
      type: 'FUNCTION',
      name: 'FN_CHECK_DEVICE_ACCESS',
      title: 'Check Device Access',
      desc: 'Executes Oracle function to verify M:N HOME_ACCESS authorization for current authenticated user.',
      fields: [{ key: 'deviceId', label: 'Device ID', type: 'number', placeholder: 'e.g. 1' }],
    },
    {
      type: 'PROCEDURE',
      name: 'SP_ACKNOWLEDGE_ALERT',
      title: 'Acknowledge Alert',
      desc: 'Executes stored procedure to transition alert status to ACKNOWLEDGED with audit timestamp.',
      fields: [{ key: 'alertId', label: 'Alert ID', type: 'number', placeholder: 'e.g. 1' }],
    },
    {
      type: 'PROCEDURE',
      name: 'SP_HOME_DEVICE_HEALTH_REPORT',
      title: 'Home Device Health Report',
      desc: 'Executes explicit cursor stored procedure to generate formatted DBMS_OUTPUT diagnostic report.',
      fields: [{ key: 'homeId', label: 'Home ID', type: 'number', placeholder: `e.g. ${currentHomeId}` }],
    },
    {
      type: 'PROCEDURE',
      name: 'SP_RECORD_SENSOR_READING',
      title: 'Record Sensor Reading',
      desc: 'Executes stored procedure to insert weak entity reading using SEQ_READING_ID sequence.',
      fields: [
        { key: 'deviceId', label: 'Device ID', type: 'number', placeholder: 'e.g. 1' },
        { key: 'value', label: 'Metric Value', type: 'number', placeholder: 'e.g. 24.5' },
      ],
    },
    {
      type: 'TRIGGER',
      name: 'TEST_TRIGGER_CYCLE',
      title: 'Test TRG_PREVENT_DEVICE_CYCLE Trigger',
      desc: 'Performs controlled circular parent assignment test. Verifies Oracle trigger rejects cycle with ORA-20030.',
      fields: [{ key: 'deviceId', label: 'Target Device ID', type: 'number', placeholder: 'e.g. 1' }],
    },
  ];

  const handleSelectTemplate = (t) => {
    setSql(t.query);
    setError(null);
    setResult(null);
  };

  const handleRunSql = async () => {
    if (!sql.trim()) return;

    const trimmed = sql.trim().toUpperCase();
    const isMutation = trimmed.startsWith('UPDATE') || trimmed.startsWith('DELETE');
    if (isMutation && !confirmPending) {
      setConfirmPending({ type: 'SQL', sql: sql.trim() });
      return;
    }

    setConfirmPending(null);
    setLoading(true);
    setError(null);
    setResult(null);

    try {
      const response = await api.post('/api/sql-console/execute', {
        sql: sql.trim(),
        homeId: currentHomeId,
      });

      if (response.data.success) {
        setResult(response.data);
      } else {
        setError(response.data.message || 'Execution error');
      }
    } catch (err) {
      console.error('SQL execution failed', err);
      setError(err.response?.data?.message || 'Server error while executing query.');
    } finally {
      setLoading(false);
    }
  };

  const handleRunPlSql = async () => {
    const routine = plSqlRoutines.find((r) => r.name === selectedPlSqlRoutine);
    if (!routine) return;

    const isMutation = routine.name === 'SP_ACKNOWLEDGE_ALERT' || routine.name === 'SP_RECORD_SENSOR_READING';
    if (isMutation && !confirmPending) {
      setConfirmPending({ type: 'PLSQL', routine: routine.name });
      return;
    }

    setConfirmPending(null);
    setLoading(true);
    setError(null);
    setResult(null);

    try {
      const response = await api.post('/api/sql-console/execute-plsql', {
        routineName: routine.name,
        params: plSqlParams,
      });

      if (response.data.success) {
        setResult(response.data);
      } else {
        setError(response.data.message || 'PL/SQL execution error');
      }
    } catch (err) {
      console.error('PL/SQL execution failed', err);
      setError(err.response?.data?.message || 'Server error executing stored routine.');
    } finally {
      setLoading(false);
    }
  };

  const currentRoutine = plSqlRoutines.find((r) => r.name === selectedPlSqlRoutine);

  return (
    <div className="w-full rounded-2xl bg-[#140F0C] border border-[#2E2520] p-6 shadow-2xl">
      {/* Header */}
      <div className="flex flex-wrap items-center justify-between gap-3 mb-4 pb-3 border-b border-[#2E2520]">
        <div className="flex items-center gap-3">
          <div className="w-3 h-3 rounded-full bg-[#C88242] shadow-[0_0_8px_rgba(200,130,66,0.6)]"></div>
          <h3 className="text-sm font-semibold tracking-wider uppercase text-[#FBF9F5]">
            SQL Console
          </h3>
          <span className="text-[10px] uppercase font-mono px-2 py-0.5 rounded bg-[#261F1A] border border-[#3E322A] text-[#A8988B]">
            {isAdmin ? 'ADMIN · FULL SQL DML + PL/SQL' : 'USER · SQL DML & PL/SQL SCOPED TO MY HOMES'}
          </span>
        </div>

        {/* Category Tabs */}
        <div className="flex items-center gap-1 bg-[#1B1613] p-1 rounded-xl border border-[#3E322A]">
          {categories.map((cat) => (
            <button
              key={cat}
              onClick={() => {
                setActiveCategory(cat);
                setError(null);
                setResult(null);
                if (cat !== 'PL/SQL' && templates[cat]?.length > 0) {
                  setSql(templates[cat][0].query);
                }
              }}
              className={`text-[11px] font-semibold px-3 py-1 rounded-lg transition-all ${
                activeCategory === cat
                  ? 'bg-[#C88242] text-white shadow-md'
                  : 'text-[#A8988B] hover:text-[#E6D9CD]'
              }`}
            >
              {cat}
            </button>
          ))}
        </div>
      </div>

      {/* Templates Row for SQL Categories */}
      {activeCategory !== 'PL/SQL' && (
        <div className="flex items-center gap-1.5 flex-wrap mb-4">
          <span className="text-[10px] uppercase tracking-wider text-[#A8988B] mr-1">
            {activeCategory} Templates:
          </span>
          {templates[activeCategory]?.map((t) => (
            <button
              key={t.label}
              onClick={() => handleSelectTemplate(t)}
              className="text-[11px] px-2.5 py-1 rounded-md bg-[#1B1613] border border-[#3E322A] text-[#E6D9CD] hover:border-[#C88242] hover:text-[#C88242] transition-colors"
            >
              {t.label}
            </button>
          ))}
        </div>
      )}

      {/* PL/SQL Specific Dashboard */}
      {activeCategory === 'PL/SQL' ? (
        <div className="flex flex-col gap-4">
          {/* Routine Selector Tabs */}
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-2">
            <div>
              <span className="text-[9px] font-mono uppercase tracking-widest text-[#C88242] block mb-1">
                FUNCTIONS
              </span>
              <div className="flex flex-col gap-1">
                {plSqlRoutines
                  .filter((r) => r.type === 'FUNCTION')
                  .map((r) => (
                    <button
                      key={r.name}
                      onClick={() => {
                        setSelectedPlSqlRoutine(r.name);
                        setError(null);
                        setResult(null);
                      }}
                      className={`text-left text-xs px-3 py-2 rounded-lg border transition-all ${
                        selectedPlSqlRoutine === r.name
                          ? 'bg-[#261F1A] border-[#C88242] text-[#FBF9F5]'
                          : 'bg-[#1A1410] border-[#3E322A] text-[#A8988B] hover:text-[#E6D9CD]'
                      }`}
                    >
                      <div className="font-semibold">{r.title}</div>
                      <div className="text-[9px] font-mono text-stone-500 truncate">{r.name}</div>
                    </button>
                  ))}
              </div>
            </div>

            <div>
              <span className="text-[9px] font-mono uppercase tracking-widest text-[#C88242] block mb-1">
                PROCEDURES
              </span>
              <div className="flex flex-col gap-1">
                {plSqlRoutines
                  .filter((r) => r.type === 'PROCEDURE')
                  .map((r) => (
                    <button
                      key={r.name}
                      onClick={() => {
                        setSelectedPlSqlRoutine(r.name);
                        setError(null);
                        setResult(null);
                      }}
                      className={`text-left text-xs px-3 py-2 rounded-lg border transition-all ${
                        selectedPlSqlRoutine === r.name
                          ? 'bg-[#261F1A] border-[#C88242] text-[#FBF9F5]'
                          : 'bg-[#1A1410] border-[#3E322A] text-[#A8988B] hover:text-[#E6D9CD]'
                      }`}
                    >
                      <div className="font-semibold">{r.title}</div>
                      <div className="text-[9px] font-mono text-stone-500 truncate">{r.name}</div>
                    </button>
                  ))}
              </div>
            </div>

            <div>
              <span className="text-[9px] font-mono uppercase tracking-widest text-[#C88242] block mb-1">
                TRIGGERS
              </span>
              <div className="flex flex-col gap-1">
                {plSqlRoutines
                  .filter((r) => r.type === 'TRIGGER')
                  .map((r) => (
                    <button
                      key={r.name}
                      onClick={() => {
                        setSelectedPlSqlRoutine(r.name);
                        setError(null);
                        setResult(null);
                      }}
                      className={`text-left text-xs px-3 py-2 rounded-lg border transition-all ${
                        selectedPlSqlRoutine === r.name
                          ? 'bg-[#261F1A] border-[#C88242] text-[#FBF9F5]'
                          : 'bg-[#1A1410] border-[#3E322A] text-[#A8988B] hover:text-[#E6D9CD]'
                      }`}
                    >
                      <div className="font-semibold">{r.title}</div>
                      <div className="text-[9px] font-mono text-stone-500 truncate">{r.name}</div>
                    </button>
                  ))}
              </div>
            </div>
          </div>

          {/* Routine Form Panel */}
          {currentRoutine && (
            <div className="p-4 rounded-xl bg-[#1A1410] border border-[#3E322A] flex flex-col gap-3">
              <div>
                <div className="flex items-center gap-2">
                  <span className="text-xs font-semibold text-[#FBF9F5]">{currentRoutine.title}</span>
                  <span className="text-[10px] font-mono text-[#C88242] bg-[#261F1A] px-2 py-0.5 rounded border border-[#3E322A]">
                    {currentRoutine.name}
                  </span>
                </div>
                <p className="text-[11px] text-[#A8988B] mt-1">{currentRoutine.desc}</p>
              </div>

              {/* Parameter Inputs */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 pt-2 border-t border-[#2E2520]">
                {currentRoutine.fields.map((f) => (
                  <div key={f.key} className="flex flex-col gap-1">
                    <label className="text-[10px] font-mono uppercase tracking-wider text-[#A8988B]">
                      {f.label}
                    </label>
                    <input
                      type={f.type}
                      value={plSqlParams[f.key] || ''}
                      onChange={(e) => setPlSqlParams({ ...plSqlParams, [f.key]: e.target.value })}
                      placeholder={f.placeholder}
                      className="px-3 py-2 rounded-lg bg-[#140F0C] border border-[#3E322A] text-[#FBF9F5] font-mono text-xs focus:outline-none focus:border-[#C88242]"
                    />
                  </div>
                ))}
              </div>

              {/* Execute Button */}
              <div className="flex items-center justify-between pt-2">
                <p className="text-[11px] text-[#A8988B]">
                  {isAdmin
                    ? 'Routine execution runs via authenticated Oracle stored procedure calls.'
                    : 'SQL operations and routines are restricted to homes you can access.'}
                </p>

                <button
                  onClick={handleRunPlSql}
                  disabled={loading}
                  className="px-6 py-2 rounded-xl bg-gradient-to-r from-[#C88242] to-[#A3642E] text-white text-xs font-semibold uppercase tracking-wider shadow-lg hover:brightness-110 active:scale-[0.98] disabled:opacity-50 transition-all flex items-center gap-2"
                >
                  {loading ? (
                    <>
                      <div className="w-3 h-3 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                      <span>Executing...</span>
                    </>
                  ) : (
                    <span>Execute {currentRoutine.type === 'FUNCTION' ? 'Function' : currentRoutine.type === 'TRIGGER' ? 'Test' : 'Procedure'}</span>
                  )}
                </button>
              </div>
            </div>
          )}
        </div>
      ) : (
        /* SQL Query Editor & Controls */
        <div className="flex flex-col gap-3">
          <div className="relative">
            <textarea
              value={sql}
              onChange={(e) => setSql(e.target.value)}
              rows={4}
              spellCheck={false}
              className="w-full px-4 py-3 rounded-xl bg-[#1A1410] border border-[#3E322A] text-[#FBF9F5] font-mono text-xs focus:outline-none focus:border-[#C88242] resize-y custom-scrollbar"
              placeholder="Enter SQL statement (e.g. SELECT device_name, status FROM devices)..."
            />
          </div>

          <div className="flex items-center justify-between">
            <p className="text-[11px] text-[#A8988B]">
              {isAdmin
                ? 'Permitted: SELECT, INSERT, UPDATE, DELETE. DDL and schema destruction prohibited.'
                : 'SQL operations are restricted to homes you can access. Identity tables are protected.'}
            </p>

            <button
              onClick={handleRunSql}
              disabled={loading || !sql.trim()}
              className="px-6 py-2.5 rounded-xl bg-gradient-to-r from-[#C88242] to-[#A3642E] text-white text-xs font-semibold uppercase tracking-wider shadow-lg hover:brightness-110 active:scale-[0.98] disabled:opacity-50 transition-all flex items-center gap-2"
            >
              {loading ? (
                <>
                  <div className="w-3 h-3 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                  <span>Executing...</span>
                </>
              ) : (
                <span>Run SQL</span>
              )}
            </button>
          </div>
        </div>
      )}

      {/* Confirmation Modal */}
      {confirmPending && (
        <div className="mt-4 p-4 rounded-xl bg-amber-950/40 border border-amber-800/60 flex flex-col gap-2">
          <div className="flex items-center gap-2 text-amber-400 font-semibold text-xs">
            <span>⚠</span>
            <span>Confirm Database Mutation</span>
          </div>
          <p className="text-xs text-amber-200/90 font-mono">
            {confirmPending.type === 'SQL'
              ? `You are about to execute a database modification:\n${confirmPending.sql}`
              : `You are about to execute stored routine ${confirmPending.routine}.`}
          </p>
          <div className="flex items-center gap-2 mt-2">
            <button
              onClick={() => {
                if (confirmPending.type === 'SQL') handleRunSql();
                else handleRunPlSql();
              }}
              className="px-4 py-1.5 rounded-lg bg-amber-600 hover:bg-amber-500 text-black font-semibold text-xs transition-colors"
            >
              Confirm & Execute
            </button>
            <button
              onClick={() => setConfirmPending(null)}
              className="px-4 py-1.5 rounded-lg bg-[#261F1A] hover:bg-[#3E322A] text-[#A8988B] text-xs transition-colors"
            >
              Cancel
            </button>
          </div>
        </div>
      )}

      {/* Error Output */}
      {error && (
        <div className="mt-4 p-3.5 rounded-xl bg-red-950/40 border border-red-800/60 text-red-200 text-xs font-mono">
          <div className="flex items-center gap-2 font-sans font-semibold text-red-400 mb-1">
            <span>⚠</span>
            <span>Execution Rejected</span>
          </div>
          <p className="break-all">{error}</p>
        </div>
      )}

      {/* Success Result Display */}
      {result && (
        <div className="mt-4 p-4 rounded-xl bg-[#1A1410] border border-[#3E322A]">
          {/* Result Meta */}
          <div className="flex items-center justify-between text-[11px] text-[#A8988B] pb-3 mb-3 border-b border-[#2E2520]">
            <span className="text-emerald-400 font-medium">✓ {result.message}</span>
            <div className="flex items-center gap-3 font-mono">
              {result.rowCount > 0 && <span>{result.rowCount} row(s)</span>}
              {result.affectedRows > 0 && <span>{result.affectedRows} affected</span>}
              <span>{result.executionTimeMs} ms</span>
            </div>
          </div>

          {/* Result Table for SELECT / PLSQL Query Output */}
          {result.columns && result.columns.length > 0 && result.rows && (
            <div className="overflow-x-auto max-h-64 overflow-y-auto custom-scrollbar">
              <table className="w-full text-left text-xs font-mono border-collapse">
                <thead>
                  <tr className="border-b border-[#3E322A] bg-[#261F1A]/80 text-[#C88242]">
                    {result.columns.map((col) => (
                      <th key={col} className="py-2 px-3 font-semibold tracking-wider">
                        {col}
                      </th>
                    ))}
                  </tr>
                </thead>
                <tbody className="divide-y divide-[#2E2520]">
                  {result.rows.length === 0 ? (
                    <tr>
                      <td colSpan={result.columns.length} className="py-4 text-center text-[#A8988B]">
                        Zero rows returned.
                      </td>
                    </tr>
                  ) : (
                    result.rows.map((row, idx) => (
                      <tr key={idx} className="hover:bg-[#261F1A]/40 transition-colors">
                        {result.columns.map((col) => (
                          <td key={col} className="py-2 px-3 text-[#FBF9F5] whitespace-pre-wrap">
                            {row[col] != null ? String(row[col]) : <span className="text-stone-500 italic">null</span>}
                          </td>
                        ))}
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
