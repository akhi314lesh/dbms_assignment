import { useAuth } from '../context/AuthContext';

export default function Authentication() {
  const { oracleUser } = useAuth();

  return (
    <div className="min-h-screen flex items-center justify-center bg-[#140F0C] text-[#FBF9F5] p-6">
      <div className="max-w-md w-full p-8 rounded-2xl bg-[#1B1613] border border-[#2E2520] text-center space-y-4">
        <div className="w-12 h-12 mx-auto rounded-xl bg-cyan-500/10 border border-cyan-500/30 flex items-center justify-center text-cyan-400 text-2xl font-bold">
          ✓
        </div>
        <h2 className="text-xl font-light tracking-wide text-[#FBF9F5]">DA3 Team Development Mode</h2>
        <p className="text-xs text-[#A8988B] leading-relaxed">
          Firebase authentication has been decoupled for local team sharing.
          You are automatically authenticated as <strong>{oracleUser?.name || 'DA3 Teammate'}</strong> ({oracleUser?.role || 'ADMIN'}).
        </p>
        <div className="p-3 rounded-xl bg-[#0D0A08] border border-[#2E2520] text-[11px] font-mono text-emerald-400">
          ✓ Dev Authorization Active · Full System Access Granted
        </div>
        <button
          onClick={() => window.location.reload()}
          className="w-full py-2.5 rounded-xl bg-gradient-to-r from-[#C88242] to-[#A3642E] text-white text-xs font-semibold uppercase tracking-wider hover:brightness-110 transition-all"
        >
          Proceed to Dashboard
        </button>
      </div>
    </div>
  );
}