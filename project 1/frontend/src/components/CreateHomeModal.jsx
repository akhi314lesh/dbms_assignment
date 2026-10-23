import React, { useState } from 'react';
import api from '../services/api';

export default function CreateHomeModal({ isOpen, onClose, onHomeCreated }) {
  const [homeName, setHomeName] = useState('');
  const [street, setStreet] = useState('');
  const [city, setCity] = useState('');
  const [pincode, setPincode] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);

  if (!isOpen) return null;

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!homeName.trim()) {
      setError('Home name is required.');
      return;
    }

    setSubmitting(true);
    setError(null);

    try {
      const response = await api.post('/api/homes', {
        homeName: homeName.trim(),
        street: street.trim(),
        city: city.trim(),
        pincode: pincode.trim(),
      });

      setHomeName('');
      setStreet('');
      setCity('');
      setPincode('');
      if (onHomeCreated) onHomeCreated(response.data);
      onClose();
    } catch (err) {
      console.error('Failed to create home', err);
      setError(
        err.response?.data?.message || 'Failed to create home. Please verify your connection and permissions.'
      );
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-md animate-fadeIn"
      onClick={onClose}
    >
      <div
        className="relative w-full max-w-md bg-[#1B1613] border border-[#3E322A] rounded-2xl shadow-2xl p-8 text-[#FBF9F5]"
        onClick={(e) => e.stopPropagation()}
      >
        <button
          onClick={onClose}
          className="absolute top-5 right-5 text-[#A8988B] hover:text-[#FBF9F5] transition-colors text-xl font-light"
          aria-label="Close"
        >
          ✕
        </button>

        <div className="mb-6 text-center">
          <p className="text-xs uppercase tracking-[0.25em] text-[#C88242] font-semibold mb-2">
            REGISTRATION
          </p>
          <h2 className="text-2xl font-light tracking-wide text-[#FBF9F5]">
            Create New Smart Home
          </h2>
        </div>

        {error && (
          <div className="mb-4 p-3 rounded-lg bg-red-900/30 border border-red-500/50 text-red-200 text-xs">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          <div>
            <label className="block text-xs uppercase tracking-wider text-[#A8988B] mb-1.5">
              Home Name *
            </label>
            <input
              type="text"
              value={homeName}
              onChange={(e) => setHomeName(e.target.value)}
              placeholder="e.g. Amberwood Villa"
              className="w-full px-4 py-2.5 rounded-xl bg-[#261F1A] border border-[#3E322A] text-[#FBF9F5] focus:outline-none focus:border-[#C88242] text-sm"
              required
            />
          </div>

          <div>
            <label className="block text-xs uppercase tracking-wider text-[#A8988B] mb-1.5">
              Street Address
            </label>
            <input
              type="text"
              value={street}
              onChange={(e) => setStreet(e.target.value)}
              placeholder="e.g. 742 Evergreen Terrace"
              className="w-full px-4 py-2.5 rounded-xl bg-[#261F1A] border border-[#3E322A] text-[#FBF9F5] focus:outline-none focus:border-[#C88242] text-sm"
            />
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs uppercase tracking-wider text-[#A8988B] mb-1.5">
                City
              </label>
              <input
                type="text"
                value={city}
                onChange={(e) => setCity(e.target.value)}
                placeholder="e.g. Springfield"
                className="w-full px-4 py-2.5 rounded-xl bg-[#261F1A] border border-[#3E322A] text-[#FBF9F5] focus:outline-none focus:border-[#C88242] text-sm"
              />
            </div>

            <div>
              <label className="block text-xs uppercase tracking-wider text-[#A8988B] mb-1.5">
                Pincode
              </label>
              <input
                type="text"
                value={pincode}
                onChange={(e) => setPincode(e.target.value)}
                placeholder="e.g. 97477"
                className="w-full px-4 py-2.5 rounded-xl bg-[#261F1A] border border-[#3E322A] text-[#FBF9F5] focus:outline-none focus:border-[#C88242] text-sm"
              />
            </div>
          </div>

          <div className="mt-4 flex gap-3">
            <button
              type="button"
              onClick={onClose}
              className="flex-1 py-3 px-4 rounded-xl border border-[#3E322A] text-[#A8988B] hover:text-[#FBF9F5] hover:bg-[#261F1A] text-xs uppercase tracking-wider transition-colors"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={submitting}
              className="flex-1 py-3 px-4 rounded-xl bg-gradient-to-r from-[#C88242] to-[#A3642E] text-white text-xs font-semibold uppercase tracking-wider shadow-lg hover:brightness-110 active:scale-[0.99] disabled:opacity-50 transition-all flex items-center justify-center gap-2"
            >
              {submitting ? (
                <>
                  <div className="w-3.5 h-3.5 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                  <span>Creating...</span>
                </>
              ) : (
                'Create Home'
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
