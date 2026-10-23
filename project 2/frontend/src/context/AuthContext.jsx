import { createContext, useContext, useState, useEffect } from 'react';
import api from '../services/api';

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
  // Built-in dev user for seamless local team testing without Firebase
  const [currentUser] = useState({
    uid: 'dev-teammate-1',
    email: 'teammate@smarthome.local',
    displayName: 'DA3 Teammate',
  });

  const [oracleUser, setOracleUser] = useState({
    userId: 1,
    name: 'DA3 Teammate',
    email: 'teammate@smarthome.local',
    role: 'ADMIN',
  });

  const [loading] = useState(false);
  const [authError, setAuthError] = useState(null);

  useEffect(() => {
    api.get('/api/auth/me')
      .then((res) => {
        if (res.data) {
          setOracleUser(res.data);
        }
      })
      .catch(() => {
        // Fallback default admin user
      });
  }, []);

  const signInWithGoogle = async () => true;
  const signInWithEmail = async () => true;
  const signUpWithEmail = async () => true;
  const signOut = async () => {
    console.log('Sign-out triggered (dev mode)');
  };
  const sendPasswordReset = async () => true;
  const clearError = () => setAuthError(null);

  const value = {
    firebaseUser: currentUser,
    currentUser,
    oracleUser,
    loading,
    authError,
    signInWithGoogle,
    signInWithEmail,
    signUpWithEmail,
    signOut,
    sendPasswordReset,
    clearError,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

// eslint-disable-next-line react-refresh/only-export-components
export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};

export default AuthContext;