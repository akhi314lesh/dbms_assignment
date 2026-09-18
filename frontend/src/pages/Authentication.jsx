import { useState, useEffect } from 'react'
import { useAuth } from '../context/AuthContext'

function Authentication() {
  const {
    signInWithGoogle,
    signInWithEmail,
    signUpWithEmail,
    sendPasswordReset,
    authError,
    clearError,
  } = useAuth()

  const [mode, setMode] = useState('signin')
  const [showSigninPassword, setShowSigninPassword] = useState(false)
  const [showSignupPassword, setShowSignupPassword] = useState(false)
  const [loading, setLoading] = useState(false)
  const [toast, setToast] = useState(null)

  // Forgot password modal state
  const [showForgotModal, setShowForgotModal] = useState(false)
  const [forgotEmail, setForgotEmail] = useState('')
  const [forgotLoading, setForgotLoading] = useState(false)

  const [signinData, setSigninData] = useState({
    email: '',
    password: '',
  })

  const [signupData, setSignupData] = useState({
    name: '',
    email: '',
    password: '',
    confirmPassword: '',
  })

  const showToast = (title, message, isError = false) => {
    setToast({ title, message, isError })

    setTimeout(() => {
      setToast(null)
    }, 4500)
  }

  const handleSignin = async (event) => {
    event.preventDefault()
    clearError()

    if (!signinData.email || !signinData.password) {
      showToast('Missing Credentials', 'Please provide both email and password.', true)
      return
    }

    setLoading(true)

    try {
      await signInWithEmail(signinData.email, signinData.password)
      // onAuthStateChanged in AuthContext synchronizes with /api/auth/me and updates state
    } catch (err) {
      showToast('Authentication Failed', err.message || 'Unable to sign in. Please verify your credentials.', true)
    } finally {
      setLoading(false)
    }
  }

  const handleSignup = async (event) => {
    event.preventDefault()
    clearError()

    if (!signupData.name.trim()) {
      showToast('Name Required', 'Please enter your full name.', true)
      return
    }

    if (!signupData.email.trim()) {
      showToast('Email Required', 'Please enter a valid email address.', true)
      return
    }

    if (signupData.password.length < 6) {
      showToast(
        'Password Too Short',
        'Your password must contain at least 6 characters.',
        true
      )
      return
    }

    if (signupData.password !== signupData.confirmPassword) {
      showToast(
        'Passwords Do Not Match',
        'Please make sure both passwords are identical.',
        true
      )
      return
    }

    setLoading(true)

    try {
      await signUpWithEmail(signupData.name, signupData.email, signupData.password)
      // onAuthStateChanged in AuthContext synchronizes with /api/auth/me and updates state
    } catch (err) {
      showToast('Registration Notice', err.message || 'Unable to create account.', true)
    } finally {
      setLoading(false)
    }
  }

  const handleOpenForgotModal = () => {
    setForgotEmail(signinData.email || '')
    setShowForgotModal(true)
  }

  const handleSendResetEmail = async (e) => {
    e.preventDefault()
    if (!forgotEmail.trim()) {
      showToast('Email Required', 'Please enter your email address to reset password.', true)
      return
    }

    setForgotLoading(true)
    try {
      await sendPasswordReset(forgotEmail.trim())
      setShowForgotModal(false)
      showToast(
        'Password Reset Sent',
        `A password reset link has been dispatched to ${forgotEmail.trim()}. Please check your inbox.`
      )
    } catch (err) {
      showToast('Reset Request Failed', err.message || 'Unable to send password reset email.', true)
    } finally {
      setForgotLoading(false)
    }
  }

  useEffect(() => {
    if (authError) {
      showToast('Authentication Notice', authError, true)
    }
  }, [authError])

  const handleGoogleLogin = async () => {
    clearError()
    setLoading(true)
    try {
      await signInWithGoogle()
    } catch (err) {
      // Handled by AuthContext and authError
    } finally {
      setLoading(false)
    }
  }


  return (
    <main className="min-h-screen w-full flex flex-col lg:flex-row bg-[#FBF9F5] text-[#1F1813] overflow-x-hidden">

      {/* =========================
          LEFT HERO SECTION
      ========================== */}
      <section
        aria-label="Smart Home Atmosphere Showcase"
        className="relative w-full lg:w-[54%] min-h-[520px] lg:min-h-screen overflow-hidden bg-[#140F0C] flex flex-col justify-between p-6 sm:p-10 lg:p-14 text-[#FBF9F5]"
      >

        {/* Background image */}
        <div className="absolute inset-0 overflow-hidden pointer-events-none">

          <img
            src="https://lh3.googleusercontent.com/aida-public/AB6AXuAyWYg7ETljkuAfG8CC_fnAga7PhFriTiNRaWCSAupKgwyrf5wS5wX_FClex9uxzJdU_jz6ljzlvt_DDm_14o_8fUctDsYUo5hpCbhNY2CK8UNDaNymUtE1iXjSQFCdWm8SI6qS_f-GS5g2GJMsvMVhUTkzoT5kTF31l08jMblFKqQQ08irfNlzw8VeV3ajWxXREDVjzmlNDyaU71f4fTQhM8Bcw6-kQnzBjUTo7mNNvzcZIki3krkF"
            alt="Luxury modern smart home"
            className="w-full h-full object-cover object-center opacity-90 brightness-[0.88] contrast-[1.02]"
            style={{
              animation: 'slowZoom 26s ease-in-out infinite alternate',
            }}
          />

          <div className="absolute inset-0 bg-gradient-to-t from-[#140F0C] via-[#140F0C]/35 to-[#140F0C]/30" />

          <div className="absolute inset-0 bg-gradient-to-r from-[#140F0C]/60 via-transparent to-[#140F0C]/30" />

        </div>

        {/* Hero header */}
        <header className="relative z-10 flex items-center justify-between">

          <div className="inline-flex items-center gap-2.5 px-3.5 py-1.5 rounded-full bg-[#140F0C]/65 border border-[#D7BA97]/30 backdrop-blur-md text-[11px] uppercase tracking-[0.2em] text-[#E8D1B5] font-medium">

            <span className="relative flex h-2 w-2">

              <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-[#C5A880] opacity-75" />

              <span className="relative inline-flex rounded-full h-2 w-2 bg-[#D7BA97]" />

            </span>

            Sanctuary Active • 72°F Comfort Mode

          </div>

          <span className="hidden sm:inline-block text-[11px] uppercase tracking-[0.2em] text-[#FBF9F5]/70 font-light backdrop-blur-sm px-3 py-1 rounded-md bg-[#140F0C]/30">
            Penthouse 48B • Atelier
          </span>

        </header>

        {/* Empty middle area */}
        <div className="relative z-10 my-auto py-12 lg:py-0" />

        {/* Bottom hero content */}
        <footer className="relative z-10 space-y-6 max-w-xl">

          <div className="flex items-center gap-3">

            <span className="h-px w-8 bg-[#D7BA97]/60" />

            <p className="text-[11px] uppercase tracking-[0.28em] text-[#E8D1B5] font-medium">
              Smart Home
            </p>

          </div>

          <blockquote className="space-y-3">

            <h1 className="font-serif text-3xl sm:text-4xl lg:text-5xl font-normal leading-[1.18] tracking-tight text-[#FAF7F2]">
              “Smarter and safer living.”
            </h1>

            <p className="text-sm sm:text-base text-[#FBF9F5]/80 font-light leading-relaxed max-w-md">
              Intelligent technology designed around the way you live.
            </p>

          </blockquote>

          <div className="pt-2 flex flex-wrap gap-2.5 sm:gap-3 text-xs tracking-wider text-[#FBF9F5]/90">

            <span className="px-3 py-1.5 rounded-full bg-[#1F1813]/60 backdrop-blur-md border border-white/10 text-[11px]">
              Adaptive Lighting
            </span>

            <span className="px-3 py-1.5 rounded-full bg-[#1F1813]/60 backdrop-blur-md border border-white/10 text-[11px]">
              Bespoke Climate
            </span>

            <span className="px-3 py-1.5 rounded-full bg-[#1F1813]/60 backdrop-blur-md border border-white/10 text-[11px]">
              Zero-Knowledge Security
            </span>

          </div>

        </footer>

      </section>

      {/* =========================
          RIGHT AUTH SECTION
      ========================== */}
      <section
        aria-label="Authentication Form"
        className="relative w-full lg:w-[46%] min-h-screen bg-[#FBF9F5] flex flex-col justify-between p-6 sm:p-12 lg:p-16"
      >

        {/* Top navigation */}
        <header className="w-full flex items-center justify-between pb-8 border-b border-[#E8E1D7]/70">

          <button
            type="button"
            onClick={() => setMode('signin')}
            className="flex items-center gap-3 group py-1 px-1 rounded-lg"
          >

            <div className="w-10 h-10 rounded-xl overflow-hidden shadow-sm flex items-center justify-center transition-transform duration-300 group-hover:scale-105">
              <img
                src="/logo.svg"
                alt="Smart Home"
                className="w-10 h-10 object-contain"
              />
            </div>

            <div className="flex flex-col text-left">

              <span className="text-sm font-semibold tracking-[0.2em] uppercase text-[#1F1813] leading-tight">
                Smart Home
              </span>

              <span className="text-[10px] tracking-[0.16em] uppercase text-[#4A3B32] font-light">
                Connected Living
              </span>

            </div>

          </button>

          <a
            href="#support"
            className="text-xs tracking-wider text-[#3D3128] hover:text-[#140F0C] transition-colors duration-200 font-medium"
          >
            Concierge →
          </a>

        </header>

        {/* Authentication content */}
        <div className="w-full max-w-md mx-auto my-auto py-8">

          {/* Tabs */}
          <nav
            aria-label="Authentication Mode"
            className="inline-flex p-1 bg-[#F5F1EB] rounded-xl border border-[#E8E1D7]/90 mb-8 w-full"
          >

            <button
              type="button"
              aria-selected={mode === 'signin'}
              onClick={() => setMode('signin')}
              className={`flex-1 py-2.5 text-xs font-medium tracking-wider uppercase rounded-lg transition-all duration-200 ${
                mode === 'signin'
                  ? 'bg-white text-[#140F0C] shadow-sm border border-[#D7BA97]/30'
                  : 'text-[#3D3128] hover:text-[#140F0C]'
              }`}
            >
              Sign In
            </button>

            <button
              type="button"
              aria-selected={mode === 'signup'}
              onClick={() => setMode('signup')}
              className={`flex-1 py-2.5 text-xs font-medium tracking-wider uppercase rounded-lg transition-all duration-200 ${
                mode === 'signup'
                  ? 'bg-white text-[#140F0C] shadow-sm border border-[#D7BA97]/30'
                  : 'text-[#3D3128] hover:text-[#140F0C]'
              }`}
            >
              Create Account
            </button>

          </nav>

          {/* =========================
              SIGN IN
          ========================== */}
          {mode === 'signin' && (

            <article className="space-y-6">

              <header className="space-y-2">

                <h2 className="font-serif text-3xl sm:text-4xl font-normal tracking-tight text-[#140F0C]">
                  Welcome home
                  <span className="italic text-[#C5A880]">.</span>
                </h2>

                <p className="text-xs sm:text-sm text-[#3D3128] font-normal leading-relaxed">
                  Manage your connected home with intelligence, simplicity, and control.
                </p>

              </header>

              <form
                className="space-y-5"
                onSubmit={handleSignin}
              >

                {/* Email */}
                <div className="space-y-1.5">

                  <label
                    htmlFor="signin-email"
                    className="block text-xs font-medium uppercase tracking-wider text-[#2A211B]"
                  >
                    Email Address
                  </label>

                  <input
                    id="signin-email"
                    type="email"
                    required
                    value={signinData.email}
                    onChange={(e) =>
                      setSigninData({
                        ...signinData,
                        email: e.target.value,
                      })
                    }
                    placeholder="Enter your email"
                    className="w-full bg-white text-[#140F0C] text-sm placeholder:text-stone-400 rounded-xl px-4 py-3 border border-[#E8E1D7] transition-all duration-200 focus:outline-none focus:border-[#C5A880] focus:ring-4 focus:ring-[#D7BA97]/20"
                  />

                </div>

                {/* Password */}
                <div className="space-y-1.5">

                  <div className="flex items-center justify-between">

                    <label
                      htmlFor="signin-password"
                      className="block text-xs font-medium uppercase tracking-wider text-[#2A211B]"
                    >
                      Password
                    </label>

                    <button
                      type="button"
                      onClick={handleOpenForgotModal}
                      className="text-xs text-[#C5A880] hover:text-[#140F0C] font-medium transition-colors"
                    >
                      Forgot password?
                    </button>

                  </div>

                  <div className="relative">

                    <input
                      id="signin-password"
                      type={showSigninPassword ? 'text' : 'password'}
                      required
                      value={signinData.password}
                      onChange={(e) =>
                        setSigninData({
                          ...signinData,
                          password: e.target.value,
                        })
                      }
                      placeholder="Enter your password"
                      className="w-full bg-white text-[#140F0C] text-sm placeholder:text-stone-400 rounded-xl px-4 py-3 pr-11 border border-[#E8E1D7] transition-all duration-200 focus:outline-none focus:border-[#C5A880] focus:ring-4 focus:ring-[#D7BA97]/20"
                    />

                    <button
                      type="button"
                      aria-label="Toggle password visibility"
                      onClick={() =>
                        setShowSigninPassword(!showSigninPassword)
                      }
                      className="absolute inset-y-0 right-0 flex items-center pr-3.5 text-stone-400 hover:text-[#1F1813]"
                    >

                      {showSigninPassword ? (

                        <svg
                          className="w-4 h-4"
                          fill="none"
                          stroke="currentColor"
                          viewBox="0 0 24 24"
                        >
                          <path
                            d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"
                            strokeLinecap="round"
                            strokeLinejoin="round"
                            strokeWidth="1.5"
                          />
                          <path
                            d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"
                            strokeLinecap="round"
                            strokeLinejoin="round"
                            strokeWidth="1.5"
                          />
                        </svg>

                      ) : (

                        <svg
                          className="w-4 h-4"
                          fill="none"
                          stroke="currentColor"
                          viewBox="0 0 24 24"
                        >
                          <path
                            d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M3 3l18 18"
                            strokeLinecap="round"
                            strokeLinejoin="round"
                            strokeWidth="1.5"
                          />
                        </svg>

                      )}

                    </button>

                  </div>

                </div>

                {/* Sign in button */}
                <button
                  type="submit"
                  disabled={loading}
                  className="w-full flex items-center justify-center gap-2 py-3.5 px-6 rounded-xl bg-[#1F1813] hover:bg-[#140F0C] disabled:opacity-70 text-[#FBF9F5] text-xs font-semibold uppercase tracking-[0.18em] shadow-md transition-all duration-200"
                >

                  {loading ? (
                    <>
                      <svg
                        className="animate-spin h-4 w-4 text-[#D7BA97]"
                        viewBox="0 0 24 24"
                        fill="none"
                      >
                        <circle
                          className="opacity-25"
                          cx="12"
                          cy="12"
                          r="10"
                          stroke="currentColor"
                          strokeWidth="4"
                        />

                        <path
                          className="opacity-75"
                          fill="currentColor"
                          d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"
                        />
                      </svg>

                      Synchronizing Sanctuary...

                    </>
                  ) : (
                    <>
                      Enter Sanctuary

                      <span className="text-[#D7BA97] text-base">
                        →
                      </span>
                    </>
                  )}

                </button>

              </form>

              {/* Divider */}
              <div className="relative flex items-center py-1">

                <div className="w-full border-t border-[#E8E1D7]" />

                <div className="absolute left-1/2 -translate-x-1/2 px-4 bg-[#FBF9F5] text-[11px] uppercase tracking-[0.2em] text-stone-400 whitespace-nowrap">
                  or continue with
                </div>

              </div>

              {/* Google */}
              <button
                type="button"
                id="google-signin-btn"
                onClick={handleGoogleLogin}
                className="w-full flex items-center justify-center gap-3 py-3 px-4 rounded-xl bg-white border border-[#E8E1D7] hover:border-[#D7BA97]/60 hover:bg-[#F5F1EB]/60 active:scale-[0.99] transition-all duration-200 shadow-sm text-xs font-medium text-[#2A211B] tracking-wide"
              >

                <svg
                  className="w-4 h-4"
                  viewBox="0 0 24 24"
                >
                  <path
                    d="M23.745 12.27c0-.7-.06-1.4-.19-2.07H12v4.51h6.6c-.29 1.52-1.14 2.82-2.4 3.68v3.05h3.88c2.27-2.09 3.665-5.17 3.665-9.17z"
                    fill="#4285F4"
                  />
                  <path
                    d="M12 24c3.24 0 5.95-1.08 7.93-2.91l-3.88-3.05c-1.08.72-2.45 1.16-4.05 1.16-3.12 0-5.77-2.1-6.72-4.93H1.25v3.15C3.26 21.36 7.34 24 12 24z"
                    fill="#34A853"
                  />
                  <path
                    d="M5.28 14.27c-.25-.72-.38-1.49-.38-2.27s.13-1.55.38-2.27V6.58H1.25C.45 8.18 0 10.03 0 12s.45 3.82 1.25 5.42l4.03-3.15z"
                    fill="#FBBC05"
                  />
                  <path
                    d="M12 4.75c1.77 0 3.35.61 4.6 1.8l3.42-3.42C17.95 1.19 15.24 0 12 0 7.34 0 3.26 2.64 1.25 6.58l4.03 3.15 4.03 3.15z"
                    fill="#EA4335"
                  />
                </svg>

                Continue with Google

              </button>

              <footer className="pt-2 text-center">

                <p className="text-xs text-[#3D3128]">

                  Don't have an account?

                  <button
                    type="button"
                    onClick={() => setMode('signup')}
                    className="font-semibold text-[#140F0C] hover:text-[#C5A880] transition-colors ml-1"
                  >
                    Create an account
                  </button>

                </p>

              </footer>

            </article>

          )}

          {/* =========================
              SIGN UP
          ========================== */}
          {mode === 'signup' && (

            <article className="space-y-6">

              <header className="space-y-2">

                <h2 className="font-serif text-3xl sm:text-4xl font-normal tracking-tight text-[#140F0C]">
                  Create your sanctuary
                  <span className="italic text-[#C5A880]">.</span>
                </h2>

                <p className="text-xs sm:text-sm text-[#3D3128] font-normal leading-relaxed">
                  Start building a smarter, safer home.
                </p>

              </header>

              <form
                className="space-y-4"
                onSubmit={handleSignup}
              >

                {/* Name */}
                <div className="space-y-1.5">

                  <label
                    htmlFor="signup-name"
                    className="block text-xs font-medium uppercase tracking-wider text-[#2A211B]"
                  >
                    Full Name
                  </label>

                  <input
                    id="signup-name"
                    type="text"
                    required
                    value={signupData.name}
                    onChange={(e) =>
                      setSignupData({
                        ...signupData,
                        name: e.target.value,
                      })
                    }
                    placeholder="Your name"
                    className="w-full bg-white text-[#140F0C] text-sm placeholder:text-stone-400 rounded-xl px-4 py-3 border border-[#E8E1D7] transition-all duration-200 focus:outline-none focus:border-[#C5A880] focus:ring-4 focus:ring-[#D7BA97]/20"
                  />

                </div>

                {/* Email */}
                <div className="space-y-1.5">

                  <label
                    htmlFor="signup-email"
                    className="block text-xs font-medium uppercase tracking-wider text-[#2A211B]"
                  >
                    Email Address
                  </label>

                  <input
                    id="signup-email"
                    type="email"
                    required
                    value={signupData.email}
                    onChange={(e) =>
                      setSignupData({
                        ...signupData,
                        email: e.target.value,
                      })
                    }
                    placeholder="name@residence.com"
                    className="w-full bg-white text-[#140F0C] text-sm placeholder:text-stone-400 rounded-xl px-4 py-3 border border-[#E8E1D7] transition-all duration-200 focus:outline-none focus:border-[#C5A880] focus:ring-4 focus:ring-[#D7BA97]/20"
                  />

                </div>

                {/* Password */}
                <div className="space-y-1.5">

                  <label
                    htmlFor="signup-password"
                    className="block text-xs font-medium uppercase tracking-wider text-[#2A211B]"
                  >
                    Password
                  </label>

                  <div className="relative">

                    <input
                      id="signup-password"
                      type={showSignupPassword ? 'text' : 'password'}
                      required
                      value={signupData.password}
                      onChange={(e) =>
                        setSignupData({
                          ...signupData,
                          password: e.target.value,
                        })
                      }
                      placeholder="Minimum 8 characters"
                      className="w-full bg-white text-[#140F0C] text-sm placeholder:text-stone-400 rounded-xl px-4 py-3 pr-11 border border-[#E8E1D7] transition-all duration-200 focus:outline-none focus:border-[#C5A880] focus:ring-4 focus:ring-[#D7BA97]/20"
                    />

                    <button
                      type="button"
                      aria-label="Toggle password visibility"
                      onClick={() =>
                        setShowSignupPassword(!showSignupPassword)
                      }
                      className="absolute inset-y-0 right-0 flex items-center pr-3.5 text-stone-400 hover:text-[#1F1813]"
                    >

                      {showSignupPassword ? '◉' : '◌'}

                    </button>

                  </div>

                </div>

                {/* Confirm password */}
                <div className="space-y-1.5">

                  <label
                    htmlFor="signup-confirm-password"
                    className="block text-xs font-medium uppercase tracking-wider text-[#2A211B]"
                  >
                    Confirm Password
                  </label>

                  <input
                    id="signup-confirm-password"
                    type="password"
                    required
                    value={signupData.confirmPassword}
                    onChange={(e) =>
                      setSignupData({
                        ...signupData,
                        confirmPassword: e.target.value,
                      })
                    }
                    placeholder="Repeat password"
                    className="w-full bg-white text-[#140F0C] text-sm placeholder:text-stone-400 rounded-xl px-4 py-3 border border-[#E8E1D7] transition-all duration-200 focus:outline-none focus:border-[#C5A880] focus:ring-4 focus:ring-[#D7BA97]/20"
                  />

                </div>

                {/* Terms */}
                <p className="text-[11px] text-[#4A3B32] leading-relaxed pt-1">

                  By creating your account, you agree to our{' '}

                  <a
                    href="#terms"
                    className="underline hover:text-[#1F1813]"
                  >
                    Terms of Sanctuary
                  </a>{' '}

                  and{' '}

                  <a
                    href="#privacy"
                    className="underline hover:text-[#1F1813]"
                  >
                    Privacy Safeguards
                  </a>.

                </p>

                {/* Create account */}
                <button
                  type="submit"
                  disabled={loading}
                  className="w-full flex items-center justify-center gap-2 py-3.5 px-6 rounded-xl bg-[#1F1813] hover:bg-[#140F0C] disabled:opacity-70 text-[#FBF9F5] text-xs font-semibold uppercase tracking-[0.18em] shadow-md transition-all duration-200"
                >

                  {loading ? (
                    <>
                      <svg
                        className="animate-spin h-4 w-4 text-[#D7BA97]"
                        viewBox="0 0 24 24"
                        fill="none"
                      >
                        <circle
                          className="opacity-25"
                          cx="12"
                          cy="12"
                          r="10"
                          stroke="currentColor"
                          strokeWidth="4"
                        />

                        <path
                          className="opacity-75"
                          fill="currentColor"
                          d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"
                        />
                      </svg>

                      Synchronizing Sanctuary...

                    </>
                  ) : (
                    <>
                      Create Account
                      <span className="text-[#D7BA97] text-base">
                        →
                      </span>
                    </>
                  )}

                </button>

              </form>

              {/* Divider */}
              <div className="relative flex items-center py-1">
                <div className="w-full border-t border-[#E8E1D7]" />
                <div className="absolute left-1/2 -translate-x-1/2 px-4 bg-[#FBF9F5] text-[11px] uppercase tracking-[0.2em] text-stone-400 whitespace-nowrap">
                  or sign up with
                </div>
              </div>

              {/* Google */}
              <button
                type="button"
                id="google-signup-btn"
                onClick={handleGoogleLogin}
                className="w-full flex items-center justify-center gap-3 py-3 px-4 rounded-xl bg-white border border-[#E8E1D7] hover:border-[#D7BA97]/60 hover:bg-[#F5F1EB]/60 active:scale-[0.99] transition-all duration-200 shadow-sm text-xs font-medium text-[#2A211B] tracking-wide"
              >
                <svg className="w-4 h-4" viewBox="0 0 24 24">
                  <path d="M23.745 12.27c0-.7-.06-1.4-.19-2.07H12v4.51h6.6c-.29 1.52-1.14 2.82-2.4 3.68v3.05h3.88c2.27-2.09 3.665-5.17 3.665-9.17z" fill="#4285F4" />
                  <path d="M12 24c3.24 0 5.95-1.08 7.93-2.91l-3.88-3.05c-1.08.72-2.45 1.16-4.05 1.16-3.12 0-5.77-2.1-6.72-4.93H1.25v3.15C3.26 21.36 7.34 24 12 24z" fill="#34A853" />
                  <path d="M5.28 14.27c-.25-.72-.38-1.49-.38-2.27s.13-1.55.38-2.27V6.58H1.25C.45 8.18 0 10.03 0 12s.45 3.82 1.25 5.42l4.03-3.15z" fill="#FBBC05" />
                  <path d="M12 4.75c1.77 0 3.35.61 4.6 1.8l3.42-3.42C17.95 1.19 15.24 0 12 0 7.34 0 3.26 2.64 1.25 6.58l4.03 3.15 4.03 3.15z" fill="#EA4335" />
                </svg>
                Continue with Google
              </button>

              <footer className="pt-2 text-center">

                <p className="text-xs text-[#3D3128]">

                  Already have an account?

                  <button
                    type="button"
                    onClick={() => setMode('signin')}
                    className="font-semibold text-[#140F0C] hover:text-[#C5A880] transition-colors ml-1"
                  >
                    Sign in
                  </button>

                </p>

              </footer>

            </article>

          )}

        </div>

        {/* Bottom footer */}
        <footer className="w-full pt-8 border-t border-[#E8E1D7]/70 flex flex-col sm:flex-row items-center justify-between text-[11px] text-[#4A3B32] gap-3">

          <div className="flex items-center gap-2">

            <span className="inline-block w-1.5 h-1.5 rounded-full bg-emerald-600" />

            <span>
              End-to-End Hardware Encrypted
            </span>

          </div>

          <div className="flex items-center gap-4">

            <a
              href="#privacy"
              className="hover:text-[#1F1813]"
            >
              Privacy
            </a>

            <span>•</span>

            <a
              href="#security"
              className="hover:text-[#1F1813]"
            >
              Security Whitepaper
            </a>

            <span>•</span>

            <span>
              © {new Date().getFullYear()}
            </span>

          </div>

        </footer>

      </section>

      {/* =========================
          FORGOT PASSWORD MODAL
      ========================== */}
      {showForgotModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm animate-fadeIn">
          <div className="relative w-full max-w-md bg-[#181310] border border-[#D7BA97]/30 rounded-2xl p-6 sm:p-8 text-[#FAF7F2] shadow-2xl space-y-5">
            <header className="space-y-1.5">
              <div className="inline-flex items-center gap-2 px-2.5 py-1 rounded-full bg-[#D7BA97]/15 border border-[#D7BA97]/30 text-[10px] uppercase tracking-[0.2em] text-[#E8D1B5]">
                Account Recovery
              </div>
              <h3 className="font-serif text-2xl font-normal text-[#FAF7F2]">
                Reset Password
              </h3>
              <p className="text-xs text-[#E6D9CD]/70 leading-relaxed">
                Enter your registered email address and we will send you a secure Firebase link to reset your password.
              </p>
            </header>

            <form onSubmit={handleSendResetEmail} className="space-y-4">
              <div className="space-y-1.5 text-left">
                <label
                  htmlFor="forgot-email-input"
                  className="block text-[11px] font-medium uppercase tracking-wider text-[#E8D1B5]"
                >
                  Email Address
                </label>
                <input
                  id="forgot-email-input"
                  type="email"
                  required
                  value={forgotEmail}
                  onChange={(e) => setForgotEmail(e.target.value)}
                  placeholder="name@residence.com"
                  className="w-full bg-[#241C16] text-[#FAF7F2] text-sm placeholder:text-stone-500 rounded-xl px-4 py-3 border border-[#D7BA97]/25 focus:outline-none focus:border-[#D7BA97] focus:ring-2 focus:ring-[#D7BA97]/20 transition-all"
                />
              </div>

              <div className="pt-2 flex items-center justify-end gap-3">
                <button
                  type="button"
                  disabled={forgotLoading}
                  onClick={() => setShowForgotModal(false)}
                  className="px-4 py-2.5 rounded-xl text-xs uppercase tracking-wider text-[#FAF7F2]/70 hover:text-[#FAF7F2] hover:bg-white/5 transition-colors"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={forgotLoading}
                  className="flex items-center gap-2 px-5 py-2.5 rounded-xl bg-[#C88242] hover:bg-[#b57335] disabled:opacity-60 text-[#140F0C] text-xs font-semibold uppercase tracking-wider transition-all shadow-md"
                >
                  {forgotLoading ? (
                    <>
                      <span className="w-3.5 h-3.5 border-2 border-[#140F0C] border-t-transparent rounded-full animate-spin"></span>
                      Sending...
                    </>
                  ) : (
                    'Send Reset Link'
                  )}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* =========================
          TOAST
      ========================== */}
      {toast && (
        <aside
          aria-live="polite"
          className="fixed top-6 right-6 z-50 max-w-sm w-[calc(100%-3rem)] bg-[#140F0C] text-[#FBF9F5] p-4 rounded-2xl shadow-2xl border border-[#D7BA97]/30 backdrop-blur-lg flex items-start gap-3 animate-fadeIn"
        >
          <div
            className={`w-6 h-6 rounded-full flex items-center justify-center flex-shrink-0 text-xs font-bold ${
              toast.isError
                ? 'bg-rose-900/40 text-rose-300 border border-rose-500/40'
                : 'bg-[#D7BA97]/20 text-[#D7BA97]'
            }`}
          >
            {toast.isError ? '✕' : '✓'}
          </div>

          <div className="flex-1 text-xs">
            <p
              className={`font-medium tracking-wide uppercase text-[10px] ${
                toast.isError ? 'text-rose-300' : 'text-[#E8D1B5]'
              }`}
            >
              {toast.title}
            </p>

            <p className="text-[#FBF9F5]/80 mt-0.5 leading-relaxed">
              {toast.message}
            </p>
          </div>
        </aside>
      )}

      {/* Animation */}
      <style>{`
        @keyframes slowZoom {
          0% {
            transform: scale(1);
          }
          50% {
            transform: scale(1.03);
          }
          100% {
            transform: scale(1);
          }
        }
      `}</style>

    </main>
  )
}

export default Authentication