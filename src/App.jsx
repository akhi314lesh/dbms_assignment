import { useState } from 'react'
import Authentication from './pages/Authentication'
import Landing from './pages/Landing'
import Home from './pages/Home'

function App() {
  const [authenticated, setAuthenticated] = useState(false)
  const [page, setPage] = useState('landing')

  if (!authenticated) {
    return (
      <Authentication
        onLogin={() => setAuthenticated(true)}
      />
    )
  }

  if (page === 'home') {
    return <Home />
  }

  return (
    <Landing
      onHome={() => setPage('home')}
    />
  )
}

export default App