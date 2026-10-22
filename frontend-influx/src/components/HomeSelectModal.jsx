import React from 'react';

export default function HomeSelectModal({
  isOpen,
  onClose,
  homes = [],
  onSelectHome,
  onCreateHome,
  loading = false,
}) {
  if (!isOpen) return null;

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-md animate-fadeIn"
      onClick={onClose}
    >
      <div
        className="relative w-full max-w-md bg-[#1B1613] border border-[#3E322A] rounded-2xl shadow-2xl p-8 text-[#FBF9F5]"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Close Button */}
        <button
          onClick={onClose}
          className="absolute top-5 right-5 text-[#A8988B] hover:text-[#FBF9F5] transition-colors text-xl font-light focus:outline-none"
          aria-label="Close modal"
        >
          ✕
        </button>

        {/* Modal Header */}
        <div className="text-center mb-8">
          <p className="text-xs uppercase tracking-[0.25em] text-[#C88242] font-semibold mb-2">
            {homes.length === 0 && !loading ? 'MY HOMES' : 'HOUSE SELECTION'}
          </p>
          <h2 className="text-2xl font-light tracking-wide text-[#FBF9F5]">
            {homes.length === 0 && !loading ? 'No homes yet' : 'SELECT A HOME'}
          </h2>
        </div>

        {/* Content */}
        {loading ? (
          <div className="flex flex-col items-center justify-center py-10 gap-3">
            <div className="w-8 h-8 border-2 border-[#C88242] border-t-transparent rounded-full animate-spin"></div>
            <p className="text-xs tracking-widest text-[#A8988B] uppercase">Loading accessible homes...</p>
          </div>
        ) : homes.length === 0 ? (
          <div className="text-center py-6">
            <p className="text-sm text-[#A8988B] mb-6">
              You do not have access to any homes yet. Create your first smart home to begin.
            </p>
            <button
              onClick={() => {
                onClose();
                if (onCreateHome) onCreateHome();
              }}
              className="w-full py-3.5 px-6 rounded-xl bg-gradient-to-r from-[#C88242] to-[#A3642E] text-white font-medium text-sm tracking-wider uppercase transition-all shadow-lg hover:brightness-110 active:scale-[0.99]"
            >
              + CREATE HOME
            </button>
          </div>
        ) : (
          <div className="flex flex-col gap-2.5 max-h-[60vh] overflow-y-auto pr-1 custom-scrollbar">
            {homes.map((home) => (
              <button
                key={home.homeId}
                onClick={() => {
                  onSelectHome(home);
                  onClose();
                }}
                className="w-full py-4 px-6 text-left rounded-xl bg-[#261F1A]/80 border border-[#3E322A]/60 hover:border-[#C88242]/80 hover:bg-[#322822] transition-all duration-200 group flex items-center justify-between"
              >
                <span className="text-base font-normal text-[#FBF9F5] group-hover:text-[#F3BA82] transition-colors">
                  {home.homeName}
                </span>
                <span className="text-xs text-[#A8988B] group-hover:text-[#C88242] transition-all transform group-hover:translate-x-1">
                  →
                </span>
              </button>
            ))}

            {/* Optional Create Home Action for users with homes */}
            {onCreateHome && (
              <button
                onClick={() => {
                  onClose();
                  onCreateHome();
                }}
                className="mt-3 w-full py-2.5 px-4 text-center rounded-lg border border-dashed border-[#3E322A] text-xs text-[#C88242] hover:border-[#C88242] hover:bg-[#261F1A] transition-all uppercase tracking-wider"
              >
                + Register Another Home
              </button>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
