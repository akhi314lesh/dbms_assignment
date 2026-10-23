import { useState } from 'react'
import { AuthProvider, useAuth } from './context/AuthContext'
import Authentication from './pages/Authentication'
import Landing from './pages/Landing'
import HomeDetails from './pages/HomeDetails'
import DeviceCategoryView from './pages/DeviceCategoryView'

function AppContent() {
  const { firebaseUser, loading } = useAuth()
  const [selectedHome, setSelectedHome] = useState(null)
  const [selectedCategory, setSelectedCategory] = useState(null)

  if (loading) {
    return (
      <div className="min-h-screen w-full flex items-center justify-center bg-[#140F0C] text-[#FBF9F5]">
        <div className="flex flex-col items-center gap-4">
          <div className="w-10 h-10 border-4 border-[#C88242] border-t-transparent rounded-full animate-spin"></div>
          <p className="text-sm tracking-widest text-[#E6D9CD] uppercase">Connecting Sanctuary Security...</p>
        </div>
      </div>
    )
  }

  if (!firebaseUser) {
    return <Authentication />
  }

  return (
    <>
      {selectedHome ? (
        <HomeDetails
          home={selectedHome}
          onBack={() => setSelectedHome(null)}
        />
      ) : selectedCategory ? (
        <DeviceCategoryView
          categoryName={selectedCategory}
          onBack={() => setSelectedCategory(null)}
        />
      ) : (
        <Landing
          onSelectHome={(home) => setSelectedHome(home)}
          onSelectCategory={(cat) => setSelectedCategory(cat)}
        />
      )}
    </>
  )
}

export default function App() {
  return (
    <AuthProvider>
      <AppContent />
    </AuthProvider>
  )
}